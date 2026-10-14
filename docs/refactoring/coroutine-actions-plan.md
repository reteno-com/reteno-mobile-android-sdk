# Reteno Android SDK — Action-per-operation + Coroutines Refactoring Plan

Branch baseline: `feature/kotlin-first-internal-api` (after `cd2b83c`).
Status legend: ☐ todo · ◐ in progress · ☑ done

## 1. Goals and non-goals

**Goals**
1. Every SDK interaction (public API call, facade call from Push/FCM, lifecycle/system trigger) lives in **one Action file** that can be read top-to-bottom to understand the whole operation.
2. Remove `OperationQueue`, `PushOperationQueue`, `RetenoThread(Factory)` and every `Executors.*` usage; all concurrency goes through structured coroutines with injected dispatchers.
3. **100 % line + branch coverage**, enforced by Kover in CI, with tests written from scratch following the bookk.me testing playbook (adapted in §4).

**Non-goals / hard constraints**
- `RetenoDatabaseManager*` interfaces and implementations, DB schema, `DbUtil*`, and the local mappers stay **unchanged** (backward compatibility with existing installs). Actions call them from `withContext(io)`.
- Public API (`Reteno`, `AppInbox`, `Recommendation`, `RetenoNotifications`, callbacks, models) stays source- and binary-compatible. New suspend/Flow public APIs are out of scope (possible follow-up).
- `RestClient`/`RestClientImpl` stays the transport (blocking `HttpURLConnection`); it gets a suspend adapter, not a rewrite.

## 2. Current state (summary of the audit)

### 2.1 What exists already
| File | Pattern |
|---|---|
| `internal/action/Action.kt` | `runAction(tag, name){}` — UUID + START/SUCCESS/FAILURE logging, `runCatching` |
| `internal/action/event/Log{Event,EcomEvent,ScreenViewEvent,RetenoEvent}Action.kt` | per-call instance, `(ServiceLocator, ioDispatcher)`, DB insert + `eventFlow.emit` |
| `internal/action/user/Update{,MultiAccount}UserAttributesAction.kt` | long-lived, `Channel` + `Mutex` + debounced segmentation refresh, calls `RetenoInternalImpl.instance.stop()/start()` |

### 2.2 What OperationQueue / PushOperationQueue actually provide
- One serial `HandlerThread` that carries **all** network I/O (every async `ApiClient` call), `clearOld*` jobs and the IAM readiness poll; a cached thread pool (`addParallelOperation`); a main-looper handler (`addUiOperation`).
- `PushOperationQueue`: hand-rolled chain `device → user → interactions → events → inbox status → recom events → wrapped links`, advanced by each step calling `nextOperation()`, aborted with `removeAllOperations()` on a *repeatable* error (5xx / 429 / IO / non-200 2xx-3xx).

### 2.3 Defects found during the audit (must be decided per item — see §7)
Sync pipeline
- S1 Device push success appends `pushUserData` without `nextOperation()` → chain stalls until next tick (`ContactRepositoryImpl.kt:124`).
- S2 Device push failure always aborts the chain, even non-repeatable (`:140`).
- S3 Stalls (neither next nor abort): user non-repeatable failure, `EventsRepositoryImpl.kt:42` empty list `return`, interactions non-repeatable with `!cacheUpdated`.
- S4 Out-of-chain pushes (FCM token, notifications toggle, deeplink click, in-app interaction, inbox `markAsOpened`) call `nextOperation()` / `removeAllOperations()` on the global chain.
- S5 Concurrent `sendData` (30 s tick vs ≤25 s requests, `forcePush`) creates multiple cursors over one deque → duplicates/out-of-order.
- S6 `pushInAppInteractions` recurses into `pushInteractions()`; in-app interactions aren't in the chain at all.
- S7 **Log events are never pushed** since `60ad093` (`pushLogEvents` has no callers) and never cleaned → table grows forever, `hasDataForSync` always true, `PushDataWorker` never cancels.
- S8 `PushDataWorker` returns `success()` before the fire-and-forget push finishes; not gated on init.
- S9 `forcePush` throttle `lastForcePushTime` not thread-safe.
- S10 Wrapped links: insert (parallel pool) races push (handler thread); `deleteWrappedLinks(1)` deletes the oldest row, not the sent one → duplicate HEAD + lost link; `http://` links throw `ClassCastException` in `ConnectionManager` → counted as repeatable → chain aborted forever.

Init / facade
- I1 `registerDevice()` throwing inside `setConfigInternal` (e.g. Firebase not initialised) → `initWaitCondition` never completes → every `runAfterInit` call queues forever.
- I2 Validation exceptions (`externalUserId` blank, invalid `marketId`) thrown inside a deferred `runAfterInit` crash the host on Main instead of reaching the caller.
- I3 `DeviceIdHelper` CLIENT_UUID mode and `awaitForDeviceId` poll forever with no timeout.
- I4 `receivedConfigFromUser` not atomic; `start()` job isn't tracked so `stop()` can't cancel it (scheduler/IAM restart in background).
- I5 All providers are `ProviderWeakReference`; stateful singletons survive only because something holds them strongly.
- I6 Inbox and Recommendation calls are not init-gated.

Logger
- L1 `Logger.saveEvent` `onFailure { TODO() }` → `NotImplementedError` can crash the host.
- L2 Recursion: `getDeviceId()` failure → `Logger.e` → `getDeviceId()`…; DB insert failure → `Logger.e` → `LogRetenoEventAction` → insert fails…
- L3 `runAction` uses `runCatching` → swallows `CancellationException`.

In-app messages
- M1 ETag never sent (`headersWithEtag` null in both branches) → 304 cache path dead.
- M2 `fromJson` throwing inside an `onSuccess` callback is swallowed by OperationQueue → `suspendCancellableCoroutine` never resumes → `start()` hangs before scheduler/IAM view start.
- M3 Event gate `isFirstMessagesProcessed` never opens if fetch fails → `bufferUntil` grows unbounded.
- M4 `refreshSegmentation` re-downloads everything and never persists/applies new segment state.
- M5 `checkPauseState` with display-rule in-app sets `lastPushInteractionId=null` → `_fullHtmlStateFlow` stuck in Success → all display-rule in-apps blocked.
- M6 `notifyShown()` counts a show before display is confirmed; `TimesPerTimeUnit` compares lifetime `showCount`.
- M7 `showIamOnceReady` retries (150×200 ms) run on the network thread, are never cancelled, can run concurrently.
- M8 Non-thread-safe shared state (`postponedNotifications`, `inAppsWaitingForEvent`, `pauseBehaviour`, activity helper fields).

Inbox / Recom / Session
- B1 Inbox listeners `WeakHashMap` with value == key → never collected (leak); polling ignores lifecycle.
- B2 Inbox/recom callbacks never invoked if `fromJson` throws in `onSuccess`.
- R1 Recom rows with empty `recomEvents` never deleted; no batch limit.
- E1 `RetenoSessionHandlerImpl.start` doesn't cancel the previous job → double loop; first install sends SessionEnded with empty id.
- E2 `LifecycleTrackingOptions` constructor default `sessionEndEventsEnabled=true` vs `DEFAULT=false`.

Push / FCM
- P1 Cold start from FCM: `createDefaultChannel` no-ops while not initialised → notification posted to non-existent channel, DELIVERED still recorded.
- P2 Notification image downloads (`BitmapUtil`, carousel) have no timeouts → can exceed FCM ~20 s budget.
- P3 Delete `PendingIntent`s all use requestCode 0 + `FLAG_CANCEL_CURRENT` → each new notification cancels earlier delete intents.
- P4 `InteractionCacheStore.recordInteraction` is dead code — pre-init interactions are not persisted.

Test suite
- T1 **The current test source set does not compile** (7 files reference APIs removed in `60ad093`).
- T2 `RetenoTestApp` makes `OperationQueue`/`PushOperationQueue` synchronous via `mockkObject`, hiding S1–S6.
- T3 No coverage tool, no testFixtures, no tests in `RetenoSdkFcm`.

### 2.4 Security findings (org policy — report, don't fix silently)
| # | Finding | Location | Recommendation |
|---|---|---|---|
| SEC1 | Access key logged in plaintext (`requestProperties` includes `X-Reteno-Access-Key`); request bodies with email/phone/push token logged | `RestClientImpl.kt:74,78` | Phase 0: stop logging headers, bodies **and request URLs** (`RestClientImpl.kt:74,126,132` log `apiContract.url`); keep method + status only. Host can enable debug view in production via `setDebug`, so this isn't debug-only. |
| SEC2 | User PII (user, anonymous attributes) passed into `Logger.captureMessage`/`Logger.e` → stored and **uploaded as remote log events** | `ContactController.kt:66,84`, `RetenoInternalImpl.kt:341` | Strip PII from log messages; review with backend logging owner. |
| SEC3 | `SQL_PASSWORD` compiled into the published AAR's `BuildConfig` | `RetenoSdkCore/build.gradle:31` | Treat as public. Escalate to security team; consider Keystore-derived key with migration (touches DB layer → separate project). |
| SEC4 | Access key, email, phone, FCM token in plaintext SharedPreferences | `SharedPrefsManager.kt:46,349-377,404` | Follow-up: EncryptedSharedPreferences/Keystore with migration. |
| SEC5 | Unescaped user attributes (`${firstName}`, `${email}`…) string-replaced into the widget model injected into HTML loaded in a JS-enabled WebView with a JS bridge → script/JSON injection | `IamControllerImpl.kt:145-152`, `FullscreenIamContainer.kt:118-119` | JSON-encode substituted values in Phase 8; add injection tests. |
| SEC6 | WHERE clauses built by string interpolation (internal values only, low risk) | `RetenoDatabaseManagerWrappedLinksImpl.kt:87,96`, `RetenoDatabaseManagerInteractionImpl.kt:176` | DB layer frozen → track as follow-up; parameterise when DB changes are allowed. |
| SEC7 | `X-Reteno-SDK-Version` sent with literal `$` prefix for MobileApi (local uncommitted fix covers only line 161, not 155) | `RestClientImpl.kt:155` | Fix both lines in Phase 0. |
| SEC8 | `POST /logs/v1/events` accepts requests with **no authentication** (SDK sends no access key for `ApiContract.LogEvent`; verified 2026-10-13 with an empty `events` list only) → likely anyone can write arbitrary records into the logs store | `RestClientImpl.kt` header `when`, backend | Raise with backend owner: rate limiting / payload limits at minimum, or require the access key header (SDK change in Phase 2 if backend agrees). |

