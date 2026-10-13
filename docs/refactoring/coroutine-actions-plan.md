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
- ☑ Test-only additions: JUnit 5.10.2 BOM + `junit-vintage-engine`, `de.mannodermaus.android-junit5` 1.10.0.0, Kover 0.7.6 (Core + Push). Kover needs `excludeInstrumentation` for JUnit/Robolectric/MockK packages (it broke JUnit 3 `TestCase.assertEquals`). ☐ PowerMock/Mockito removal deferred: legacy `RestClientImplTest` and two Push tests still use `Whitebox`; removed when those tests are rewritten.
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

### Phase 2 — Data sync pipeline ☐
Actions (`internal/action/sync/`): `SyncDataAction` (the `syncOnce` body), `PushDeviceStep`, `PushUserStep`, `PushInteractionsStep`, `PushInAppInteractionsStep`, `PushEventsStep`, `PushInboxStatusStep`, `PushRecomEventsStep`, `PushWrappedLinksStep`, `PushLogEventsStep`, `ClearOldDataAction` (adds log-event cleanup), `ForcePushAction`.
Infra: `SyncCoordinator`, `SyncScheduler` (30 s / 10 s debug view, 0–10 s jitter), `PushDataWorker` → `CoroutineWorker`.
Removes: `PushOperationQueue`, `ScheduleController(Impl)`, all `push*`/`clearOld*` in repositories, `RetenoThread(Factory)`.
Decisions: S1–S10 fixed (D2); log events join the 30 s cycle (D3).
`PushLogEventsStep` (D3):
- Runs **last** in `syncOnce()`, so business data (device, user, interactions, events…) is always sent first and a failing logs endpoint can't block it.
- Sends batches of 50 (`getLogEvents(50)`, oldest first since the manager orders by timestamp ASC) and loops until the table is empty or a batch fails. Today's `pushLogEvents(null)` would send the whole backlog in one POST, which for installs that accumulated rows since `60ad093` could be thousands of stack traces in one request.
- Backlog cap before pushing: if `getLogEventsCount() > 500`, delete the oldest excess with `getLogEvents(excess)` + `deleteLogEvents(...)`. This uses only existing DB manager methods (there's no delete-by-time on this manager, and it stays frozen).
- Success or non-repeatable error (4xx except 429) → delete the batch and continue. Repeatable error → keep rows, stop the step, retry next cycle.
- Its own failures are logged with `Logger.i` only, never `Logger.e`, so a failing logs endpoint can't create new log rows (no feedback loop).
- Ships together with SEC2 (PII removed from log messages); without that, restoring the upload would start sending emails/phones embedded in messages.
Tests: empty table → no request; batch size 50; drains multiple batches; cap deletes oldest excess; repeatable failure keeps rows and doesn't stop earlier steps; non-repeatable deletes; failure doesn't call `Logger.e`; after drain `hasDataForSync` reflects the empty table.
Tests per step: empty table → Continue without request; success drains all batches; non-repeatable drops batch and continues; repeatable → Abort and keeps rows; request payload mapping. Coordinator: order, abort stops later steps, conflation of N requests during a run → exactly one follow-up, cancellation on stop, scheduler cadence with virtual time. Worker: background + data → awaits sync; foreground → failure; no data → cancels unique work; Abort → retry.

### Phase 3 — Contact, device, user ☐
Actions (`internal/action/device|user/`): `RegisterDeviceAction` (init), `EnsureDeviceRegisteredThisSessionAction` (ON_START), `UpdateFcmTokenAction`, `UpdateNotificationsEnabledAction`, `SetUserAttributesAction`, `SetMultiAccountUserAttributesAction`, `SetAnonymousUserAttributesAction`, `RefreshSegmentationAction` (debounced trigger shared by both user actions; real implementation in Phase 8).
Changes: the two existing user actions stay separate files but lose their `Channel`/`retry{true}` loop in favour of a `Mutex`-serialized suspend invoke launched on `RetenoScope`; `RetenoInternalImpl.instance.stop()/start()` → injected `SdkLifecycle`; device/user network writes go through `SyncCoordinator` (S4); `getFcmToken`'s "fetch and save if empty" side-effect moves into `RegisterDeviceAction` only, so a later `onNewToken` with the same token isn't skipped (D4).
Removes: `ContactController`, `ContactRepository(Impl)`; `ConfigRepositoryImpl` shrinks to `RestConfig`/prefs access.
Tests: device dedupe vs existing rows, suffix switch restarts SDK and clears session, same suffix doesn't restart, invalid marketId rejected synchronously, segmentation refresh debounced (5 s virtual), token unchanged → no insert, token changed → insert + sync trigger, notifications toggle emits lifecycle event once.

### Phase 4 — SDK init, config, start/stop, session, lifecycle ☐
Actions (`internal/action/init|lifecycle/`): `InitializeSdkAction` (cached-config boot, WorkManager/prefs warm-up), `ApplyConfigAction` (`setConfig`), `StartSdkAction` (ON_START), `StopSdkAction` (ON_STOP), `SetLifecycleEventConfigAction`, `SetSessionDurationAction`, `EnableAutoScreenTrackingAction`, `UpdatePushPermissionStatusAction`, `AppForegroundedAction` / `AppBackgroundedAction` (what `AppLifecycleController.start/stop` do: resume broadcast, install/update metadata events, html preload, open/background events), session loop as a tracked `Job` in `SessionTicker`.
Fixes: I1 (init completes with a failed-registration state; registration retried by sync), I3 (timeouts for device-id polling, D5), I4 (`StartSdkAction` job cancelled by `StopSdkAction`; atomic config guard), E1 (cancel previous session job), I6 (inbox/recom gated on `InitGate`).
`RetenoInternalImpl` becomes a thin facade: validate synchronously → `scope.launch { gate.await…(); action(...) }`.
Tests: cached vs user config ordering, user config replaces device-id mode, registration failure still opens the gate, start→stop before fetch completes cancels IAM/scheduler start, session start/end events at reset duration boundaries (virtual time), double start → one ticker.

### Phase 5 — Interactions, deeplinks, Push & FCM modules ☐
Actions (`internal/action/interaction|deeplink|push/`): `RecordInteractionAction` (DELIVERED/CLICKED), `RecordInteractionClickAction`, `RecordInAppInteractionAction`, `TrackDeeplinkClickAction`, `ProcessCachedInteractionsAction` or removal of `InteractionCacheStore` (D6), `InitializeIamFromPushAction`, `SaveDefaultNotificationChannelAction`.
Push module: receivers/activity/`RetenoNotificationService` keep their Android entry points but delegate to the facade only; `RetenoNotificationHelper` image downloads get connect/read timeouts (P2); delete intents get unique request codes (P3); cold-start channel creation waits on `InitGate` with a bounded timeout or falls back to the raw default channel (P1).
FCM module: first tests ever (`onMessageReceived` routing, `onNewToken`, unsupported-OS guard).
Removes: `InteractionController`, `InteractionRepository(Impl)`, `DeeplinkController`, `DeeplinkRepository(Impl)`, `ApiClient` async variants.
Tests: blank token drops DELIVERED but not CLICK action, forcePush flag triggers sync, blank wrapped link ignored, `http://` link handled as non-repeatable (D2), receiver → facade wiring per Android component.

### Phase 6 — App Inbox ☐
Actions (`internal/action/inbox/`): `GetInboxMessagesAction`, `GetInboxMessagesCountAction`, `MarkInboxMessageOpenedAction`, `MarkAllInboxMessagesOpenedAction`, `SubscribeInboxCountAction` / `UnsubscribeInboxCountAction` backed by an `InboxCountPoller` (`Job` + `delay(30 s)`, emits only on change, stops when no subscribers; optional pause in background, D8).
`AppInboxImpl` keeps its public callback API and maps `Result` → `onSuccess`/`onFailure` on main.
Fixes: B1 (subscriber set with strong refs and explicit unsubscribe), B2.
Removes: `AppInboxController`, `AppInboxRepository(Impl)`.
Tests: query params mapping, callback thread is main, parse error → `onFailure`, polling cadence, change-only emission, last value replay to late subscriber, unsubscribe-all stops polling.

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
Delete `OperationQueue`, any leftover controllers/repositories, `RetenoThread*`, `ProviderWeakReference` for stateful singletons (switch to `ProviderStrongReference`, I5); delete remaining legacy tests; rewrite DB manager, mapper, schema/DbUtil and validator tests in the new style; flip Kover rule to whole modules at 100 %; README/CHANGELOG; version bump.

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
| D4 | Keep the "fetch FCM token if empty" side effect? | Only during registration. |
| D5 | Timeout for CLIENT_UUID device-id polling | Bounded (e.g. 10 s) then fall back to cached/random id with an error log. |
| D6 | `InteractionCacheStore` (dead write path) — implement persistence for pre-init interactions or remove? | Implement via `RetenoDatabaseManagerInteraction` insert before init (DB unchanged), remove the store. |
| D7 | Kover exclusion list (BuildConfig, R, deprecated shims) | Keep minimal; deprecated shims are tested rather than excluded if cheap. |
| D8 | Inbox count polling in background | Pause on ON_STOP, resume on ON_START. |
| D9 | Send ETag for in-app list (M1) | Yes, if backend supports 304 on that endpoint. |
| D10 | Count in-app show only after confirmed display (M6) | Yes. |
| D11 | Push/FCM modules in the 100 % gate from Phase 5? | Yes. |