SEC1–SEC3, SEC5 and SEC8 should be raised with the security team / SDK owner, not only fixed in code.

## 3. Target architecture

```
com.reteno.core.internal
├── action/                      one file per operation, grouped by feature
│   ├── trace/                   ActionRunner, ActionTrace, TraceFormatter, ActionFailureReporter, MonotonicClock
│   ├── event/  user/  device/  sync/  init/  lifecycle/  interaction/
│   ├── deeplink/  inbox/  recommendation/  iam/  push/
├── coroutine/
│   ├── RetenoDispatchers.kt     interface { main; io; default } + AndroidRetenoDispatchers
│   ├── RetenoScope.kt           retenoScope(): SupervisorJob + handler that logs via sink, never persists
│   └── InitGate.kt              StateFlow<InitState>; awaitInit(); awaitUserInit()
├── event/
│   └── EventBus.kt              injectable wrapper over the shared eventFlow
├── log/
│   ├── RetenoLogSink.kt         RetenoLogSink, AndroidLogSink, isLoggingEnabled(), log()
│   ├── LogPriority.kt
│   └── Redact.kt                token / email / phone / fields / text / summary / stackTrace
├── network/
│   ├── ApiResult.kt             Success(body, headers) | Failure(statusCode, body, error).isRepeatable
│   └── SuspendApiClient.kt      suspend fun request(...): ApiResult  (wraps RestClient on io)
├── sync/
│   ├── SyncCoordinator.kt       single-flight, conflated requests, ordered steps
│   └── SyncScheduler.kt         foreground loop: delay(period) → coordinator.request()
└── state/                       explicit in-memory state holders (IAM, session, inbox subscribers)
```

### 3.1 Action contract
```kotlin
internal class LogEventAction(
    private val eventsDb: RetenoDatabaseManagerEvents,
    private val restConfig: RestConfig,
    private val eventBus: EventBus,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner,
) {
    suspend operator fun invoke(event: Event): Result<Unit> = runner("LogEvent") {
        args("type" to event.eventTypeKey)
        step("save event to local database") { withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) } }
        step("notify in-app message triggers") { eventBus.emit(event) }
    }
}
```
Rules:
- **Explicit constructor dependencies, not `ServiceLocator`.** This is the one deliberate departure from the existing actions: the SutFixture test pattern needs plain mocks, and stubbing `serviceLocator.xProvider.get()` chains is what made the current tests brittle. `ServiceLocator` gains an `actions` section that builds actions.
- `suspend operator fun invoke(...)`: returns `Result<T>`.
- `ActionRunner.run` (replaces `runAction`; built in Phase 0): rethrows `CancellationException`, returns `Result<T>`, logs through `RetenoLogSink` (never persisted), and reports one remote log event per failed action through an injected `ActionFailureReporter`. `LogRetenoEventAction` gets `NoFailureReporting`, so a failing log-event insert can't recurse (L2).
- No `RetenoInternalImpl.instance` / `Reteno.instance` access inside actions. Cross-cutting needs (restart SDK, force sync) are injected interfaces (`SdkLifecycle`, `SyncTrigger`).
- Host callbacks are invoked via `withContext(dispatchers.main)`, so the thread contract is explicit and testable.
- Validation the caller must see (blank `externalUserId`, `marketId`) runs **synchronously in the facade before deferring** (fixes I2), in a shared validator (`UserAttributesValidator`).

### 3.2 Replacing the queues
| Today | Replacement |
|---|---|
| `OperationQueue.addOperation{ apiClient… }` | `SuspendApiClient.request()` on `dispatchers.io` |
| `addParallelOperation{}` | `scope.launch(io)` or plain `withContext(io)` inside the action |
| `addUiOperation{}` | `withContext(dispatchers.main)` |
| `addOperationAfterDelay` | `delay()` inside a tracked `Job` |
| `PushOperationQueue` chain | `SyncCoordinator` (below) |
| `ScheduledExecutorService` (sync, inbox polling) | `while (isActive) { delay(period); … }` in a `Job` cancelled on stop/unsubscribe |
| `runAfterInit {}` | `scope.launch { initGate.awaitInit(); action(...) }` |

**SyncCoordinator semantics** (single-flight, replaces S1–S6):
- `request()` sends to a `Channel(CONFLATED)`; one consumer coroutine runs `syncOnce()`, so multiple `forcePush` calls during a run fold into **one** follow-up run (replaces the 1 s throttle, S9).
- `syncOnce()` runs steps in order: `PushDevice → PushUser → PushInteractions → PushInAppInteractions(new) → PushEvents → PushInboxStatus → PushRecomEvents → PushWrappedLinks → PushLogEvents(restored)`.
- Each step is its own Action returning `StepOutcome.Continue | StepOutcome.Abort`. A step drains its table in batches. Non-repeatable error → drop the batch, continue. Repeatable error → `Abort` (keeps today's "retry on next tick" contract).
- Out-of-chain pushes (FCM token, notifications toggle, deeplink click, in-app interaction, inbox opened) become **"persist, then `syncTrigger.request()`"**, so all network writes for a table go through one serialized consumer. That also removes the wrapped-link double-consumer race (S10) without touching the DB manager.
- `suspend fun syncNow(): SyncResult` is exposed for `PushDataWorker`, which becomes a `CoroutineWorker` that awaits it and maps Abort → `Result.retry()` (S8).

### 3.3 Step-by-step action logging

Every action logs a readable timeline of what it did. The step names double as documentation of the operation, which fits the no-comments rule: reading the `step("…")` calls top to bottom reads like the operation's spec.

**What it looks like in logcat** (filter: `tag:RetenoAction`, or `tag:RetenoAction message:a1f3c2` for one run)
```
D RetenoAction: ┌─ [a1f3c2] SetUserAttributes  externalUserId=…8f21  user={firstName, email, phone}
D RetenoAction: │  [a1f3c2] ✓ validate attributes                              0 ms
D RetenoAction: │  [a1f3c2] ✓ compare device id suffix          unchanged      1 ms
D RetenoAction: │  [a1f3c2] ✓ save user to local database       rows=1         4 ms
D RetenoAction: │  [a1f3c2] → request sync
D RetenoAction: └─ [a1f3c2] SetUserAttributes ✓ SUCCESS                          9 ms
```
Nested actions (the sync run and its steps) share the root id and indent:
```
D RetenoAction: ┌─ [9c01ab] Sync  trigger=forcePush
D RetenoAction: │  ┌─ [9c01ab.1] PushDevice
D RetenoAction: │  │  [9c01ab.1] ✓ load unsynced devices         count=1        2 ms
D RetenoAction: │  │  [9c01ab.1] ✓ send device to server         200          181 ms
D RetenoAction: │  └─ [9c01ab.1] PushDevice ✓ CONTINUE                         186 ms
D RetenoAction: │  ┌─ [9c01ab.2] PushUser
D RetenoAction: │  │  [9c01ab.2] · nothing to send
D RetenoAction: │  └─ [9c01ab.2] PushUser ✓ CONTINUE                             1 ms
E RetenoAction: │  │  [9c01ab.3] ✗ send interactions to server   503 repeatable 230 ms
W RetenoAction: │  └─ [9c01ab.3] PushInteractions ✗ ABORT                       232 ms
W RetenoAction: └─ [9c01ab] Sync ✗ ABORTED at PushInteractions (3/9)            420 ms
```
Possible endings: `✓ SUCCESS`, `✗ FAILURE <ExceptionClass>: <message>` (stack trace attached at ERROR), `⊘ CANCELLED`, and step-defined outcomes such as `CONTINUE` / `ABORT`.

**API** (`internal/action/trace/`, as built in Phase 0)
```kotlin
internal class ActionRunner(
    sink: RetenoLogSink,
    failureReporter: ActionFailureReporter,
    clock: MonotonicClock = SystemMonotonicClock,
    newRootId: () -> String = { UUID.randomUUID().toString().take(6) },
) {
    suspend operator fun <T> invoke(
        name: String,
        outcome: (T) -> String = { "SUCCESS" },
        reportFailure: Boolean = true,
        block: suspend ActionTrace.() -> T,
    ): Result<T>
}

internal interface ActionTrace {
    fun args(vararg values: Pair<String, Any?>)
    suspend fun <T> step(name: String, detail: (T) -> String = { "" }, block: suspend () -> T): T
    fun note(message: () -> String)
}
```
Usage:
```kotlin
suspend operator fun invoke(event: Event): Result<Unit> = runner("LogEvent") {
    args("type" to event.eventTypeKey)
    step("save event to local database") { withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) } }
    step("notify in-app message triggers") { eventBus.emit(event) }
}
```

**Rules**
- **Format and output:**
  - One `Log` call per line, so concurrent actions never split each other's lines. The `[id]` prefix lets you filter one run out of interleaved output.
  - Lines are capped at 1 000 chars (logcat's limit is about 4 000) and truncated with `…`.
  - Single tag `RetenoAction` (within the 23-char limit that matters on minSdk 21). Inside the message the action keeps its own name.
- **When step logs are printed:** at DEBUG, only when `BuildConfig.DEBUG || Util.isDebugView()`, the same gate as today's `Logger.d`. In the published release AAR `BuildConfig.DEBUG` is false, so only the host's `setDebug(true)` turns them on.
- **Cost when logging is off:** the `args`, `detail` and `note` lambdas are never called, so no strings are built.
- **What gets persisted and uploaded:** `step` lines are never stored as log events. **One** remote log event is created per failed action, not per step. It contains the action name, failed step, exception class and message, with no `args` (protects SEC2 and prevents the feedback loop from D3).
- **Redaction is mandatory (org policy), even in debug output:**
  - Access key and other tokens: never printed. The FCM token is shown as its last 4 characters.
  - Email and phone: masked (`j***@***.com`, `***4567`).
  - User and attributes: field *names* only, never values.
  - Server calls: a plain-language step name (`send device to server`, `load in-app messages from server`) plus HTTP status and, on failure, `repeatable`/`non-repeatable`. **No URLs, hosts, paths, query params, headers or bodies.** The step name is chosen by the action, never derived from `ApiContract.url`, so a URL can't leak by accident. A fixture test asserts that no line written during the sync and network tests contains `http`, `reteno.com` or `/v1/`.

  All of this goes through `internal/action/trace/Redact.kt` helpers, so actions never format PII themselves.
- **Durations** come from an injected monotonic clock (`() -> Long` nanos), so tests produce deterministic `ms` values. `kotlin.time.TimeSource` is still experimental on Kotlin 1.8.
- **Nesting:** the current trace travels as a `CoroutineContext` element. A child `runner(...)` called inside a parent becomes `[root.n]` and indents automatically. A trace started on another coroutine (`launch` without the element) is a new root.
- **Output path:** everything goes to a `RetenoLogSink` interface (default: `android.util.Log`). This part of Phase 9 moves into Phase 0, so tests capture lines with a `FakeLogSink` instead of `mockkObject(Logger)`.

**Testing**
- **Format tests:** `ActionTrace` and the formatter have their own exhaustive tests asserting exact lines for:
  - success, failure, cancellation
  - nested and multiple siblings
  - empty details, truncation
  - logging disabled: no lines, and the lambdas are never called
  - every redaction helper
- **Action tests** assert their trace only through `TestActionRunner.successfulTrace(name, args, steps…)`, which builds the expected lines with `TraceFormatter`. A format change never breaks them; a changed step name or argument does, because that is part of the action's contract.
- Landed in **Phase 0** (`ActionRunner`). From Phase 1 on, the Definition of Done requires every action to express its work as named `step`s.

## 4. Testing standard (adapted from bookk.me-backend `AGENTS.md`)

### 4.1 Rules carried over verbatim
- **Never `runBlocking`.** Every test body is `runUnitTest { }`.
- `given()` / `whenn()` / `then()` are **required in every test**; `runUnitTest` asserts at runtime that all three ran.
- **Fresh SUT and fresh mocks per test** via a `private class SutFixture { val dep = mockk<…>(); val sut = X(dep) }`. No class-level mocks, no shared state.
- Test names are backticked sentences: `` `should abort sync when device push fails with repeatable error`() ``.
- JUnit 5 assertions (`org.junit.jupiter.api.Assertions`). Assert failure types with `is`.
- MockK + kotlinx-coroutines-test + JUnit 5 as the primary stack.
- **Contract-first coverage checklist per operation** (happy path, every failure branch, cancellation, threading of callbacks, sync-trigger emission), ending in a **Final Coverage Report** mapping each item to a test; gaps listed explicitly.
- **Strict TDD**: red → green → refactor, one behaviour per test.
- Long-running work started by the SUT (sync consumer, scheduler, pollers, session ticker) is driven with `runCurrent()` / `advanceTimeBy()`, never `advanceUntilIdle()`. Because `TestScope.backgroundScope` only exists from coroutines-test 1.7, that work runs in a fixture-provided child scope (`testScope + Job()`) that the fixture cancels at the end of `runUnitTest`.
- `runUnitTest` has **no timeout** (decision D1). It is `runTest(context) { body(); assertFormat() }` on coroutines-test 1.6.4.
- No comments in new code (existing comments untouched). KDoc on **public API** is kept, since it's what SDK consumers read.
- After each feature: self code-review pass, fix findings, and record new learnings in the repo's `AGENTS.md`.

### 4.2 Android adaptations
| bookk rule | Adaptation here | Why |
|---|---|---|
| Entity `companion object { fun stub() }` | Stub factories live in the fixtures module as top-level functions (`stubUser(...)`, `stubInAppMessage(...)`), defaulted params | Domain models are **public SDK API**; adding companions to them changes the published surface. Local per-test-file builders remain forbidden. |
| `testFixtures` source set | New module `:RetenoSdkTestFixtures` (`testImplementation project(':RetenoSdkTestFixtures')`) | Kotlin in Android `testFixtures` needs AGP ≥ 8.5 experimental flags; repo is on AGP 8.1. |
| JUnit 5 everywhere | JUnit 5 via `de.mannodermaus.android-junit5`; Robolectric tests (SQLite DB managers, receivers, notification builder, WebView container) run on JUnit 4 through `junit-vintage-engine` in the same task, still using `runUnitTest` + given/whenn/then | Robolectric has no JUnit 5 runner. `runUnitTest` is runner-agnostic. |
| H2 datasource tests | Robolectric in-memory SQLite for DB managers, one DB per test | DB managers are frozen but must still reach 100 %. |
| bookk `runUnitTest(timeout = …)` | No timeout parameter | Toolchain stays on coroutines 1.6.4 (D1). |
| `mockTransaction()` | `testDispatchers()` fixture returning `RetenoDispatchers` backed by one `StandardTestDispatcher(testScheduler)` | Every action takes `RetenoDispatchers`. |

### 4.3 Fixtures module contents
`Test.kt` (port of bookk `runUnitTest`/`given`/`whenn`/`then`, verbatim, do-not-edit) · `TestDispatchers.kt` · `stub/*` · `FakeSuspendApiClient` (scripted responses + recorded requests) · `FakeSyncTrigger` · `FakeEventBus` · `LoggerFixture` (scoped `mockkObject(Logger)` set up and torn down **inside** `runUnitTest`, so no cross-test leakage).

### 4.4 Coverage gate
- Kover (`org.jetbrains.kotlinx.kover`) on Core, Push, Fcm; merged report at root.
- `koverVerify` rule: **line 100 %, branch 100 %** on the scope below; run locally before each PR (Kover is not part of CI).
- Phase gates: while migrating, the rule applies to `com.reteno.core.internal.**` plus each feature package once its phase is done; at Phase 10 it applies to whole modules.
- Exclusions only via a reviewed list in `build.gradle`: `BuildConfig`, `R`, `*$$serializer` if any, `@Deprecated` `BackwardCompatibilityDriver` shims (decision D7). No ad-hoc `@Exclude` annotations.
- Known Kotlin bytecode branch artefacts (inline `?.let`, `when` on sealed exhaustiveness, coroutine state machines) are resolved by restructuring code, not by exclusions.

## 5. Phase plan (feature by feature)

Each phase is a vertical slice: new actions + their tests, call sites switched, the old controller/repository methods for that slice **deleted**, the matching legacy tests **deleted**, `koverVerify` green for the slice, sample `app` smoke-tested.

### Phase 0 — Foundations ◐ (done except scanning jobs; see notes)
Build/test infra
- ☑ Non-compiling legacy tests (T1): there were **9** files, not 7. Deleted the 5 that only test removed APIs (`AppLifecycleControllerTest`, `EventControllerTest`, `ScreenTrackerControllerTest`, `RetenoSessionHandlerImplTest`, `RetenoActivityHelperFragmentTest`); removed only the broken methods from `RetenoImplTest` (6), `EventsRepositoryImplTest` (2), `LogEventRepositoryImplTest` (4); fixed `IamControllerImplTest` (3 stub lines) to use the global `eventFlow`, keeping its recent tests.
- ☑ **No toolchain bump (D1):** Kotlin 1.8.0, coroutines 1.6.4, AGP and Gradle stay as they are. New code avoids Kotlin 1.9+ features (`data object`, `entries`) and coroutines 1.7+ APIs (`backgroundScope`, `runTest(timeout=)`).
- ☑ Test-only additions: JUnit 5.10.2 BOM + `junit-vintage-engine`, `de.mannodermaus.android-junit5` 1.10.0.0, Kover 0.7.6 (Core + Push). Kover needs `excludeInstrumentation` for JUnit/Robolectric/MockK packages (it broke JUnit 3 `TestCase.assertEquals`). ☐ PowerMock/Mockito removal deferred: legacy `RestClientImplTest` still uses `Whitebox` (the two Push tests went away in Phase 5); removed when that test is rewritten.
- ☑ `:RetenoSdkTestFixtures` (JVM module) with `runUnitTest`/`given`/`whenn`/`then` and `workScope()` (replacement for `backgroundScope`), plus its own self-tests. Fakes of `internal` SDK types (`FakeLogSink`, `testDispatchers()`, `virtualClock()`, `CountingDispatcher`) live in `RetenoSdkCore/src/test/.../fixtures` because another module can't see `internal`.
- ☑ `AGENTS.md` + `CLAUDE.md` (`@AGENTS.md`).
- ☑ CI `tests_SDK:Core` runs fixture tests and `testDebugUnitTest`. **Kover stays local only (decision): `koverVerifyDebug` is run before each PR, not in CI.** ☐ Semgrep + dependency scanning jobs: pending a decision on runner images/tooling.

Runtime infra (each with full tests)
- ☑ `RetenoDispatchers` / `AndroidRetenoDispatchers`, `retenoScope()`.
- ☑ `ActionRunner` + `ActionTrace` + `TraceFormatter` + `ActionFailureReporter` (`RemoteLogFailureReporter`, `NoFailureReporting`) + `MonotonicClock`; `RetenoLogSink` / `AndroidLogSink` / `isLoggingEnabled()`; `Redact` (§3.3). Old `runAction` stays until Phase 1 migrates the 4 event actions.
- ☑ `InitGate` (wired into the facade in Phase 4).
- ☑ `SuspendApiClient` over `RestClient` (returns `ApiResult.Success(body, headers) | Failure(code, body, throwable)` with `isRepeatable`); parse `fromJson` **inside** the action so parsing errors become `Result.failure`, not hangs (M2, B2).
- ☑ `EventBus` wrapping today's `eventFlow`.
- ☑ Security quick fixes: SEC1 (RestClientImpl logs only method, status and redacted error summary), SEC7 (both version headers), L1 (`Logger` never throws into the host; it also crashed earlier than expected, because `RetenoLogEvent()` defaults read `Reteno.instance`). Covered by `RestClientImplLoggingTest` (verified failing on the old code) and `LoggerWithoutSdkInstanceTest`.

### Phase 1 — Events ☑
Done:
- `LogEventAction`, `LogEcomEventAction`, `LogRetenoEventAction` rewritten on `ActionRunner` with explicit deps (`RetenoDatabaseManager*`, `deviceId: () -> DeviceId`, `EventBus`, `RetenoDispatchers`). `LogScreenViewEventAction` delegates to `LogEventAction(Event.ScreenView(name))`, which also removes the double `Event.ScreenView` construction. E-commerce stays separate because it stores parameters (`EcomEvent.toDb()`) but emits `asRegularEvent()` without them.
- `LogRetenoEventAction` runs with `reportFailure = false`, so a failing log-event insert can never produce another log event (L2 for this path). `NoFailureReporting` was removed in favour of that flag.
- `di/ActionsLocator` builds each action once (previously one instance per call) with a shared runner using `RemoteLogFailureReporter { Logger.captureMessage(it, ERROR) }`; `RetenoInternalImpl` calls `actions.logX(...)`.
- Old `internal/action/Action.kt` (`runAction`, swallowed cancellation — L3) deleted.
- Coverage gate extended to `internal.action.event` + `di.ActionsLocator`.
Deferred: `LogLifecycleEventAction` is not needed — lifecycle events already flow through `logEvent`; `AppLifecycleController` itself migrates in Phase 4. The global `eventFlow` stays as `EventBus`'s backing flow until IAM moves to `EventBus` in Phase 8.

Final Coverage Report (Phase 1)
- [x] LogEvent inserts `EventsDb(deviceId, event.toDb())`: `LogEventActionTest.should insert event for current device`, `ActionsLocatorTest.should wire log event action…`
- [x] Insert on IO: `…should insert event on io dispatcher` (×3 actions, via `MarkingDispatcher`)
- [x] Emission after insert / ecom emitted as regular event / screen view emits stored event: `…should emit…` tests
- [x] Insert failure → `Result.failure`, no emission, exactly one report with failing step: `…should fail without emitting when insert fails`, `ActionsLocatorTest.should send one redacted remote log…`
- [x] Log-event insert failure is never reported (no recursion): `LogRetenoEventActionTest.should not report its own failure…`, `ActionsLocatorTest.should not send remote log…`
- [x] Cancellation before running → nothing inserted/emitted/reported: `LogEventActionTest.should neither insert nor emit when cancelled before running`
- [x] Trace shows type/level and step names but no parameter values, order ids or messages: `…should trace…` tests
- [x] Each action created once: `ActionsLocatorTest.should create each action once`
- Gaps: facade (`RetenoInternalImpl.logX`) wiring has no unit test yet — the facade is covered in Phase 4. Not smoke-tested on a device in this phase.

### Phase 2 — Data sync pipeline ☑
Done (`internal/action/sync/`):
- Steps (each `SyncStep`, returns `StepOutcome.CONTINUE | ABORT`, named steps, DB on IO): `PushDeviceStep`, `PushUserStep`, `PushInteractionsStep`, `PushInAppInteractionsStep` (new), `PushEventsStep`, `PushInboxStatusStep`, `PushRecomEventsStep`, `PushWrappedLinksStep`, `PushLogEventsStep` (restored, last, batches of 50, backlog cap 500, `reportFailure = false`).
- `SyncDataAction` (`SyncScope.CONTACT` = device+user, `ALL` = everything; stops at first ABORT; a step that fails unexpectedly counts as CONTINUE so one bad row can't block later tables).
- `SyncCoordinator` (`SyncTrigger`: `request()` conflated, `syncNow(scope)` under one `Mutex`).
- `DataSync` (replaces `ScheduleController`): foreground loop 30 s (10 s debug view) + 0–10 s jitter, restart-safe, enqueues `PushDataWorker`; `clearOldData()` after 3 s; delegates `request`/`syncNow`.
- `ClearOldDataAction` (one child action per table, failures isolated, same INFO reports as before).
- `BackgroundSyncAction` + `PushDataWorker` as `CoroutineWorker` (awaits the sync; SYNCED → success, ABORTED/NOT_INITIALIZED → retry, foreground/no data → failure).
- Traces: `TracedOutcome` lets step values and action results mark themselves ✓/✗ (`ApiResult.Failure` → `✗ send device to server   503 repeatable`, aborted step → `✗ ABORT` at WARN).
- Wiring: `ServiceLocator` owns `ActionsLocator`; repositories call `requestSync()` after saving (device, in-app interaction, wrapped link, inbox opened); `ContactController` uses `syncContactNow()` for registration and user updates; `IamViewImpl` and the facade request syncs; `AppLifecycleController` calls `clearOldData`.
- Deleted: `PushOperationQueue`, `ScheduleController(Impl)` + provider, `EventController` + provider, `EventsRepository(Impl)` + provider, `LogEventRepository(Impl)` + provider, every `push*`/`clearOld*` in repositories and controllers, and their legacy tests (52 legacy tests removed, 4 legacy test files deleted; remaining legacy tests updated to assert `requestSync`).
- SEC2 shipped with the log upload: `Logger` persists only `Redact.uploadable` text; validation messages in `ContactController` no longer contain the user/attributes.
Fixed defects: S1–S10, plus unchanged-unsynced devices now marked synced (worker can cancel), empty event groups deleted instead of stalling, non-https wrapped links dropped instead of blocking the chain forever, registration failure no longer throws out of init (part of I1).
Kept for later: `RetenoThreadFactory` (still used by inbox polling, Phase 6); `OperationQueue` (other flows).
Review follow-up (`/code-review` of Phase 5): restored conditional vibration pattern (a null pattern turned vibration off), interaction token fetched again when not stored (dropping deliveries was worse than the fetch; D4 now reads "fetch only where an interaction or registration needs it"), token read moved to IO, the cache → await → process sequencing moved out of `ActionsLocator` into `RecordPushInteractionAction`, image size/scheme limits, button extras written only when present (host receivers may check `containsKey`). Kept by decision: the default channel is created before init from bundled/stored data (P1) — hosts must set `defaultNotificationChannelConfig` before the first push, because Android lets apps change only a channel's name and description later; action-button clicks before init stay in memory (the request table has no action column, DB frozen).
Behaviour changes to announce: technically-public internals removed (`ServiceLocator.scheduleControllerProvider`, `ServiceLocator.logEventRepositoryProvider`, `ScheduleController`, `LogEventRepository`, `ContactController` constructor and push methods). In CONTACT sync a repeatable device failure now also postpones the user push (previously both were sent independently).

Final Coverage Report (Phase 2) — Kover on gated packages: 784/784 lines, 253/253 branches, 344/344 methods.
- [x] Every step: empty table → no request; request method/endpoint/body; success deletes/marks synced; repeatable → ABORT and rows kept; non-repeatable → rows dropped and CONTINUE; IO dispatch; no PII/URL in trace — `Push*StepTest` (75 tests)
- [x] Device rules: no synced / outdated / changed / unchanged-not-outdated, rounds and round limit — `PushDeviceStepTest`
- [x] User merge, diff-only request, full model persisted, outdated → full send — `PushUserStepTest`
- [x] Log events: batches, backlog cap boundary, delete-failure stop, no self-report — `PushLogEventsStepTest`
- [x] Order, CONTACT scope, abort stop point, crashed step continues, thrown step fails once — `SyncDataActionTest`
- [x] Conflation, syncNow serialization, crash → ABORTED — `SyncCoordinatorTest`
- [x] Scheduler cadence/jitter/debug view/restart/stop, enqueue on IO + failure reported, cleanup delay — `DataSyncTest`
- [x] Cleanup per table, report wording, isolation — `ClearOldDataActionTest`
- [x] Background sync outcomes — `BackgroundSyncActionTest`; worker mapping + enqueue request — `PushDataWorkerTest`
- [x] Real wiring order against all 9 tables/endpoints, enqueue, INFO reports, cancel on no data — `ActionsLocatorSyncWiringTest`
- [x] Upload sanitizing — `RedactTest`, `LoggerUploadSanitizingTest`
- Gaps: legacy repositories' new `requestSync()` calls are covered only by the updated legacy tests (Contact, AppInbox); Interaction/Deeplink `requestSync` paths are exercised indirectly and get dedicated tests when those repositories are dissolved in Phases 5–6. Not smoke-tested on a device.

### Phase 2 follow-up — code review fixes ☑
From `/code-review` of Phases 0–2:
1. Log events stored by older SDK versions are sanitized at upload time (`PushLogEventsStep` maps every message through `Redact.uploadable`).
2. Backlog above 500 log events is deleted in chunks of 200 (stops if a chunk can't be deleted).
3. A crashed contact step (device/user) aborts the sync, so a user is never sent before its device; a crashed data step continues but the sync result is `INCOMPLETE`, so the background worker retries.
4. Locking moved from the whole run to each step (`SyncDataAction.stepLock`, fair `Mutex`): `syncContactNow()` during init waits for at most one running step instead of a full sync.
5. Requested syncs wait for SDK initialization (`InitGate`, opened by `RetenoInternalImpl` when init completes; `syncNow` is not gated because registration runs during init).
6. `Redact.text` also masks e-mail addresses, long digit runs (phone numbers) and quoted JSON values.
8. `PushEventsStep` loads the events table once per round and sends every group from it (50-batch budget kept).
9. Saving the "device registered" flag and reporting removed rows are named steps.
10. `TracedOutcome.traceLabel` defaults to `toString()`; enums no longer repeat their names.
Not changed (7): one save still requests a full sync; requests are conflated and empty tables cost a few reads. Revisit only if profiling shows a cost.
`public.gpg` (public keyring only) added to `.gitignore`.
Coverage after fixes: 826/826 lines, 270/270 branches; 679 Core tests, 53 Push tests passing.

### Phase 3 — Contact, device, user ☑
Done:
- Device actions (`internal/action/device/`): `RegisterDeviceAction` (init; returns the CONTACT `SyncResult`), `EnsureDeviceRegisteredThisSessionAction` (ON_START), `UpdateFcmTokenAction`, `UpdateNotificationsEnabledAction`; shared `CurrentDevice` (injected device factory, dedupe against stored devices) and `ContactSession` (device-sent-this-session flag, replaces `ContactController.isDeviceSentThisSession`).
- User actions (`internal/action/user/`): `SetUserAttributesAction`, `SetMultiAccountUserAttributesAction`, `SetAnonymousUserAttributesAction`; shared `ContactWriter` (external id → user → device → CONTACT sync, plus account switch: stop SDK on main, clear session on IO, change suffix, write, start SDK on main), `SegmentationRefresh` (5 s debounce), `UserAttributesValidator`, `traceFields()` (names only, no values).
- `SerialQueue` (`internal/coroutine/`) keeps contact operations in call order (replaces the two action-owned `Channel` loops); a task cancelling itself no longer kills the queue.
- `SdkLifecycle` (`internal/lifecycle/`) replaces `RetenoInternalImpl.instance.stop()/start()` inside actions; `RetenoInternalImpl` implements it.
- Facade: validation runs synchronously before deferring (I2 — blank `externalUserId` / invalid `marketId` reach the caller even before init instead of crashing on Main); contact calls enqueue on `actions.contactQueue`; device id / notification channel accessors go straight to `ConfigRepository`; FCM token no longer written to the facade log.
- `ConfigRepository.getStoredFcmToken()` added; `UpdateFcmTokenAction` compares against the stored token (D4 — a token silently fetched by `getFcmToken()` no longer hides a real change). Registration paths still use `getFcmToken()` (fetch if empty).
- Registration failure (e.g. Firebase not initialised) is now a reported action failure, init continues (rest of I1).
- Anonymous attributes now request a sync after saving (previously waited for the next 30 s tick).
- Deleted: `ContactController`, `ContactRepository(Impl)`, their providers, `ServiceLocator.contactControllerProvider` (technically public), the old `Update*UserAttributesAction`s, `ContactControllerTest`, `ContactRepositoryImplTest`, 6 legacy facade contact tests.
- Side fix: `RetenoSessionHandlerImpl.trackSessionEvent` uses `Reteno.instanceInternal?` — removes the long-standing flaky Push failure (proper fix in Phase 4).
Not done (moved): `ConfigRepositoryImpl` still serves lifecycle/IAM/push code; it shrinks as those phases land.

Final Coverage Report (Phase 3) — Kover gated packages: 1122/1122 lines, 326/326 branches.
- [x] Device dedupe, default subscription from settings, IO, trace — `CurrentDeviceTest`
- [x] Registration: token → device → CONTACT sync → session flag; aborted result; token failure reported, nothing sent — `RegisterDeviceActionTest`
- [x] Once per session, synced devices forgotten (only synced, none → no delete), request sync — `EnsureDeviceRegisteredThisSessionActionTest`
- [x] Token unchanged/changed, compares stored token without fetching, token never traced — `UpdateFcmTokenActionTest`
- [x] Notifications unchanged/changed, device with new subscription — `UpdateNotificationsEnabledActionTest`
- [x] External id changed/unchanged, user valid/invalid/absent/attribute-less, account switch order + threads + session flag, no restart when suffix unchanged — `ContactWriterTest`
- [x] Single vs multi account suffix, segmentation only when started, redacted trace — `SetUserAttributesActionTest`, `SetMultiAccountUserAttributesActionTest`
- [x] Anonymous valid/invalid, IO, trace without values — `SetAnonymousUserAttributesActionTest`
- [x] Debounce, refresh failure reported and later refresh still runs — `SegmentationRefreshTest`
- [x] Validation messages and boundaries — `UserAttributesValidatorTest`; synchronous throw from facade before init — `RetenoInternalImplUserValidationTest`
- [x] Order, failure isolation, self-cancelling task, scope cancellation — `SerialQueueTest`
- [x] Wiring of every contact action, segmentation → IAM controller, account switch → session handler, INFO reports, queue failure logged — `ActionsLocatorContactWiringTest`
- Gaps: not smoke-tested on a device; legacy facade tests for user attributes were deleted, the facade is fully re-tested in Phase 4.

### Phase 4 — SDK init, config, start/stop, session, lifecycle ☑
Test infrastructure goal: **remove `runRetenoTest` from Core.** `RetenoInternalImpl` gets an internal constructor taking its collaborators (an `SdkActions` interface implemented by `ActionsLocator`, `InitGate`, scope, lifecycle owner); facade tests construct it directly with a recording fake — no `mockkConstructor(ServiceLocator)`, no init loop, no static instance, no MockK on gated classes. Delete `RetenoImplTest` and Core's `runRetenoTest`/`createRetenoAndAdvanceInit`.
Actions (`internal/action/init|lifecycle/`): `InitializeSdkAction` (cached-config boot, WorkManager/prefs warm-up), `ApplyConfigAction` (`setConfig`), `StartSdkAction` (ON_START), `StopSdkAction` (ON_STOP), `SetLifecycleEventConfigAction`, `SetSessionDurationAction`, `EnableAutoScreenTrackingAction`, `UpdatePushPermissionStatusAction`, `AppForegroundedAction` / `AppBackgroundedAction` (what `AppLifecycleController.start/stop` do: resume broadcast, install/update metadata events, html preload, open/background events), session loop as a tracked `Job` in `SessionTicker`.
Fixes: I1 (init completes with a failed-registration state; registration retried by sync), I3 (timeouts for device-id polling, D5), I4 (`StartSdkAction` job cancelled by `StopSdkAction`; atomic config guard), E1 (cancel previous session job), I6 (inbox/recom gated on `InitGate`).
`RetenoInternalImpl` becomes a thin facade: validate synchronously → `scope.launch { gate.await…(); action(...) }`.
Tests: cached vs user config ordering, user config replaces device-id mode, registration failure still opens the gate, start→stop before fetch completes cancels IAM/scheduler start, session start/end events at reset duration boundaries (virtual time), double start → one ticker.

Progress (stages, each kept green):
- ☑ **4a Session.** `SessionTracker` (`internal/lifecycle/`) replaces `RetenoSessionHandlerImpl`: injected prefs, shared `LifecycleEventFilter`, `logEvent`, scope, clock and session-id factory. A second `start()` while running is ignored (E1: no double loop), `SessionEnded` only when a previous session id exists (no bogus end event on first install), the loop runs on an owned `SupervisorJob` scope (no thread leak). `RetenoSessionHandlerProvider` is a `ProviderStrongReference` (`get()` now `@Synchronized`).
- ☑ **4b App lifecycle & screens.** Actions (`internal/action/lifecycle/`): `AppForegroundedAction` (resume broadcast → outdated-data cleanup → install/update event + saved version → html preload → `ApplicationOpened` once per foreground), `AppBackgroundedAction`, `TrackPermissionDialogAction`, `SetLifecycleEventConfigAction` (one shared filter for app and session events), `EnableAutoScreenTrackingAction` (starts the fragment watcher once), `UpdatePushPermissionStatusAction`. Helpers: `LifecycleEvents` (filter + named step), `AppForegroundState`, `ScreenTracker`. Android access sits behind `AppPlatform` (`internal/platform/`: `AppBroadcaster`, `AppInfo`, `TopActivity`, `FragmentScreenWatcher`, `AppLifecycleWatcher`, `AndroidAppPlatform`), so `ActionsLocator` tests use `FakeAppPlatform`. Foreground/background/permission tasks run in order on a lifecycle `SerialQueue`, the I/O ones on `dispatchers.io`. The push-subscription lifecycle event moved into `UpdateNotificationsEnabledAction` (the `notificationState` flow observer is gone; the property stays on `ConfigRepository`).
  - Deleted: `AppLifecycleController`, `ScreenTrackingController`, their providers, `ServiceLocator.appLifecycleControllerProvider` (technically public — CHANGELOG), 3 legacy facade tests. The sample app's lifecycle screen uses `Reteno.setLifecycleEventConfig` instead of reflection.
  - Leftover for Phase 10: `ConfigRepository.getAppPackageInfo()` and `notificationState` are now unused by SDK code.
- ☑ **4b follow-ups found in 4c review.** Lifecycle and screen-view events are only produced after init (lifecycle tracking starts inside `ApplyConfig` after the gate opens; screen tracking and notification state come through the facade's `runAfterInit`; the session tracker ticks only after `StartSdk`), so they don't wait on the gate themselves. The gate is awaited only at entry points outside the facade gate: `StartStopSequencer` (process lifecycle), `SyncCoordinator` (`forcePushData()` and legacy sync requests), `BackgroundSyncAction` (cold-start worker). `LifecycleEventFilter` is created with defaults and updated by `ServiceLocator.setConfig` (it no longer reads `configProvider.get()` lazily, which would throw if `actions` was built before any config; a user config now also overrides cached tracking options).
- ☑ **4c Init/config.** Actions (`internal/action/init/`): `BootSdkAction` (warm-up as a child action whose failure is reported but doesn't stop boot → read cached config → restore device id → apply cached config), `ApplyConfigAction` (use config → child `ApplyConfigOptions`: init device id for the configured mode, wait for it, cache config, session duration — failures reported, the gate still opens → register device (user config) → open init gate → in-app pause options → start app lifecycle tracking (user config) → process cached push interactions), `StartSdkAction`, `StopSdkAction`, `SetSessionDurationAction`. Boot and config run in order on an init `SerialQueue` on `dispatchers.main`; the user config is also set synchronously so it's readable right after `setConfig` (a cached boot that runs later doesn't overwrite it). `StartStopSequencer` (`internal/lifecycle/`) keeps start/stop requests in call order, waits for user init, and a stop cancels a start that hasn't finished (I4); `SdkRunState` replaces `isStartedAtomic`. `InitGate` replaces the three `CompletableDeferred`s and the ANR wait; it releases waiters in FIFO order, so API calls made before init keep their order. Access keys are never traced.
- ☑ **4d Thin facade.** `RetenoInternalImpl` has an internal constructor (`ServiceLocator`, `sdkActions: (ServiceLocator) -> SdkActions`, `isOsSupported`); the public constructor keeps its signature. `SdkActions` (`di/`), implemented by `ActionsLocator`, is the facade's only path into Phase 1–4 features (`submit*` fire-and-forget calls, boot/config/start/stop, `requestSync`). `ActionsLocator` provides its own `SdkLifecycle` to `ContactWriter` (no more `RetenoInternalImpl.instance` in the wiring) and gets its scope injected. Every deferred API checks the OS version consistently (`notificationsEnabled`, `updatePushPermissionStatus`, `setSessionDuration` didn't).
  - Deleted: `RetenoImplTest`, Core's `runRetenoTest` / `createRetenoAndAdvanceInit`, the relaxed `ActionsLocator`/`DataSync` mocks in Core's `BaseRobolectricTest`; `RetenoActivityHelperActivityTest`, `DbUtilDeviceTest` and `IamViewImplTest` no longer need the facade.
  - A gated class loaded in a Robolectric sandbox made Kover drop later JUnit 5 hits, so the gate depended on test order; facade tests no longer build real actions.
  - D5 (device-id polling timeout) still not implemented, as decided.

- ☑ **Logic encapsulated in actions (Phases 2–4, after review).** Actions no longer receive behaviour as lambdas built in `ActionsLocator`/`ServiceLocator`; they take their real collaborators (`SharedPrefsManager`, `RestConfig`, `ConfigRepository`, `RetenoSessionHandler`, `IamController`, `IamView`, `InteractionCacheStore`, `AppInboxRepository`, `RetenoDatabaseManager`, `RetenoActivityHelper`, `InitGate`, other actions) and call them inside their own steps. New shared pieces: `SdkConfiguration` (`internal/config/`: active config, user-config-wins rule, debug flag), `RemoteLog`/`LoggerRemoteLog` (`internal/log/`), `AppLifecycleTracking` (`internal/lifecycle/`), and `AppPlatform` methods for WorkManager init, background sync scheduling and device description. `StartStopSequencer` is the `SdkLifecycle`; `SerialQueue` logs its own failures. Function types remain only for time and identity sources (`clock`, `jitter`, id generators) and platform event callbacks. Collaborators that tests replace sit behind interfaces in the same file (`RegisterDevice`/`RegisterDeviceAction`, `SyncScheduler`/`DataSync`, `SdkConfiguration`/`SdkConfigurationImpl`, …); tests mock the interfaces, never concrete gated classes, so the gate runs in one JVM (~2.5 min). `forcePushData()` now waits for init like every other facade call. Fixed on the way: `DataSync.clearOldData()` recursion caused by a property/function name clash (would have rescheduled itself forever and never cleaned up).

Final Coverage Report (Phase 4) — Kover gated packages (+ `internal.action.init`, `internal.action.lifecycle`, `internal.platform`): 1713/1713 lines, 424/424 branches; Core 822 tests, Push 53/53 (two runs, no flaky failure).
- [x] Session start ignored while running, end event only with a previous session, expiry, foreground time, timed in-apps, stop before start, cleared session, shared filter — `SessionTrackerTest`, `LifecycleEventFilterTest`
- [x] Resume broadcast → cleanup → install/update/unchanged → preload → opened once / from background — `AppForegroundedActionTest`; backgrounded with whole seconds or not in foreground — `AppBackgroundedActionTest`
- [x] Permission dialog on top / other / unknown — `TrackPermissionDialogActionTest`; lifecycle options, auto screen tracking (watcher started once, no screen names traced), push permission broadcast — their action tests
- [x] Enabled/disabled lifecycle event as a named step — `LifecycleEventsTest`; foreground state transitions — `AppForegroundStateTest`; trigger/exclusion/enable — `ScreenTrackerTest`
- [x] Android adapters: explicit broadcasts with stopped packages, version read + unknown package + missing name, top activity on M+ and pre-M, missing manager, fragment watcher start/stop/non-fragment activity, process lifecycle + paused activity — `internal/platform/*Test` (Robolectric)
- [x] Boot with/without cache, warm-up failure continues, no access key traced — `BootSdkActionTest`; user vs cached config order and threads, device-id mode, user config kept over cached, option failure still opens the gate — `ApplyConfigActionTest`
- [x] Start order/threads, in-apps fetched once until stopped — `StartSdkActionTest`; stop order — `StopSdkActionTest`; session duration — `SetSessionDurationActionTest`
- [x] Start waits for user init, stop cancels an unfinished start, call order kept — `StartStopSequencerTest`; FIFO release of init waiters — `InitGateTest`
- [x] Wiring: lifecycle watcher → ordered foreground/background/permission, events held until init, filter shared — `ActionsLocatorLifecycleWiringTest`; boot/config/start/stop/session duration — `ActionsLocatorInitWiringTest`; every `submit*` port — `ActionsLocatorFacadeWiringTest`; account switch through the locator's own start/stop — `ActionsLocatorContactWiringTest`
- [x] Facade: boot + lifecycle observer, first config only, every deferred API held until init in call order, immediate after init, sync not gated, after-init callbacks, state, unsupported OS keeps config only — `RetenoInternalImplTest`; synchronous validation — `RetenoInternalImplUserValidationTest`
- Gaps: not smoke-tested on a device; `RetenoInternalImpl` itself is not in the Kover gate yet (its IAM/inbox/interaction members move in Phases 5–8); D5 not implemented.

### Phase 5 — Interactions, deeplinks, Push & FCM modules ☑
Done:
- Core actions: `internal/action/interaction/` — `RecordPushInteractionAction` (every push status: save request → wait for init → process), `ProcessCachedInteractionsAction`, `RecordInteractionAction` (DELIVERED/CLICKED/OPENED with the push token, fetched on IO if not stored; no token → not recorded, row kept), `RecordInteractionClickAction` (CLICKED + action, no token needed), `RecordInAppInteractionAction` (save + sync); `internal/action/deeplink/TrackDeeplinkClickAction` (blank link ignored, save + sync, link never traced); `internal/action/push/` — `InitializeIamFromPushAction` (main), `SaveDefaultNotificationChannelAction` (IO). Wired in `ActionsLocator` as `submit*` ports (`SdkActions`), each fire-and-forget on the SDK scope.
- **D6 implemented as recommended (fixes P4; to confirm):** every push interaction status is first persisted (interaction-request table), then processed once the init gate is open — immediately after init, so rows also survive a process death after init — and again in `ApplyConfig` for leftovers. Processing runs under a `Mutex`, records each row through `RecordInteraction`, deletes it only when recorded (a missing token keeps it), and requests one sync. `InteractionCacheStore` (and its `RetenoInternalImpl.instance` call) is gone. Clicks with an action can't be stored in that table and still wait in memory for init.
- `IamViewImpl` records in-app interactions and clicks through `InteractionSubmitter` (implemented by `ActionsLocator`) instead of launching them on `iamShowScope`, which `pause()` cancels on ON_STOP.
- **Push/FCM entry point:** `RetenoInternalFacade` is now public with `@InternalRetenoApi` (`RequiresOptIn`, level ERROR; Core/Push/FCM opt in module-wide) and gained `application` and `updatePushPermissionStatus()`. `RetenoInternalEntryPoint.facade` resolves it (`install(fake)` for tests, `install(null)` restores the SDK instance). No `RetenoInternalImpl.instance` / `Reteno.instance` left in Push or FCM.
- Push rewritten against the entry point: one `NotificationClickHandler` shared by `RetenoNotificationClickedActivity`/`Receiver` (record click unless in-app, dismiss button notification, notify host receiver, deeplink click + open link with app fallback, in-app from push); `Util` takes a `Context`; `RetenoNotificationHelper` takes a `NotificationImages` source (`RemoteNotificationImages` in production).
- Fixes: **P1** — the default channel is always created, even before init: host channel config when known (`SdkConfiguration.config`, null before any config), otherwise stored → bundled → fallback description (invalid JSON no longer crashes). **P2** — image downloads use 3 s connect / 5 s read timeouts (`BitmapUtil.openImageConnection`), accept only http/https, stop above 5 MB (declared or streamed), always disconnect, and the big picture is downloaded once instead of twice; a carousel image that can't be decoded is skipped instead of throwing. **P3** — delete intents use the notification id as request code (verified: the test fails with the old `0`). Also: `RetenoNotifications.updateDefaultNotificationChannel(name)` wiped the channel description (and vice versa) — it now starts from the existing channel; malformed `custom_data`/carousel JSON no longer breaks the notification; FCM token values are no longer logged by the FCM service/handler.
- Deleted: `InteractionController`, `DeeplinkController`, `InteractionRepository(Impl)`, `DeeplinkRepository(Impl)`, `InteractionCacheStore`, their providers, `ServiceLocator.interactionControllerProvider` / `deeplinkControllerProvider` / `currentConfig` (first two technically public — CHANGELOG), the unused `ApiClient` sync/put variants, 4 legacy Core test files, and the whole legacy Push test tree (`BaseRobolectricTest`, `RetenoTestApp`, `runRetenoTest`, `createReteno()` spy, PowerMock `Whitebox` uses).
- Gates: Core adds `internal.action.interaction|deeplink|push` and `RetenoInternalEntryPoint`; **D11 implemented as recommended: Push and FCM are gated as whole modules** (only generated `R`/`BuildConfig` excluded). FCM got JUnit 5/Robolectric/Kover and its first tests.
Review follow-up (`/code-review` of Phase 5): restored conditional vibration pattern (a null pattern turned vibration off), interaction token fetched again when not stored (dropping deliveries was worse than the fetch; D4 now reads "fetch only where an interaction or registration needs it"), token read moved to IO, the cache → await → process sequencing moved out of `ActionsLocator` into `RecordPushInteractionAction`, image size/scheme limits, button extras written only when present (host receivers may check `containsKey`). Kept by decision: the default channel is created before init from bundled/stored data (P1) — hosts must set `defaultNotificationChannelConfig` before the first push, because Android lets apps change only a channel's name and description later; action-button clicks before init stay in memory (the request table has no action column, DB frozen).
Behaviour changes to announce: technically-public `ServiceLocator.interactionControllerProvider` / `deeplinkControllerProvider`, `InteractionController`, `DeeplinkController`, `InteractionCacheStore` and the `DeeplinkRepository`/`InteractionRepository` interfaces removed; `RetenoInternalFacade` is new opt-in API; `RetenoNotificationHelper`/`BitmapUtil` callers see timeouts.

Final Coverage Report (Phase 5, after review fixes) — Kover: Core gated packages 1825/1825 lines, 444/444 branches (861 tests); Push module 461/461 lines, 232/232 branches (102 tests); FCM module 37/37 lines, 8/8 branches (7 tests); fixtures self-tests and `:app:compileDebugKotlin` green.
- [x] Interaction saved with token and time on IO, token read on IO, no token → not recorded, insert failure → one report, token redacted — `RecordInteractionActionTest`; request saved on IO then processed only after the gate opens / right away after init — `RecordPushInteractionActionTest`
- [x] Click saved with action without token, `forcePush` sync, URL not traced — `RecordInteractionClickActionTest`; in-app interaction save + sync, failure → no sync, description not traced — `RecordInAppInteractionActionTest`
- [x] Every row recorded and deleted, one sync, empty → nothing, failed or tokenless row kept, concurrent runs record once — `ProcessCachedInteractionsActionTest`; push interaction recorded through its stored request, kept until init — `ActionsLocatorFacadeWiringTest`; ApplyConfig runs it — `ApplyConfigActionTest`, `ActionsLocatorInitWiringTest`
- [x] Deeplink save + sync, blank ignored, no URL in trace — `TrackDeeplinkClickActionTest`; in-app from push on main, channel save on IO — `push/*ActionTest`
- [x] Every new `submit*` port and the in-app submitter — `ActionsLocatorFacadeWiringTest`; facade: push interaction passed on without waiting for init, unsupported OS no-op, new calls held until init in order, channel config null before configuration — `RetenoInternalImplTest`; `SdkConfiguration.config` — `SdkConfigurationTest`; entry point install/fallback — `RetenoInternalEntryPointTest`; download timeouts and http(s)-only — `BitmapUtilImageConnectionTest`
- [x] Push service: token → facade, notification shown on default channel, DELIVERED only with notifications and channel enabled, host PushReceived forward, push without id → custom broadcast — `RetenoNotificationServiceTest`
- [x] Channel: bundled/stored/vibration without pattern/invalid stored/unreadable/invalid bundled/host config/pre-O, enabled checks incl. missing id, missing channel, blocked, host config re-applied — `RetenoNotificationChannelTest`
- [x] Click receiver and activity: click, button click + dismiss, no id, host forward without notification id, open app with extras, deeplink + open, unresolvable link → app, in-app with/without activity shown, in-app flag without id, no extras, no intent, no launch activity, SDK failure swallowed, unsupported OS — `RetenoNotificationClicked{Receiver,Activity}Test`; link fallback order — `IntentHandlerTest`, `UtilTest`
- [x] Notification builder: id, title/app name, short/long text, big picture/failed/blank, carousel/failed image/blank/invalid JSON, badge, icon/color meta-data, ≤3 buttons with links and nested custom data, absent button fields not written, trampoline receiver vs activity, per-notification delete intents (P3), no delete receiver, grouping rule — `RetenoNotificationHelperTest`; real image download with local server, failure, declared and streamed size limit, non-web URLs — `RemoteNotificationImagesTest`
- [x] Receivers → `notificationsEnabled` (enabled, app blocked, channel blocked, SDK broadcasts, null context, failure, unsupported OS) — `NotificationStateReceiversTest`; host event receivers and listeners incl. empty extras and in-app custom data — `HostEventReceiversTest`
- [x] `RetenoNotifications`: channel rename/describe keeps the other field, creation when missing, permission request without activity / before 13 / already granted / granted → status reported / denied, futures, every permission status branch, grouping rule — `RetenoNotificationsTest`
- [x] FCM: reteno push shown + DELIVERED, other message → custom broadcast, token, `isRetenoMessage`, failures swallowed, unsupported OS, host handler forwarding — `RetenoFirebaseMessagingServiceTest`
- Gaps: not smoke-tested on a device. Push/FCM tests were written against the rewritten code (characterisation of kept behaviour); red was observed for the Core actions (compile), the channel-update bug and P3 (mutation check), not for every Push branch. Android 13 branches are covered by pinning `SDK_INT` on the API 26 framework (Robolectric 4.8.1 has no API 33). Carousel downloads are still sequential, so many slow images can add up to several timeouts.

### Phase 6 — App Inbox ☑
Done (`internal/action/inbox/`):
- `GetInboxMessagesAction` (page / pageSize / status filters), `GetInboxMessagesCountAction`, `MarkInboxMessageOpenedAction` (save opened status for the current device → request sync), `MarkAllInboxMessagesOpenedAction` (POST all opened → clear local statuses → refresh count). Each waits for the init gate as a named step (**fixes I6**: inbox calls before init no longer go out without device id / access key).
- `passResponseToHost` (`HostResponse.kt`): host callbacks always on main; server failure → `onFailure(code, body, error)`, traced but not uploaded (old behaviour); unreadable response → `onFailure(null, null, error)` **and** one reported action failure (**fixes B2**: the callback is always answered).
- `InboxCountPoller` (`InboxCountPolling`) replaces the repository's scheduler and `WeakHashMap`: subscribers held strongly until unsubscribed (**fixes B1**), one 30 s polling `Job` only while there are subscribers and the app is in foreground (**D8 implemented as recommended, to confirm**: `StartSdkAction` resumes, `StopSdkAction` pauses), counts emitted only when they change, last count replayed on main to late subscribers, everything forgotten when the last subscriber leaves, `refresh()` used by `PushInboxStatusStep` and mark-all. There is now one poller per SDK instead of possibly two repository instances behind weak-reference providers (I5 for inbox).
- `AppInboxImpl` is a thin facade over the `InboxActions` port (`SdkActions` extends it; `ActionsLocator` launches the actions on its scope); `RetenoInternalImpl.appInbox` builds it directly.
- Deleted: `AppInboxController`, `AppInboxRepository(Impl)`, their providers, `ServiceLocator.appInboxProvider` / `appInboxRepositoryProvider` (internal), 4 legacy test files (47 legacy tests). Callbacks of `subscribeOnMessagesCountChanged` now receive the replayed value asynchronously on main instead of synchronously on the caller's thread.
- Gate: `internal.action.inbox` and `AppInboxImpl` added.

Final Coverage Report (Phase 6) — Kover: Core gated packages 1994/1994 lines, 494/494 branches (851 Core tests); Push 102 and FCM 7 tests still at 100 %; `:app:compileDebugKotlin` green.
- [x] Query params mapping incl. no filters, messages on main, server failure on main and not reported, unreadable response → `onFailure` + one report at the read step, waits for init, no response content in trace — `GetInboxMessagesActionTest`
- [x] Count on main, network failure passed through, waits for init — `GetInboxMessagesCountActionTest`
- [x] Opened status saved for current device on IO + sync, waits for init — `MarkInboxMessageOpenedActionTest`; mark all: request body, clear local on IO, refresh count, success/failure on main, failure keeps local statuses and isn't reported, waits for init — `MarkAllInboxMessagesOpenedActionTest`
- [x] Poller: immediate first fetch, 30 s cadence, change-only emission, failure to every subscriber, replay on main, duplicate subscribe, partial unsubscribe keeps polling, last unsubscribe stops and forgets, unsubscribe-all, background no polling, pause/resume, resume without subscribers, subscriber removed in background, refresh while polling / not polling, late count after everyone left dropped — `InboxCountPollerTest`
- [x] Facade mapping + unsupported OS — `AppInboxImplTest`; locator ports (page, count, subscribe after start, unsubscribe, opened/mark-all) — `ActionsLocatorInboxWiringTest`; Start/Stop resume/pause order — `StartSdkActionTest`, `StopSdkActionTest`; sync step refresh — `PushInboxStatusStepTest`, `ActionsLocatorSyncWiringTest`
- Gaps: `RetenoInternalImpl.appInbox` is not tested directly (building `AppInboxImpl` in the Robolectric facade test would load a gated class in a sandbox); not smoke-tested on a device.

### Phase 7 — Recommendations ☐
Actions (`internal/action/recommendation/`): `FetchRecommendationAction<T>` (typed with JSON fallback), `FetchRecommendationJsonAction`, `LogRecommendationEventsAction`.
Fixes: R1 (empty rows deleted in `PushRecomEventsStep`), failed fetch no longer produces an ERROR log event with a synthetic `Throwable("null")`.
Removes: `RecommendationController`, `RecommendationRepository(Impl)`.

### Phase 8 — In-app messages ☐ (largest; split in sub-PRs)
8a State & rules: `InAppMessagesState` (thread-safe holder: messages, waiting-for-event, scheduled, postponed, pause behaviour), pure `DisplayRulesEvaluator` (frequency/schedule/targeting/segment) with exhaustive table tests.
8b Fetch: `FetchInAppMessagesAction` (list + content + segments, ETag decision M1/D9, persists via `RetenoDatabaseManagerInAppMessages`), `RefreshSegmentationAction` (fix M4), `PreloadIamHtmlAction`.
8c Triggering: `OnEventForInAppsAction` (EventBus collector, gate opens on fetch failure too — M3), `OnSessionTimeForInAppsAction`, `ShowNextInAppAction` (counts the show only after display — M6/D10), `PauseInAppMessagesAction`, `SetInAppPauseBehaviourAction`, `PausePushInAppMessagesAction`, `SetPushInAppPauseBehaviourAction` (fix M5).
8d View: `IamViewImpl` keeps WebView/containers; readiness poll becomes a cancellable `Job` with `delay(200)` checked on main (M7); `RecordInAppInteractionAction` reuse; `ReportWidgetInitFailedAction`; host callbacks posted on main consistently; **SEC5** JSON-encoding of personalisation values.
Removes: `IamController(Impl)`, `IamRepository(Impl)`, `OperationQueue` usage in view.
Tests: every display-rule branch, pause × behaviour matrix for both sources, event-gate buffering, segment-check on event, readiness poll cancel on teardown, callback thread assertions, injection payloads (`"`, `</script>`, unicode) render as literal text.

### Phase 9 — Logger & observability ☐
`Logger` becomes a thin façade over an injected `RetenoLogSink`; persistent log events only through `LogRetenoEventAction`, recursion impossible by construction. (SEC2 PII stripping moves earlier, into Phase 2, alongside `PushLogEventsStep`.)

### Phase 10 — Removal & whole-module gate ☐
Test infrastructure goal: after the DB-manager tests are rewritten, delete Core's `BaseRobolectricTest`, `BaseUnitTest` and `RetenoTestApp`. Only self-contained Robolectric tests (own fixture, `runUnitTest`, given/whenn/then) remain.
Delete `ServiceLocator`, `di/provider/*`, `di/base/*` and the public `RetenoInternalImpl.serviceLocator` (D12): one internal composition root owns the shared infrastructure (database and managers, prefs, `RestConfig`, `RestClient`, `ConfigRepository`, `InitGate`, `SdkConfiguration`) as plain singletons (fixes I5), and the facade gets its collaborators through the internal constructor, so Robolectric facade tests no longer load gated classes. Rewrite the sample `app` against the public API only.
Delete `OperationQueue`, any leftover controllers/repositories, `RetenoThread*`; delete remaining legacy tests; rewrite DB manager, mapper, schema/DbUtil and validator tests in the new style; flip Kover rule to whole modules at 100 %; README/CHANGELOG; version bump.

### Suggested order & rationale
0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10.
Sync (2) comes right after events because every later slice's "persist then push" depends on `SyncCoordinator`. Init (4) after contact (3) because `ApplyConfigAction` calls `RegisterDeviceAction`. IAM (8) last among features: biggest, most stateful, depends on events, user, session and interactions.

## 6. Per-action Definition of Done
- [ ] One file under `internal/action/<feature>/`, `internal` visibility, explicit deps, `suspend operator fun invoke`, `runner(...)`.
- [ ] Work expressed as named `step(...)`s (§3.3); args/details go through `Redact` helpers; logcat output checked once on the sample app.
- [ ] No `Reteno.instance` / `RetenoInternalImpl.instance` / `ServiceLocator` access inside.
- [ ] DB access only through existing `RetenoDatabaseManager*` on `dispatchers.io`.
- [ ] Contract checklist written first; Final Coverage Report in the PR description.
- [ ] 100 % line/branch for the file in Kover.
- [ ] Old code path and its legacy tests deleted in the same PR.
- [ ] Sample `app` smoke: the feature works against the QA backend.

## 7. Decisions needed before/while starting
| ID | Question | Recommendation |
|---|---|---|
| D1 | Toolchain bump | **Decided: no bump.** No `runUnitTest` timeout; no `backgroundScope`. |
| D2 | Fix sync defects S1–S10 or keep strict parity? | **Decided: fix.** |
| D3 | Restore log-event upload (S7)? | **Decided: include in the 30 s cycle as the last step** (see Phase 2). Endpoint verified live on 2026-10-13: `POST /logs/v1/events` with `{"events":[]}` → 200 `events success create`; GET → 405. |
| D4 | Keep the "fetch FCM token if empty" side effect? | Registration and recording push interactions (Phase 5 review); other token comparisons use the stored token. |
| D5 | Timeout for CLIENT_UUID device-id polling | Bounded (e.g. 10 s) then fall back to cached/random id with an error log. |
| D6 | `InteractionCacheStore` (dead write path) — implement persistence for pre-init interactions or remove? | **Implemented as recommended in Phase 5 (to confirm):** interaction-request table before init, processed after init; store removed. |
| D7 | Kover exclusion list (BuildConfig, R, deprecated shims) | Keep minimal; deprecated shims are tested rather than excluded if cheap. |
| D8 | Inbox count polling in background | **Implemented as recommended in Phase 6 (to confirm):** paused by `StopSdk`, resumed by `StartSdk`. |
| D9 | Send ETag for in-app list (M1) | Yes, if backend supports 304 on that endpoint. |
| D10 | Count in-app show only after confirmed display (M6) | Yes. |
| D11 | Push/FCM modules in the 100 % gate from Phase 5? | **Implemented as recommended in Phase 5 (to confirm):** whole modules gated. |
| D12 | Remove `ServiceLocator`? | **Decided: yes, at the end (Phase 10).** `ServiceLocator`, `di/provider`, `di/base` and `RetenoInternalImpl.serviceLocator` are deleted (public API removal → CHANGELOG + version bump); the sample app is rewritten at the end and stops reaching SDK internals by reflection. |
