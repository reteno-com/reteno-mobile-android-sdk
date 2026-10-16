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
| SEC9 | In-app custom data (`customData` + url + in-app id, may hold personalised values) was broadcast to **every** receiver on the device registered for `com.reteno.custom-inapp-data`, incl. exported receivers of other apps; the custom push broadcast (`com.reteno.custom-push`, whole push payload) still is | `IamViewImpl.tryHandleCustomData` (removed), `RetenoNotificationService.sendCustomPushBroadcast` | In-app data fixed in 8d (`AppBroadcaster.sendToApp`, `setPackage(packageName)`); apply the same to the custom push broadcast (Push module) — raise with the SDK owner since apps relying on cross-package delivery would break. |
| SEC8 | `POST /logs/v1/events` accepts requests with **no authentication** (SDK sends no access key for `ApiContract.LogEvent`; verified 2026-10-13 with an empty `events` list only) → likely anyone can write arbitrary records into the logs store | `RestClientImpl.kt` header `when`, backend | Raise with backend owner: rate limiting / payload limits at minimum, or require the access key header (SDK change in Phase 2 if backend agrees). |

SEC1–SEC3, SEC5, SEC8 and SEC9 should be raised with the security team / SDK owner, not only fixed in code.

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
        step("save event to local database") {
            withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) }
        }
        step("notify in-app message triggers") {
            eventBus.emit(event)
        }
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
    step("save event to local database") {
        withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) }
    }
    step("notify in-app message triggers") {
        eventBus.emit(event)
    }
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

### Phase 7 — Recommendations ☑
Done (`internal/action/recommendation/`):
- `FetchRecommendationAction` (generic `invoke<T : RecomBase>`): wait for init → POST the request for the variant → recommendations parsed into `responseClass` go to `onSuccess`; a response that can't be read into it goes to `onSuccessFallbackToJson(body)` (documented contract, so the action succeeds and nothing is reported; the failed read step still shows in the trace); server failure → `onFailure(code, body, error)`, traced, not reported. All host callbacks on main.
- `FetchRecommendationJsonAction`: same request, raw body → `onSuccess`, server failure → `onFailure` (not reported).
- `LogRecommendationEventsAction`: saves the events on IO; a null or empty event list is skipped with a note instead of reaching the DB manager (which logged it as an ERROR remote event). No sync request and no init wait — same as before, the 30 s cycle sends them.
- `RecommendationImpl` is a thin facade over the `RecommendationActions` port (`SdkActions` extends it; `ActionsLocator` launches the actions on its scope); `RetenoInternalImpl.recommendation` builds it directly. `ActionsLocator` now holds one `recomEventsDb` for cleanup, sync and logging.
- Fixes: **I6** for recommendations (fetches before init are held until the gate opens instead of going out without device id / access key); failed fetch no longer produces an ERROR log event with a synthetic `Throwable("null")`; **B2** for recommendations (parsing happens inside the action, so the callback is answered once the SDK is initialized — a fetch made before init is held, with no timeout, until init completes; see open decisions below). **R1**: the "empty rows" part was already closed in Phase 2 (`PushRecomEventsStep` deletes all-empty groups, the DB manager never returns empty groups) and empty groups can no longer be logged; the batch-limit part is not done (see gaps).
- Deleted: `RecommendationController`, `RecommendationRepository(Impl)`, `RecommendationControllerProvider`, `RecommendationRepositoryProvider`, `RecommendationProvider`, `ServiceLocator.recommendationProvider` (internal), 3 legacy test files (9 legacy tests), the `recommendation` mock in `BaseRobolectricTest`.
- Gate: `internal.action.recommendation` and `RecommendationImpl` added.

Review follow-up (`/code-review` of Phase 7):
- The request is serialized in `RecommendationImpl` on the caller's thread, as before the migration, so a host that clears or reuses its `products`/`fields` lists after the call no longer changes (or breaks) the request; actions take the JSON body.
- Recommendation events are saved through a `SerialQueue` (`RecommendationEvents`): the frozen DB manager does an unguarded check-then-insert of the variant row and shares one `ContentValues`, so parallel saves for a new variant could drop events (also true before the migration).
- `stepOrNull` (`internal.action.trace`) reads with a fallback and rethrows `CancellationException`; `runCatching { step(…) }` turned cancellation into the fallback path.
- The variant id is URL-encoded in `ApiContract.Recommendation.GetRecoms`: it was put into the request path raw, so an id like `../../v1/user?x=` sent the request (with the access key) to another endpoint on the same host. Pre-existing; low risk because the id comes from the host app, but worth mentioning to the security owner.
- The two fetch actions' private host-answer helpers are named `answerHost`, so they no longer shadow the inbox `passResponseToHost`. The init wait and the request stay written out in each action (a shared helper was tried and reverted: it hid the steps from a top-to-bottom read).

### Legacy test migration audit (after Phase 7) ☑
Every legacy test method deleted or changed on the branch (27 deleted files, 17 modified, about 320 methods) was mapped to the new test that asserts the same behaviour. Almost everything is covered, mostly more strictly; dropped tests match decisions above (queue chaining, 1 s force-push throttle, fire-and-forget pushes, PII in log messages). Fixed from the findings:
- **Default notification channel fallback restored** (regression from Phase 5): reading the stored channel, reading the bundled `default_channel` resource and storing it are each guarded again as on master, so a failure falls back to the bundled description or the fallback channel instead of dropping the push (FCM handler path) — `RetenoNotificationChannelTest`.
- **`AppInboxImpl` / `RecommendationImpl` catch failures again** (as on master): a failing SDK call (including request serialization, which runs on the caller's thread since Phase 7) never reaches the host; it is reported once through `RemoteLog.error("<Facade>.<call> failed: <redacted summary>")` — `AppInboxImplTest`, `RecommendationImplTest`.
- **Kept by decision:** `RetenoNotificationService` recognises a Reteno push only when `es_interaction_id` holds a string (`getString(...) != null`); on master `containsKey` sent a push whose key is present but null down the Reteno path. Such a push is now handled as a custom push.
Corrections: Phase 4 "sync not gated" is stale — `forcePushData()` waits for init; the 5 files Phase 0 called broken on master compiled on master and broke at `60ad093` on this branch; Phase 6 deleted 3 inbox test files and rewrote the fourth.
Coverage gaps found by the audit — closed in the test-quality pass below, except `RetenoInternalImpl.backgroundSync`/`appInbox`/`recommendation` wiring (the Robolectric facade test must not load gated classes; resolved by D12 in Phase 10) and `IamViewImpl` interaction recording (Phase 8).

### Readability pass (after Phase 7) ☑
Every action, sync step and helper that runs steps was reformatted so it reads top to bottom (rules in `AGENTS.md` → "An action reads top to bottom like its spec"): step and runner bodies always on their own lines (every one-line step and runner body rewritten), conditional steps as braced blocks, a step result used in a condition named first (`BackgroundSyncAction`), every line within 120 columns (long `api.request`/`args`/signatures/constructor calls wrapped, locator constructor calls with named arguments), consistent blank lines. Small readability extractions: `CurrentDevice.withoutLocalState()`, the opened inbox message built as a named value, `SyncDataAction`'s loop variable renamed `syncStep` so it isn't confused with a trace step. No behaviour change.
Tests got the same pass: the 142 new-style test and fixture files on the branch (legacy-style tests awaiting their Phase 8/10 rewrite left as they are) — runner/step bodies on their own lines, 120 columns, wrapped calls or named `expected` values, given/whenn/then spacing, `SutFixture` first and constants last. Assertion/verify/stub tokens per file are unchanged against HEAD apart from the new tests above. Gate after both passes: Core 2215/2215 lines, 494/494 branches (872 tests; branches fell from 510 because the facades check the OS in one place), Push 105 and FCM 7 tests at 100 %.
Test-quality pass (after the readability pass) ☑ — test-only, no production change:
- Fixed: `ActionsLocatorSyncWiringTest` is plain JUnit 5 now (it no longer needed Robolectric) and its self-comparing assertion became a real one; `TopActivityTest` back on the single sdk 26 sandbox with an `SDK_INT` pin; `SuspendApiClientTest` asserts type and message; no `advanceUntilIdle()`; `PushDataWorkerTest` asserts inside `runUnitTest`; misleading names fixed; multi-behaviour tests split; `by lazy` identity tests removed.
- Gaps closed: user merge keeps synced attributes (both persist paths, literal expected row); device/user wire JSON pinned to literal field names; lifecycle event parameters, version/build saved while events are disabled, per-option `toTypeMap` mapping; `FakeSdkActions` records forwarded objects and callbacks (`assertSame` in facade tests); invalid user input never forwarded after init; notifications false→true saved value and `pushSubscribed`; background sync keeps periodic work in foreground/before init; retention as literal 24 h / 1 h; non-repeatable interaction + failed delete; single-callback unsubscribe at locator level; `RetenoActivityHelperImpl` start/resume dispatch and late-registration replay; channel attribute mapping; real system block-state broadcasts; notifications and channel both off; no state check while handling a push; receiver without extras doesn't forward; activity-level button click and host forward.
- Mutation-checked (each killed by the new tests): persist merged instead of full user, skip saving build number, swap `toTypeMap` entries, 23 h retention, facade drops the user, channel ignores `show_badge`.
- Gate: Core 2215/2215 lines, 494/494 branches (941 tests), Push 129, FCM 8 tests at 100 %, fixtures and `:app:compileDebugKotlin` green.
Production issues found by these tests:
- **Fixed — default notification channel parsing:** `NotificationChannelData` was parsed by Gson without a no-arg constructor, so its Kotlin defaults were ignored (a description without `importance` created the default channel blocked, without `lockscreen_visibility` private; a missing `id`/`name` put null into non-null fields and channel creation threw, leaking the half-set channel id). Parsing now goes through `NotificationChannelJson` (all fields nullable with defaults, so Gson calls its constructor): missing optional fields take `NotificationChannelData` defaults, a description without `id`, `name` or `description` is treated as invalid and falls back to the bundled description, then the fallback channel — `RetenoNotificationChannelTest`.
- **Fixed — market id message:** "max length must be less than 64" → "max length is 64" (64 characters are accepted). "latin characters" stays: the backend accepts only latin letters. **Open:** the rule uses `isLetterOrDigit()`, so the SDK accepts non-latin letters (e.g. Cyrillic) that the backend rejects — decide whether to tighten the rule to latin letters.
- **Later (decided):** `RetenoActivityHelperImpl` remembers the current activity only on resume, so a callback registered between an activity's start and resume (e.g. `FragmentScreenWatcher`) never gets `start` for it.
Open decisions (same behaviour as App Inbox, Phase 6 — decide for both together):
- **Init wait without timeout:** a fetch made before init (or when the host never initializes) keeps its callback, and whatever it captures, until init. Options: keep; or answer `onFailure(null, null, error)` after a bounded wait.
- **Host callback exceptions:** an exception thrown by the host's callback is caught by `ActionRunner` and uploaded as an SDK failure report (stack trace through `Redact.uploadableStackTrace`); before, it crashed the host on the main thread. Options: keep; or rethrow host callback exceptions on main and not report them.

Final Coverage Report (Phase 7, after review fixes) — Kover: Core gated packages 2085/2085 lines, 510/510 branches (870 Core tests); Push 102 and FCM 7 tests at 100 %; fixtures self-tests and `:app:compileDebugKotlin` green.
- [x] Typed fetch: POST body/variant, typed result on main, unreadable → raw JSON fallback on main without report, server failure on main without report, waits for init, cancelled before init → no request/callback, trace without URL/response content — `FetchRecommendationActionTest`
- [x] JSON fetch: request, raw body on main, server failure on main without report, waits for init, trace — `FetchRecommendationJsonActionTest`
- [x] Events: mapped events saved on IO, empty and null lists skipped, insert failure → one report at the save step, trace args — `LogRecommendationEventsActionTest`
- [x] Variant id encoded in the request path — `FetchRecommendationActionTest`; read fallback keeps cancellation — `StepOrNullTest`
- [x] Facade mapping, request serialized at call time, unsupported OS — `RecommendationImplTest`; locator ports (typed, JSON, events, events saved in call order) — `ActionsLocatorRecommendationWiringTest`
- Gaps: the serial event queue is checked by call order only — the single-threaded test dispatcher can't show the race it prevents; `RetenoInternalImpl.recommendation` not tested directly (same reason as `appInbox`: the Robolectric facade test must not load a gated class); `PushRecomEventsStep` still sends the whole table in one request (`getRecomEvents(limit)` limits events *per variant*, so a real batch needs a draining loop; the backlog stays bounded by `ClearOldData`'s time-based cleanup); not smoke-tested on a device.
Behaviour changes to announce: recommendation fetches wait for SDK initialization; internal `ServiceLocator.recommendationProvider` removed.

### Phase 8 — In-app messages ☑ (largest; split in sub-PRs; 8a ☑, 8b ☑, 8c ☑, 8d ☑)
8a State & rules ☑ (`internal/iam/`, gated):
- `InAppDisplayRules` / `DisplayRulesEvaluator`: one pure check per message — content → segment → frequency → schedule — returning a `DisplayDecision` (`SHOW`, `NO_CONTENT`, `NOT_IN_SEGMENT`, `FREQUENCY_LIMIT_REACHED`, `OUTSIDE_SCHEDULE`; a `TracedOutcome` for the 8c traces), plus `matchesEvent(rule, event)` (name + parameters, AND/OR, all operators). Time comes from the injected SDK clock instead of `System.currentTimeMillis()`/`ZonedDateTime.now()`.
- `InAppMessagesState` / `InAppMessagesStateImpl`: synchronized holder for pause flag, pause behaviour, postponed messages and in-apps waiting for an event (fixes M8 for that state). `postponeIfPaused` is atomic (the check-then-add race between pause and show is gone); `setPaused(false)` hands back the message to show (POSTPONE: first postponed, SKIP: none) and forgets the rest. Messages/scheduled lists stay where they are until 8b/8c need them.
- `IamControllerImpl` takes both through their interfaces (owned by `ActionsLocator`, passed by `IamControllerProvider`), so its Robolectric test uses interface mocks and never loads the gated classes.
- Decided behaviour changes: "specific days and time" schedule rules are evaluated in the rule's `timeZone` (it was ignored; device time was used), an unknown zone falls back to the device zone; an invalid `REG_EX` pattern is no match plus one WARN line naming only the parameter (it threw inside the event collector and the event was lost); the waiting-for-event list is replaced on every fetch, also with an empty list (removed campaigns kept firing on events).
- Kept for 8c (D10/M6): `TimesPerTimeUnit` still compares the lifetime `showCount`, so after `count` shows the message is never shown again; `NoLimit` still means once per session on app start.
- Deleted: `FrequencyRuleValidator`, `ScheduleRuleValidator`, `RuleEventValidator` (technically public — CHANGELOG) and their 46 legacy tests, migrated into `DisplayRules{Evaluator,Frequency,Schedule,Event}Test`; `IamControllerImpl.inAppsWaitingForEvent` (`@VisibleForTesting`). In `IamControllerImplTest` the 7 rule/pause/event tests became 9 wiring tests against interface mocks; the pause behaviour itself is in `InAppMessagesStateTest`.

Final Coverage Report (8a) — Kover: Core gated packages 2317/2317 lines, 605/605 branches (995 Core tests); Push 134, FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green.
- [x] Decision order and content/segment cases — `DisplayRulesEvaluatorTest`
- [x] Every frequency rule incl. boundaries (`==` interval/time unit blocks, last show at session start blocks, count without last show) and all-rules-must-match — `DisplayRulesFrequencyTest`
- [x] Show/hide after, every rule must match, days listed/not listed/none, all-day, start/end minute edges, from midnight, overnight both halves and outside, rule time zone for hours and day of week, unknown zone → device zone — `DisplayRulesScheduleTest`
- [x] Event name, no/empty parameters, every operator incl. missing parameter, no parameters at all, case rules, full vs partial regex, invalid pattern (no match, other values still checked, WARN without pattern/value), AND/OR — `DisplayRulesEventTest`
- [x] Pause/postpone/skip/resume, nothing kept from before the pause, waiting list replace/clear — `InAppMessagesStateTest`
- [x] Controller wiring (most recent allowed, rejected skipped, postpone through state, resume shows handed-back message, behaviour passed on, waiting list replaced incl. empty, event match/no match) — `IamControllerImplTest`; SDK clock and shared state — `ActionsLocatorIamWiringTest`
- Mutation-checked: device zone instead of rule zone, invalid regex rethrown, postponing while not paused (this one survived first and got its own test).
- Gaps: not smoke-tested on a device; the rest of the controller (fetch, segmentation, triggering, html) is 8b–8d.
8b Fetch ☑ (`internal/action/iam/`, gated):
- `FetchInAppMessagesAction`: cached list from the local database (IO) → `If-None-Match` with the stored list version only when the cache is not empty (**D9 decided**; M1 fixed — the ETag was never sent) → server list. Success: parse inside the action (M2: a parse error can no longer hang the fetch), restore show history from the cache by `messageId`, load missing contents (3 retries, failure leaves them empty), check segments, replace the cache and save the new version. 304 **and any other failure or unreadable list (decided)**: use the cached messages — offline users keep their in-apps and the event list is not emptied — load their missing contents and check + save their segments. The result becomes the current messages in `InAppMessagesState`.
- `CheckInAppSegmentsAction` (`CheckInAppSegments`): one request for the distinct segments, applies the answer (`checkResult` null → not in segment, retry advice, check time from the SDK clock), optionally saves the checked messages; a failed or unreadable answer changes nothing. Used by fetch, by the controller before showing event/timer in-apps, and by `SegmentationRefreshImpl`, which now re-checks the current messages in place and saves them (**M4 fixed**: it re-downloaded the whole list and discarded the results).
- `PreloadIamHtmlAction` + `InAppHtmlPreload` (fire-and-forget from app foreground, as before): move html kept in settings to the file, version via HEAD on the configured or default address (failure or no header → default version), download only when versions differ; **a failed download keeps the old version so the next preload retries (decided)** — it used to record the new version with the old content and never retry.
- `StartSdkAction` fetches, then hands the messages to `IamController.processInAppMessages` (trigger sorting stays in the controller until 8c); `AppForegroundedAction` requests the preload.
- Deleted: `IamController.getInAppMessages/refreshSegmentation/preloadHtml`, `IamRepository.getBaseHtml/getInAppMessages/getInAppMessagesContent/saveInAppMessages/checkUserInSegments` and the repository's `SharedPrefsManager` dependency; 11 legacy repository tests and 1 legacy controller test (migrated to the action tests). `ServiceLocator`'s in-app database and file manager providers became `internal` for `ActionsLocator`.
- Known, not changed (frozen local mapper): `InAppMessage.updateFromDb` restores segment retry advice only if the fresh message already has one, so retry advice does not survive a refetch; `SegmentRule.shouldCheckStatus` is still unused (segments are checked whenever an event/timer in-app is about to show).

Final Coverage Report (8b) — Kover: Core gated packages 2507/2507 lines, 667/667 branches (1041 Core tests); Push 134, FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green.
- [x] Cache read on IO, full request without cache, version header with cache, server list, show history restored, missing contents (request body, 3 retries, attached, failed/unreadable → none, none requested when all have content), segments checked without saving, cache replaced on IO, version saved; 304 / 503 / offline / unreadable → cached, cached contents loaded, cached segments checked and saved, cache and version untouched; nothing cached + failure → empty; current messages kept; notes; no version/address in trace — `FetchInAppMessagesActionTest`
- [x] No request without segments (incl. async rules without segment), one request per distinct segment, in/out/null answer, check time, retry advice, unanswered messages untouched, save on IO only when asked and something was checked, failed/unreadable answer → untouched and not reported — `CheckInAppSegmentsActionTest`
- [x] Migration from settings, settings read on IO, default/configured address, up to date (same version, both unknown, missing/empty header), download from the same address, file before version, failed download keeps version and succeeds, nothing traced from address/content — `PreloadIamHtmlActionTest`; background launch — `InAppHtmlPreloadTest`
- [x] Debounced refresh checks and saves current messages, keeps working after a failed check — `SegmentationRefreshTest`; fetch → schedule order and empty list on fetch failure — `StartSdkActionTest`; preload requested on foreground — `AppForegroundedActionTest`; controller checks and saves segments before event in-apps, not on app start — `IamControllerImplTest`; real wiring (fetch on start, refresh after user update, preload on foreground) — `ActionsLocator*WiringTest`
- Mutation-checked: version sent without cache, failed fetch shows nothing, version saved on failed download (survived first — the test didn't assert success — now killed), unanswered segment counted as in segment, fetched messages not kept for refresh.
- Gaps: not smoke-tested on a device; backend 304 support for the list endpoint was not verified live.

Review follow-up (`/code-review` of 8b), mutation-checked:
- `StartSdkAction` runs the fetch on IO again (it ran on the main scope, so list/content/segment parsing hit the main thread at start).
- All in-app database writes of the new actions go through `InAppMessagesCache` (one `Mutex` around delete+insert; the table has no unique key, so concurrent upserts could duplicate rows). The debounced segmentation refresh no longer saves — every start re-checks segments anyway — so it can't write stale messages back over a newer list.
- The cached path (304/failure) saves the refreshed messages, so contents loaded for cached messages are kept instead of being downloaded again on every start.
- Contents and segment checks run in parallel again, as before the migration.
- Base html: a failed version check (HEAD) keeps the current html (it used to download again and overwrite a real version with the default one); a failed file write keeps the old version so the next preload retries. The version header constant is shared with `RestClientImpl`; `?.let ?:` replaced by explicit `if`.
- For 8c: segment fields of the shared `InAppMessage` objects are written by segment checks while display decisions read them on other threads (existed before) — 8c serializes triggering; a refresh debounced shortly before `stop()` can still send one segment request.
8c Triggering ☑ (`internal/action/iam/`):
- `ProcessInAppMessagesAction` (from `StartSdkAction`): sorts messages into app start / event / time spent, schedules the time-spent ones in the session tracker, replaces the in-apps waiting for events, shows the app-start candidate, and always opens the event gate — also when processing fails (**M3**).
- `ShowInAppAction`: the one path from candidates to the display, serialized by a lock shared with the segmentation refresh (the 8b review's unsynchronized segment fields): checks and saves segments for event/timer candidates, decides from the highest message id down (each decision traced), postpones while paused, otherwise hands the message to `InAppDisplay`.
- `InAppTriggers`: subscribes to the event bus at SDK boot (`ActionsLocator.boot`), holds events until the gate opens, shows the waiting in-apps an event matches, and runs time-spent in-apps the session tracker reports due.
- `PauseInAppMessagesAction` / `SetInAppPauseBehaviourAction`: facade (`pauseInAppMessages`, `setInAppMessagesPauseBehaviour`) and `ApplyConfigAction` go through them; resuming shows the postponed message the state hands back.
- `RecordInAppShownAction` (**D10 decided**): the show is counted when the view reports the widget displayed (`IamViewImpl` → `InteractionSubmitter.submitInAppShown`, same moment as `onDisplay` and the opened interaction), no longer when the SDK decides to show it (**M6**: a failed load or an app sent to background counted as a show).
- **"N times per time unit" decided**: up to N shows; at the limit the in-app can show again once a full unit has passed since the last show, and the count restarts at 1 (`DisplayRulesEvaluator` + `RecordInAppShownAction`). It used to mean "N shows, a unit between each, then never again".
- **Segment checks decided: kept on every event/timer show** (`SegmentRule.shouldCheckStatus` stays unused, so segment retry advice not surviving a refetch has no effect).
- `IamController` keeps only html loading and the `InAppDisplay` port (`show` — dropped while another in-app is loading); `IamRepository.updateInAppMessages`, the controller's event collector and the `bufferUntil` util (technically public — CHANGELOG) are gone.
- Moved to 8d with the view: **M5** (`pausePushInAppMessages` also blocks display-rule in-apps: `checkPauseState` stores `lastPushInteractionId = null` and the html state stays `Success`), and testing that the view reports a show only on display (`IamViewImplTest` is still the vacuous legacy test).

Final Coverage Report (8c) — Kover: Core gated packages 2694/2694 lines, 705/705 branches (1079 Core tests); Push 134, FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green.
- [x] Highest id first, rejected skipped, none passing, no candidates, decision inputs, segments checked and saved for triggered / not on app start, postpone while paused, one at a time, decisions traced — `ShowInAppActionTest`
- [x] App start shown, time spent scheduled and shown when due, nothing scheduled without timers, event in-apps waiting, mixed targeting, gate opened after app start and on failure, sort traced — `ProcessInAppMessagesActionTest`
- [x] Waiting in-apps shown on matching event after gate, events held until gate, unmatched ignored, listening once, due in-apps shown — `InAppTriggersTest`
- [x] Pause, resume shows postponed / nothing, skip behaviour, traces — `PauseInAppMessagesActionTest`; first show, increment without/other rules, within unit, restart after unit, never shown, save on IO, trace — `RecordInAppShownActionTest`; new times-per-unit rule incl. boundaries — `DisplayRulesFrequencyTest`
- [x] Controller show/drop while loading — `IamControllerImplTest`; facade pause/behaviour after init and unsupported OS — `RetenoInternalImplTest`; locator: pause, skip behaviour, show saved, logged event shows waiting in-app end to end — `ActionsLocatorIamWiringTest`; start/apply config callers — `StartSdkActionTest`, `ApplyConfigActionTest`, `ActionsLocatorInitWiringTest`
- Mutation-checked: times-per-unit limit as `<=`, count restart at exactly one unit, pause ignored, gate not opened on failure, events not held, triggering not serialized.
- Gaps: the view's "record show on display" is untested (8d); not smoke-tested on a device.

Review follow-up (`/code-review` of 8c):
- Host `pauseInAppMessages` / `setInAppMessagesPauseBehaviour` go through a `SerialQueue` (`InApp`), so they apply in call order (separate launches on the default dispatcher could reorder pause/resume).
- The in-app trigger lock covers only in-memory work and its save: segment results are applied (and saved) under it, show counts are recorded under it, decisions + postpone/display run under it; the segment request itself runs before the lock, so a slow segment check no longer holds up the app-start in-app or `StartSdk`.
- With several "N per time unit" rules the count restarts only after the longest unit has passed (with the first rule only, `2 per hour + 5 per day` never reached its daily limit).
- Event trigger loop survives a failing event (logged under `Reteno`), stops on cancellation, and keeps at most 50 events while the gate is closed (oldest dropped) — background-only processes no longer buffer forever and old events are not replayed hours later.
- The event gate opens right after the waiting list is replaced (as before the migration) and on failure, not on cancellation — a cancelled start keeps buffered events for the next start.
- Postpone and gate opening are named steps; step names say where they act ("… in in-app state", "count show in in-app message"); a nested action is never wrapped in a step and conditional nested calls are braced (`AGENTS.md`).
- `boot()` only subscribes to events: `InAppTriggers` takes `ShowInApp` as `Lazy`, so the display graph (controller, repository, REST client) is built on first use instead of on the main thread in `Application.onCreate`.
- Mutation-checked: restart by first unit, gate opened on cancellation, unbounded buffer, segment check under the lock. Not testable on the single-threaded test dispatcher: call order through the `InApp` queue and the lock around segment/show-count writes.
8d View ☑ (`internal/action/iam/`, `internal/iam/`, `view/iam/`):
- `LoadInAppHtmlAction`: base html + message model; personalisation placeholders filled from the newest pending user, falling back per field to the synced one, each value **JSON-string-escaped** (**SEC5** fixed: `"`, `</script>`, unicode stay literal text); users read only when the model has placeholders, on IO.
- `LoadPushInAppHtmlAction`: widget by interaction id and base html in parallel, 30 s timeout (fails the action), bundled widget (`AppPlatform.bundledWidget()`) when the server answer is missing or unreadable.
- `ReportWidgetFailureAction`: sends widget init/runtime failures to the server (tenant = opened interaction id, else instance id for display-rule in-apps; interaction id for push in-apps — as before).
- `InAppPresenter` (implements `IamView`, `InAppDisplay`, `InAppScreenEvents`; main-confined): one in-app at a time (display rules dropped while one is on screen, a push in-app replaces it); a display-rule in-app offered while the app is in background waits for `start()`; `beforeDisplay` → load → `InAppScreen.create`, a failed load/create frees the screen; on widget display it sizes the screen, calls `onDisplay`, counts the show (D10) and records the opened interaction; readiness wait is a cancellable job (150 × 200 ms, **M7**), cancelled by `pause()`/teardown, giving up frees the screen; clicks record the click with the opened interaction id, send custom data or open the link, then free the screen; close button / dismiss call `beforeClose`/`afterClose`; failures call `onError`, report the failure and record a failed interaction. All host callbacks run on main.
- **M5 fixed**: `pausePushInAppMessages` holds only push in-apps; display-rule in-apps keep showing. A held push in-app is revealed on resume; with `SKIP_IN_APPS` it is dropped (when held, when skip is chosen while held, or when displayed while paused); a held in-app replaced by a newer push in-app is not revealed later.
- `AndroidInAppScreen` (`view/iam`, via `AppPlatform.inAppScreen()`): the WebView containers, JS bridge, activity start/stop of the current activity, `ACTION_VIEW` links (any failure to open is logged, not thrown), custom data through `AppBroadcaster.sendToApp` restricted to the app's own package (**SEC9**).
- Wiring: `ActionsLocator.inAppPresenter` replaces the view and controller everywhere (`ShowInApp`, `PauseInAppMessages`, `ApplyConfig`, `StartSdk`, `StopSdk`, `InitializeIamFromPush`); `ServiceLocator.iamViewProvider` (public) returns it.
- Deleted: `IamViewImpl`, `IamController(Impl)`, `IamRepository(Impl)`, `ApiClient(Impl)` (last user gone), their providers and `ServiceLocator.iamControllerProvider`; legacy `IamViewImplTest`, `IamControllerImplTest`, `IamRepositoryImplTest`, `ApiClientImplTest`. All internal — nothing for the CHANGELOG.
- Behaviour changes to announce: push pause no longer blocks display-rule in-apps (M5); in-app custom data reaches only receivers in the app's own package (SEC9); a link opened without a current activity starts from the application context (was dropped); an in-app whose activity wasn't ready when the app went to background is presented on the next activity start (used to stay stuck).
- Test fixture: `MarkingDispatcher` implements `Delay`, so `delay` on `testDispatchers().main` runs on virtual time (it used to fall back to a real-time delay).

Final Coverage Report (8d) — Kover: Core gated packages 3030/3030 lines, 845/845 branches (1182 Core tests); Push 134, FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green.
- [x] Message model in base html, layout kept / default top, new id, no content fails, users not read without placeholders / read on IO, newest pending wins, per-field fallback to synced, empty without users, all 8 placeholders, missing address, injection payloads (`"`, `</script>`, unicode) literal, user values not traced — `LoadInAppHtmlActionTest`
- [x] Widget from server, bundled widget on failure / unreadable answer / missing bundled file, layout defaults, timeout fails, base html in parallel — `LoadPushInAppHtmlActionTest`; failure payload and tenant — `ReportWidgetFailureActionTest`
- [x] Held before start, `beforeDisplay` on main before loading, create from html, second in-app ignored, view api entry, load / create failure frees screen, present on widget display, `onDisplay` on main, show counted on display only (D10), opened interaction id, resize / no digits / no height, readiness wait / give-up / pause cancels, hide on stop, re-present on start / not before display / nothing on screen, click recording (with / before display), link open / blank / missing, custom data with / without link / empty / undelivered, close button and dismiss callbacks, widget failure report + failed interaction + `onError` on main, runtime error under opened id, unreadable / unknown / null widget messages, messages with nothing on screen, push load + announce, push replaces, push not counted, M5 matrix (hold, resume, skip while held, skip at display, postpone, skip when paused, load while postponed, display rules unaffected, stale held), push ids in clicks / custom data / failure report, push load failure, removed callback — `InAppPresenterTest` (59)
- [x] Container create / failure, bridge and dismiss forwarded, start / stop of current activity only, ready checks, present with / without activity / container / failing show, resize / hide / destroy, link from activity / application / no handler / refused, custom data to app receivers / none — `AndroidInAppScreenTest` (Robolectric, ungated `view/iam`); `bundledWidget`, `inAppScreen` — `AndroidAppPlatformTest`; package-scoped data broadcast — `AppBroadcasterTest`
- [x] Locator: in-app offered before start shown on start, held after stop, push pause from user config holds push in-app, push in-app from push module loads widget, skip behaviour drops postponed, waiting in-app shown on event (host sees `beforeDisplay`), show and opened interaction saved on widget display — `ActionsLocator{Init,Facade,Iam}WiringTest`
- Mutation-checked: M5 applied to display rules, held kept after teardown, pause not cancelling readiness, tenant ignoring opened id, `shown` ignoring present result, teardown keeping the screen, inactive presenter showing, skip behaviour holding, start presenting unrevealed in-app, second in-app replacing current.
- Gaps: not smoke-tested on a device (WebView, containers, JS bridge are exercised only through mocks).

Review follow-up (`/code-review` of 8d) — Kover: Core gated packages 3065/3065 lines, 857/857 branches (1195 Core tests); Push 134, FCM 8 at 100 %:
- Widget messages are parsed into `features.iam.IamJsRawEvent` (`@SerializedName` fields, kept by the consumer R8 rules); the private parse class in `internal.iam` would have been renamed in minified host apps and every widget message ignored.
- Host lifecycle callbacks are guarded: an exception is reported through `RemoteLog` (callback name and exception class only) and the in-app flow continues, so a throwing `beforeDisplay`/`beforeClose` no longer leaves the presenter stuck with an in-app "on screen".
- Each container gets its own JS bridge tagged with its html id; the presenter ignores widget messages and dismissals from an in-app that is no longer current (a replaced WebView can still post until it is destroyed).
- Readiness timeout: an in-app that was already presented stays for the next activity start (as before the migration); one never presented is closed with `beforeClose`/`afterClose` (`DISMISSED`) instead of being destroyed silently.
- A widget that finishes loading while the app is in background waits for `start()` before it is revealed, so no `onDisplay`, show count or opened interaction is recorded for an in-app nobody saw.
- `LoadInAppHtmlAction` and `LoadPushInAppHtmlAction` build the html on IO (the bundled widget read, parsing and string replacement ran on main through the presenter).
- `LoadPushInAppHtmlAction` and `ReportWidgetFailureAction` wait for SDK initialization in a named step; "read bundled widget from app resources".
- The loading job starts lazily after it is stored, so a load failing without suspending can't hit an unassigned job.
- `StartSdkAction`, `StopSdkAction`, `ApplyConfigAction`, `InitializeIamFromPushAction` call the view directly (the presenter moves onto main itself; the extra `withContext(main)` is gone).
- Mutation-checked: stale messages accepted, unguarded host callback, timeout closing a presented in-app / closing without callbacks, reveal in background, waiting in-app kept after teardown / after start, init wait skipped (both actions), html built on main (both actions).

### Phase 9 — Logger & observability ☑
`Logger` becomes a thin façade over an injected `RetenoLogSink`; persistent log events only through `LogRetenoEventAction`, recursion impossible by construction. (SEC2 PII stripping moves earlier, into Phase 2, alongside `PushLogEventsStep`.)
- `Logger` (public, signatures unchanged) writes `v/d/i/w/e` to the installed `RetenoLogSink` (same `isLoggingEnabled()` gate, message built only when enabled, `e` adds the stack trace) and passes `captureMessage` / `captureEvent` / `e` to the installed `RemoteLog`; nothing is recorded before the SDK installs one. It no longer reaches `RetenoInternalImpl.instance`.
- `RemoteLogRecorder` (`internal.log`, the one `RemoteLog` the SDK installs): redacts (`Redact.uploadable`, `uploadableStackTrace` for errors), fills package, device id (`uninitialized` when not available) and app/OS/device description from `AppPlatform.logEventOrigin()` (read once), and queues the event through `LogEventQueue` (`SdkActions.submitRetenoLogEvent` → `LogRetenoEventAction`). `ServiceLocator.actions` builds it with the platform and installs it with the shared sink once `ActionsLocator` exists (the queue is `Lazy`).
- Recursion: `RemoteLogGuard` (per-thread) drops anything recorded while the recorder itself is queueing and while the log-event pipeline touches its tables (`LogRetenoEventAction` save, `PushLogEventsStep` count/read/delete), because the frozen DB layer logs its own errors through `Logger.e`. Failures of those two runners were already never reported.
- `RemoteLog` gains `record(message, level)` and `recordError(message, error)`; `info`/`error` are defaults. `LoggerRemoteLog` (still used by `AppInboxImpl`/`RecommendationImpl`) forwards to `Logger`. `LogPriority.VERBOSE` added for `Logger.v`.
- Deleted: `RetenoInternalFacade.logRetenoEvent` and `RetenoInternalImpl.logRetenoEvent` (technically public — CHANGELOG), Robolectric `LoggerUploadSanitizingTest` / `LoggerWithoutSdkInstanceTest` (moved to `RemoteLogRecorderTest` / `LoggerTest`).
- Behaviour changes to announce: log events recorded before the host calls `Reteno.initWith` are saved right away (the facade used to hold them until init; they still carry `uninitialized` as device id); `Logger.captureEvent` keeps only message and level of the given event (package, device and versions were already overwritten before).

Final Coverage Report (9) — Kover: Core gated packages 3107/3107 lines, 861/861 branches (1216 Core tests); Push 134, FCM 8 at 100 %; fixtures and `:app:compileDebugKotlin` green.
- [x] Message, level, package, device id queued; info/error levels; interpolated values dropped; error message + stack trace redacted (no email, no host); `uninitialized` device id; muted drops; no re-entry from inside queueing; queue not resolved early; app/device description read once — `RemoteLogRecorderTest`
- [x] Block runs when not muted, skipped while muted, outer mute kept after inner block, unmuted after failure, result returned — `RemoteLogGuardTest`
- [x] Priorities and tag, no message built when off, error stack trace, error arguments local only, error recorded remotely, captured message / event with level, nothing remote before install — `LoggerTest`; `LoggerRemoteLogTest`; `VERBOSE` mapping — `AndroidLogSinkTest`; origin from package manager and build — `AndroidAppPlatformTest`
- [x] Failing save that logs creates no new event — `LogRetenoEventActionTest`; failing count/read/delete that log create no new events — `PushLogEventsStepTest`
- Mutation-checked: guard ignored by recorder, inner block unmuting outer, guard never unmuting, origin read per event, save not muted, upload DB not muted, `Logger.e` not recorded remotely.
- Gaps: legacy call sites still use `Logger` directly (removed with their classes in Phase 10).

Review follow-up (`/code-review` of 8d + 9) — Kover: Core gated packages 3134/3134 lines, 871/871 branches (1232 Core tests); Push 134, FCM 8 at 100 %:
- `ActionsLocator` builds the `RemoteLogRecorder` itself (no `remoteLog` constructor parameter; package name comes from `LogEventOrigin`) and `installLogging()` installs it into `Logger`; `ServiceLocator.actions` only calls that. Locator tests read failure reports from the saved log events (`LocatorFixture.remoteLogs`), and `installLogging` has its own test.
- `Logger` keeps up to 50 records made before the SDK installs its remote log and hands them over on install (oldest dropped); the old facade path held them until init.
- `Logger.captureEvent` keeps the caller's event (wrapper SDKs set their own `sdkVersion`, `platformName`, `version`, `device`): only the message is redacted, package and device id replaced (`RemoteLog.recordEvent`).
- `LoadInAppHtmlAction` shows content with a layout type this SDK doesn't know full screen and puts `{}` for a missing model (Gson leaves those declared non-null fields null); the old controller did the same.
- `InAppPresenter`: a failed `present()` counts as "not ready yet" (retried, closed with `DISMISSED` if it never succeeds) instead of stopping the wait with the in-app stuck as current; a display-rule in-app offered in background is no longer lost when a push in-app takes the screen on start — it is offered once the screen is free (not when a push in-app replaces another).
- Push in-app pause, pause behaviour and lifecycle callback go through `SdkActions.submitPushInAppPause` / `submitPushInAppPauseBehaviour` / `submitInAppLifecycleCallback` (skipped on unsupported OS like every facade call); `IamViewProvider` and the technically public `ServiceLocator.iamViewProvider` are deleted (CHANGELOG).
- `LoadPushInAppHtmlAction` drops the 30 s timeout: it could not interrupt the blocking HTTP call, which `RestClientImpl` already bounds; the server-answer check is a braced block; `ActionsLocator` shares one id generator and one file manager.
- Mutation-checked: failed present ending the wait, pending offered over a current in-app / in background / never after close / on replacement, captured event not redacted, buffer not flushed / unbounded, unknown layout failing.

### Phase 10 — Removal & whole-module gate ◐ (split: 10a ☑, 10b ☑, 10c ☑, 10d ☐, 10e ☐)
Baseline before 10 (whole Core module, only `R`/`BuildConfig` excluded): 6130/8686 lines, 1497/2642 branches — gaps mostly in the WebView slide-up container, the DB layer and `DbUtil`, shared prefs, `util`, `permission`, `data/local/config`.
- 10a Sample app against the public API only (it reads `RetenoInternalImpl.serviceLocator` by reflection and writes SDK tables through internal DB managers; must go before 10b).
  - ☑ Done. Deleted the database read/write screens (12 fragments, their layouts, `nav_graph_database`), `RetenoInitListener` and the session-time display (both read SDK internals), and the in-app base URL field (it wrote into the SDK's private shared prefs). The device-id screen keeps only the sample's own `DeviceIdProvider` settings (custom id, delay, external id). Activities use `Reteno.instance`; logging and `toStringVerbose` are the sample's own (`SampleLog`); the sample's device-id-mode preference is gone. Remaining `com.reteno.core` imports are public API types (models in public signatures, `Procedure` in push listeners). Session duration is saved for the next launch's `RetenoConfig` instead of changed live. `:app:assembleDebug` green.
  - The Sentry screen is now "Error reporting": "Crash app" throws in the app; "Crash sdk" logs a test error through `Logger.e` (stored and uploaded as an SDK error log event) via the debug-only `_interop.TestErrorInternal` (`RetenoSdkCore/src/debug`, not in the release AAR), reached from the sample's `src/debugHooks` (debug + qa); the sample's release build has a no-op bridge and disables the button. Replaces reflection on the private `testCrash()`.
  - Now unused outside tests, removed in 10b: `RetenoInternalImpl.isInitializedByUser` (technically public — CHANGELOG) and the private `testCrash`.
- 10b Composition root (D12): one internal component owns the shared infrastructure as plain singletons; delete `ServiceLocator`, `di/provider/*`, `di/base/*`, public `RetenoInternalImpl.serviceLocator`; facade gets collaborators through its internal constructor.
  - ☑ Done. `di/SdkComponent` is the composition root: plain `by lazy` singletons for prefs, `DeviceIdHelper`, `RestConfig`, `RestClient`, `ConfigRepository`, `FileManager`, activity helper, the database and every manager, the session tracker, `InitGate`, `LifecycleEventFilter`, `SdkConfigurationImpl`, and `ActionsLocator` (which installs logging). Fixes **I5**: weak-reference providers could hand out a second `RestConfig` / database manager after GC; now one instance each (`SdkComponentTest`). `ActionsLocator` takes `SdkInfrastructure` (tests mock the interface in `LocatorFixture`); the facade's internal constructor takes `FacadeInfrastructure` and asks `SdkActions.isInitialized` / `awaitInit()` instead of reading the gated `InitGate`, so `RetenoInternalImplTest` / `RetenoInternalImplUserValidationTest` run on `FakeFacadeInfrastructure` without loading gated classes. `SdkConfigurationImpl` holds the config itself (`RetenoConfigProvider` gone; `DeviceIdHelper` / `RestClientImpl` read `configuration.config`). `AndroidAppPlatform` takes `Lazy<WorkManager>`; WorkManager get-or-initialize moved into the component.
  - Deleted: `ServiceLocator`, `di/provider/*` (22 providers incl. the public `RetenoConfigProvider`), `di/base/*` (public `Provider`, `ProviderNewInstance`, `ProviderStrongReference`, `ProviderWeakReference`), `RetenoInternalImpl.serviceLocator`, `RetenoInternalImpl.isInitializedByUser`, the private `testCrash()`; `BaseRobolectricTest` / `DbUtilDeviceTest` no longer stub the `ServiceLocator` constructor. CHANGELOG (technically public): `ServiceLocator`, `RetenoInternalImpl.serviceLocator`, `RetenoInternalImpl.isInitializedByUser`, `RetenoConfigProvider`, `com.reteno.core.di.base.*`.
  - Final Coverage Report (10b) — Kover: Core gated packages 3134/3134 lines, 871/871 branches (1235 Core tests); Push 134, FCM 8 at 100 %; `:app:compileDebugKotlin` green. New: locator init state and waiter release (`ActionsLocatorInitWiringTest`), one instance per collaborator (`SdkComponentTest`); config assertions read `SdkConfiguration.config` instead of verifying the deleted provider.
  - Gaps: `SdkComponent`'s `actions` / `sessionHandler` / WorkManager wiring is exercised only in production (building a real `ActionsLocator` under Robolectric breaks the gate); 10d decides how the whole-module gate covers it.
- 10c Legacy removal: `OperationQueue`, `RetenoThread*`, leftover controllers/repositories, singleton lookups (`DeviceInfo` → platform).
  - ☑ Done. Legacy threading gone: `OperationQueue` (public object — CHANGELOG), `RetenoThread`, `RetenoThreadFactory`, `AndroidRetenoDispatchers` (the component passes `retenoDispatchers(main, io)`); `RetenoTestApp` no longer mocks the queue.
  - No singleton lookups left inside SDK internals: `AndroidAppPlatform.createDevice` describes the device from its own `Context` (category via `TelephonyManager`, null service → `MOBILE`; app version via `AppInfo`; OS, model, language, time zone); `Device`'s companion (`createDevice`, `fetchDeviceCategory`/`fetchLanguageCode`/`fetchTimeZone`), `DeviceInfo.fetchAppVersion`, `isGooglePlayServicesAvailable` and the unused `DeviceDb.toDevice()` are deleted. `RetenoLogEvent.version` defaults to `null` and `RemoteLogRecorder.recordEvent` fills it from `LogEventOrigin`. Remaining lookups are the public entry points (`Reteno.instance`, `RetenoInitializer`, deprecated `BackwardCompatibilityDriver`) and the two system entry points (`PushDataWorker`, created by WorkManager; `RetenoInternalEntryPoint`, Push's way in).
  - Dead code deleted (found by an unreferenced-declaration scan): `SqlStateEncrypt`, `FrequencyRuleType`, `InAppMessagesList`, `EventWaitingQueue`, `VerticalSwipeDismissBehavior` (the slide-up container uses `SwipeDismissBehavior`), `ResultDomain`, `Util.fromRemote`, `isNonRepeatableError`, `toJsonOrNull`, `listFromJsonOrNull`, `SchedulerUtils.getOutdatedTime` (+ its 2 legacy tests), `RetenoInternalImpl.swapInstance`.
  - Behaviour change: a stored device row with no category reads as `MOBILE` (it asked the singleton for the live category; current code always writes one) — decided to keep, CHANGELOG.
  - CHANGELOG (technically public): `OperationQueue`, `Device.Companion` (`createDevice`), `SqlStateEncrypt`, `FrequencyRuleType`, `InAppMessagesList`, `EventWaitingQueue`, `VerticalSwipeDismissBehavior`, `Util.fromRemote`, `isNonRepeatableError`, `toJsonOrNull`, `SchedulerUtils.getOutdatedTime`, `RetenoInternalImpl.swapInstance`; `RetenoLogEvent.version` default is now `null`.
  - Final Coverage Report (10c) — Kover: Core gated packages 3146/3146 lines, 879/879 branches (1237 Core tests); Push 134, FCM 8 at 100 %; `:app:compileDebugKotlin` green. New: device id/push fields, app/OS/locale/time zone, mobile / tablet / no telephony service (`AndroidAppPlatformTest`), app version filled for captured events without one (`RemoteLogRecorderTest`).
  - Review follow-up (`/code-review` of 10a–10c) — Kover: Core gated packages 3152/3152 lines, 883/883 branches (1244 Core tests); Push 134, FCM 8 at 100 %; `:RetenoSdkCore:compileReleaseUnitTestKotlin`, `:app:compileDebugKotlin`, `:app:compileReleaseKotlin` green.
    - Unsupported OS no longer builds the SDK: `runAfterInit` returns at once, `isInitialized` / `isStarted` answer `false` without touching `actions`, `AppInboxImpl` / `RecommendationImpl` take `Lazy` actions (10b had made every deferred call build `ActionsLocator` and install remote logging there). Mutation-checked.
    - `TestErrorInternalTest` moved to `src/testDebug` (the debug-only hook doesn't exist in the release unit-test variant).
    - An unreadable package version is `null` again for the device and log events (`AppInfo.installedVersionName()`), not ` (0)`.
    - `SessionTracker` runs on the component's `retenoScope` (injected dispatchers, uncaught errors logged under `Reteno`) instead of a hard-coded `Dispatchers.Default` scope; one log sink for the component.
    - `RetenoInternalImpl.instance` is back as a deprecated alias of `Reteno.instance` (the old sample app taught it); the debug-only `_interop.DeviceIdInternal` (no users after 10a) is deleted; facade reads component members directly (no second `lazy`); init test split in two; blank-line layout fixed in touched files.
  - Kept on purpose: `ConfigRepository(Impl)` (shared infrastructure the actions use for device id, FCM token, notification channel), `IamFetchResult` (public model), `RetenoApplication` / `BackwardCompatibilityDriver` (deprecated public API).
- 10d ☑ Tests: delete `BaseRobolectricTest` / `BaseUnitTest` / `RetenoTestApp`, rewrite DB manager, mapper, schema/DbUtil, validator tests in the new style, cover the rest; Kover rule flipped to the whole Core module at 100 %.
  - Target (decided 2026-10-15): **Robolectric only for thin Android adapters** (`internal/platform`, `view/iam` adapters, the frozen SQLite layer, Push notification adapters). Logic moves behind interfaces and is tested with plain JUnit 5; full Robolectric removal stays a later option (needs an emulator job with adapters outside Kover, or a DB rewrite once the toolchain freeze lifts).
  - Baseline after 10c (whole Core module, only `R`/`BuildConfig` excluded): 6079/8303 lines, 1493/2567 branches in the shared test JVM; 6127 / 1500 with one JVM per test class (969 s instead of ~90 s). Kover merges forks correctly; the classloader clash only hits classes used by both runners (`Util`, `Logger`, `RetenoActivityHelperImpl`).
  - 10d-1 Coverage infrastructure: run Robolectric tests in their own JVM (second test task) so coverage no longer depends on runner/test order; whole-module report alongside the gated one.
    - ☑ Done. Core's `testDebugUnitTest` runs the JUnit 5 engine only and is finalized by a new `testDebugRobolectricUnitTest` (JUnit vintage engine — every Robolectric/JUnit 4 test — in its own JVM, same classpath, Kover agent copied in so both append to `testDebugUnitTest.ic`); Kover report/verify tasks depend on both. 926 + 318 tests, ~75 s (unchanged). `-PkoverWholeModule` switches the debug report to the whole module for progress tracking. Experiments: one JVM per test class gives 6127 vs 6079 lines (Kover merges forks as a union) but takes 969 s; the two-JVM split gives 6079 — the remaining 48 lines are lost to legacy tests mocking concrete classes (`RetenoTestApp`: `mockkObject(Util)`, `mockkStatic(Logger)`), removed with those tests in 10d-2…10d-4. Gate unchanged: 3152/3152 lines, 883/883 branches; Push 134, FCM 8.
  - 10d-2 Models and mappers: plain JUnit 5 tests replacing the `BaseUnitTest` ones; `BaseUnitTest` deleted.
    - ☑ Done. Gated: `data.local.mappers`, `data.local.model`, `data.remote.mapper`, `data.remote.model`, `domain.model`, `features`, `util.InAppMessageUtil` — gate 4925/4925 lines, 1398/1398 branches (1047 JUnit 5 + 297 Robolectric tests); whole module 6482/8273 lines, 1781/2544 branches (from 6079 / 1493). Legacy `*KtTest` mapper tests (7 remote, 2 local incl. the 609-line Robolectric ecom test) replaced by new-style tests that check every mapped field; `EcomEventMapperTest` / `OrderTest` stay Robolectric because public `Order.attributes` uses `android.util.Pair`; the display-rules parser (`InAppMessageUtilTest`), frozen `InAppMessageMapper`, stored-value parsers and user diff/accumulate models got their first direct tests.
    - Fixed: accumulated user attributes without custom fields on either side became `fields = []`, so the next diff against the saved state saw a change and sent a user update with nothing in it (`UserAttributesRemote.createAccModel` keeps `null`). Response models Gson builds now have defaults (`InAppMessageListResponse.messages`, `InAppMessagesContentResponse.contents`, `AsyncRulesCheckResponse.checks`, `AsyncRulesCheckResult`, `InboxMessagesCountRemote`, `IamJsRawEvent`): a missing list reads as empty instead of a `null` hiding in a non-null type.
    - Removed: unreachable branches (`null ->` in `FrequencyRule` / `ScheduleRule.fromJson`, `eventName != null` in `TargetingRule`, the equality early returns in `UserRemote` / `UserAttributesRemote.createDiffModel`), dead `SegmentRule.shouldCheckStatus` / `AsyncRuleRetryParams.shouldRetry`, `InAppMessage.notifyShown`, `EventWaitingForInApp`, `DisplayRuleType` (only its inlined `.name` was used), the `RemoteConstants` object wrapper (now top-level constants). CHANGELOG (technically public): `DisplayRuleType`, `EventWaitingForInApp`, `InAppMessage.notifyShown`, `SegmentRule.shouldCheckStatus`, `AsyncRuleRetryParams.shouldRetry` / `STANDARD_RETRY_TIME`.
    - Build: the Robolectric task inherits `--tests` filters and filtered runs no longer fail when one engine has no match. `BaseUnitTest` is deleted in 10d-3 with its last users (`RestClientImplTest`, `ConfigRepositoryImplTest`).
    - Mutation-checked: phantom custom fields, list order counted as a change, frequency `enabled` ignored, list default removed (the `unreadCount` default and `UserRemote`'s equality check turned out equivalent; the check was removed).
  - 10d-3 Config, prefs, network, util: `DeviceIdHelper`, `RestConfig`, `ConfigRepositoryImpl`, `RestClientImpl`, `Util` / `BitmapUtil` / `InAppMessageUtil` / `BuildUtil` / `Logger` — logic on JUnit 5 behind interfaces, `SharedPrefsManager` / `FileManager` as thin adapters.
    - ☑ Done. Gated packages added: `data.local.config`, `data.local.file`, `data.local.sharedpref`, `data.remote.api`, `data.repository`, `domain` (whole package), `util` (whole package, replaces the `InAppMessageUtil*` class filter). `BaseUnitTest` deleted with its last users (`RestClientImplTest`, `ValidatorTest`, rewritten).
    - Config: Android sources moved behind `internal/platform` interfaces — `DeviceIdSources` (`Settings.Secure` android id, Play Services app set id) and `FcmTokenSource` (Firebase), both Robolectric-tested adapters. `DeviceIdHelper(sources, prefs, config)` returns the id (`suspend withDeviceIdMode`), polls a custom provider with `delay`, falls back to a random UUID on null/failure and **rethrows cancellation** (a cancelled wait used to fall back to a random UUID). `ConfigRepositoryImpl(prefs, restConfig, fcmTokens)` lost its `Context`.
    - Network: `RestClientImpl(restConfig, config, logSink, connections = HttpsConnections)`. `HttpConnections` replaces `ConnectionManager`; `HttpsConnections` refuses plain http and ignores a socket factory the host app installed globally. Callbacks are invoked outside the `try` (a throwing callback no longer gets a second `onFailure`), the connection is closed before a retry, a body sets `doOutput`, query params are encoded with `URLEncoder` (`%20` for spaces) instead of `android.net.Uri`, logs go to the SDK log sink as one `METHOD → status` / `METHOD failed: <redacted summary>` line. Removed the gzip upload path (only reachable through a caller `Content-Encoding` header no caller sets). Tests run on plain JUnit 5 against `LocalHttpServer` (fixture: scripted answers, recorded requests incl. chunked bodies).
    - Prefs/files: `SharedPrefsManager` (round trips, cached configuration incl. a lost access key, which still fails boot as before) and `FileManager(context, dispatchers)` (io dispatcher injected, writes with `writeText`, bundled `base_html` fallback) are Robolectric adapters.
    - Util: API 33 branches of `getApplicationMetaData` / `getResolveInfoList` / `queryBroadcastReceivers` and `isOsVersionSupported` go through internal `sdkInt` overloads tested on JUnit 5 with mocked framework objects; `BuildUtil.trampolinesDisabled`, `DeviceInfo.deviceModel`, `SchedulerUtils.outdatedDeviceAndUserTime(debugView, debugBuild)` are pure. `BitmapUtil.getScaledBitmap` returns `null` right away when the download fails (no second error log), and the unscaled image when it can't be scaled (a 1×4000 image; Robolectric tests with generated PNGs, `stubPng`).
    - Behaviour changes: no more logs of email, phone, external id, device ids, FCM token (`DeviceIdHelper`, `RestConfig`, `ConfigRepositoryImpl`, `SharedPrefsManager`), contexts and image URLs (`Util`, `BitmapUtil`, `DeviceInfo`); `BuildUtil` reads the target SDK every time instead of caching it process-wide; `toStringVerbose` of an empty bundle reads `Bundle: []` (was `Bundle:]`); a bundled `base_html` that can't be read now fails the step instead of returning blank html.
    - CHANGELOG (technically public): `ConfigRepository.getAppPackageName` / `getAppPackageInfo`; `com.reteno.core.util.TAG`, `PROP_KEY_DEBUG_VIEW`, `PROP_VALUE_DEBUG_VIEW_ENABLE`, `THREAD_PREFIX_NAME`.
    - Mutation-checked (all killed): custom url getting identity headers, 408 not retried, retry count not decremented (needed a new two-retry test), `+` for spaces, global socket factory honoured, blank bundled html, null instead of unscaled image, encrypted-db check inverted, Kyiv zone fix dropped, debug view ignored, trampolines without target check, custom device id flag default.
    - Final Coverage Report (10d-3) — Kover: Core gated packages 5587/5587 lines, 1597/1597 branches (1140 JUnit 5 + 289 Robolectric tests); Push and FCM at 100 %; fixtures and `:app:compileDebugKotlin` green. Checklist → tests: device id modes / fallback / cancellation (`DeviceIdHelperTest`), identity copies (`RestConfigTest`), token and channel logic (`ConfigRepositoryImplTest`), platform sources (`DeviceIdSourcesTest`, `FcmTokenSourceTest`), prefs round trips (`SharedPrefsManagerTest`), headers per contract / body / query / redirects / statuses / retries / dropped connection / timeouts / log redaction / https-only (`RestClientImplTest`), files (`FileManagerTest`), util (`UtilTest`, `BitmapUtilTest`, `BuildUtilTest`, `DeviceInfoTest`, `SchedulerUtilsTest`, `ResponseCallbackTest`, `ValidatorTest`). Not applicable: callback thread and cancellation of the blocking `RestClient` (covered by `SuspendApiClientTest`). Gaps: none in gated packages.
  - 10d-4 DB layer: self-contained Robolectric tests on real SQLite for managers, `DbUtil*`, schemas; `BaseRobolectricTest` / `RetenoTestApp` deleted.
    - ☑ Done (D13: stored data frozen, DB code restructured). Gated: `data.local.database` (whole package). `BaseRobolectricTest`, `RetenoTestApp`, `base.robolectric.Constants` and the 14 legacy DB tests that mocked `RetenoDatabase` / `Cursor` are deleted; `RetenoActivityHelperActivityTest` rewritten self-contained (full lifecycle-helper rewrite stays in 10d-5).
    - `RetenoDatabaseImpl`: one `write` transaction helper (open failure still propagates, SQLite / illegal-state failures logged with table name only and answered with -1 / 0 / empty), `read` for queries (mitigate, rethrow), version-keyed `migrations` list, `openRetrying(sleep)` (5 attempts, 400 ms × attempt backoff, first failure rethrown) tested directly; unused `insertOrThrow` / `update` removed; the explicit "drop encrypted database" step removed (Android's corruption handler already deletes and recreates a file that isn't SQLite — a mutation run proved it equivalent; test kept). Tests on real SQLite: create, foreign keys, CRUD, missing table / column mitigation, closed database, upgrades from versions 1 and 10 (old schemas written in the test), downgrade, encrypted file, open retries.
    - Managers share `readRows` (read, collect unreadable rows, delete them after the cursor closes, report once without row content) and `selectColumns` / `limitClause`; readers use non-null getters for `NOT NULL` / `INTEGER PRIMARY KEY` columns (null checks there were unreachable). `RetenoDatabaseManagerImpl` tested on JUnit 5 with interface mocks; everything else on real SQLite through the `TestDatabase` fixture.
    - Fixed: in-app message `position` was never read back (the join query didn't select it, so every reload returned `null`). Four managers shared one `ContentValues` field across calls (unsafe with concurrent IO coroutines) — each call builds its own. Delete-by-time used string-built SQL (`< '$time'`) — now bound arguments (test: a quote-injection value deletes nothing). PII (devices, users, `ContentValues`, links) no longer logged.
    - Behaviour changes: `insertMultiple` answers an empty list when the transaction fails (was ids of rolled-back rows); DB writes and counts no longer swallow every `Throwable`, only SQLite / illegal-state failures; a commit failure answers the failure value instead of the uncommitted id. Kept as stored: a null in-app `model` is written as the text `"null"`.
    - Mutation-checked with `--no-watch-fs` (the first 10d-3/10d-4 runs reused stale classes; both sets re-run, sources checksum-identical afterwards): 23 DB mutants killed (one by the compiler); one equivalent — skipping the early return when a recommendation variant can't be stored still stores nothing, because the foreign key rejects orphan events. 10d-3's 12 mutants re-confirmed killed.
    - Schema check (after review): the first upgrade tests used hand-written "old" tables that didn't match history (version 1 `Device` without `pushSubscribed`, version 10 `UserAttributes` without its foreign key). Replaced by the exact `CREATE` statements of every distinct released schema, extracted from git history into `src/test/resources/database/history` (12 schemas for versions 1–10; versions 4, 7 and 9 each shipped more than one schema without a version bump). Tests: a fresh install creates all 16 tables exactly as their schema constants declare; upgrading every released schema gives the current columns with existing columns in place (migrations only append); stored devices, users and attributes survive. Table creation and all migration statements are byte-identical to `master`.
    - Fixed (pre-existing on `master`, decided 2026-10-16): a database from build `3f28644` (version 9 before `iamPosition` existed) never got `iamPosition`, because its migration only ran for versions below 9; on `master` saving in-app messages silently failed for those users. The same `ALTER` now also runs for version-9 databases (`10 to …VERSION_9`; databases that have the column hit "duplicate column", already ignored). Mutation-checked.
    - Final Coverage Report (10d-4) — Kover: Core gated packages 6547/6547 lines, 1759/1759 branches (1143 JUnit 5 + 229 Robolectric tests); Push 134 and FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green. Checklist → tests: create / upgrade / downgrade / mitigation / closed db / retries (`RetenoDatabaseImplTest`); per manager — every field round trip incl. nullable columns empty and set, limit and order, unreadable rows removed (row without id reported, kept), table unreadable, counts, deletes incl. by time with bound arguments (`RetenoDatabaseManager*ImplTest`); sync check over all tables (`RetenoDatabaseManagerImplTest`). Not applicable: cancellation and callback thread (blocking DB layer, called by actions on `dispatchers.io`). Gaps: none in gated packages.
  - 10d-5 Android UI and entry points: in-app containers (incl. the copied `SwipeDismissBehavior`), `permission`, lifecycle helper, facade (`Application` / lifecycle as interfaces → JUnit 5), `SdkComponent`, `Reteno` / `RetenoInitializer` / `BackwardCompatibilityDriver`.
    - Split into 5a in-app views, 5b `permission`, 5c lifecycle helper, 5d facade and entry points (`RetenoInternalImpl`, `RetenoInternalFacade`, `Reteno`, `RetenoConfig`, `RetenoInitializer`, `BackwardCompatibilityDriver`, `SdkComponent`). Starting gap (whole module): 569 lines, 271 branches.
    - 5a ☑ In-app views; `com.reteno.core.view` gated. Decided 2026-10-16: the public 383-line Java copy of Material's `SwipeDismissBehavior` is replaced by internal Kotlin — `SwipeGesture` (pure rules: dismiss past 50 % or on a fling in an allowed direction, fade out over the first half, clamping; JUnit 5) and `SwipeDismissBehavior` (`ViewDragHelper` adapter with the one-finger guard and the accessibility dismiss action; Robolectric touch tests). Unused modes (sensitivity, RTL, one-way horizontal) dropped. Fixed: a vertical fling toward the edge with no displacement settled the message the wrong way. CHANGELOG (technically public): `com.reteno.core.view.iam.container.slideup.SwipeDismissBehavior`.
    - Containers share `createInAppWebView`: file and content access off (they defaulted to on below Android 11 next to JavaScript and the JS bridge), JS console messages go through `Logger` with `Redact.text` instead of an unconditional `Log.d`. Containers lost their try/catch blocks; `AndroidInAppScreen` guards every container call and builds containers through `IamContainerFactory` (`AndroidIamContainers`), so its test no longer mocks a companion. Slide-in start offset is a pure function (`slideInStart`, WebView measures 0 in Robolectric).
    - Test isolation fix: `Logger` replays remote-log records buffered before install, so tests asserting on `FakeRemoteLog` saw records from earlier tests (one failed in the full run; DB tests and `UtilTest` passed by order). New fixture `Logger.installCapturing(sink, remoteLog)` drains the buffer first.
    - Mutation-checked (`--no-watch-fs`, sources checksum-identical): 14 killed after one test tightening (sideways jitter during a vertical drag).
    - Gate after 5a: Core gated 6757/6757 lines, 1867/1867 branches (1152 JUnit 5 + 254 Robolectric tests); Push 134 and FCM 8 at 100 %; fixtures and `:app:compileDebugKotlin` green.
    - 5b ☑ `permission` (gated). Fixed three hangs in notification-permission requests: a plain `Activity` on top got a checker without launchers (request never answered) — only `ComponentActivity`s get checkers now; a second request overwrote the first one's continuation — requests are serialized with a `Mutex`; an activity destroyed (e.g. rotated) during the dialog left the caller waiting forever — pending requests are answered "not granted" on destroy, the late answer to the recreated activity is ignored. `requestChecker` no longer swallows cancellation (`withTimeoutOrNull` instead of `runCatching`); launcher registration failures are caught narrowly (`IllegalStateException`) and logged. CHANGELOG (technically public): `AndroidActivityAware.requireActivity()` (protected, unused). Tests drive real `ComponentActivity` permission flows (`onRequestPermissionsResult`, `controller.recreate()`).
    - 5c ☑ lifecycle helper (`lifecycle` gated). One guarded notification path: a failing callback is logged and no longer stops the others; callbacks are notified outside the lock in registration order. The Android 12 "no trampolines" callbacks class was identical to the base one and is gone (`BuildUtil.shouldDisableTrampolines` stays for Push). `Logger.i` on every lifecycle event removed. The callbacks object has a concrete type (`LifecycleCallbacks`) so tests can call `onActivityPreCreated`, which Robolectric's API 26 interface doesn't have. Old `RetenoActivityHelperActivityTest` and the unused `TestFragment` folded into `RetenoActivityHelperImplTest`.
    - 5d ☑ facade and entry points (classes gated: `Reteno`, `RetenoInternalImpl`, `RetenoInternalFacade`, `RetenoConfig`, `RetenoInitializer`, `BackwardCompatibilityDriverKt`, `SdkComponent`). Decided 2026-10-16: `RetenoConfig.Builder(config)` / `Builder(config, accessKey)` now copy every setting (they dropped `platform` and `sessionDuration`); the deprecated `RetenoConfig(...)` factory and `RetenoImpl(application, accessKey)` keep throwing without an access key as on `master` (tests document it). Facade: redundant OS checks inside `runAfterInit`, unreachable try/catch blocks and `Logger.i` lines (one printed the device id) removed; `hasDataForSync` keeps its guard (SQLite can fail to open). WorkManager fallback moved to `internal/platform/AppWorkManager.kt`. `PushDataWorkerTest` builds a real facade instead of mocking the concrete `RetenoInternalImpl`. Entry points are tested with the OS pinned below API 26 (`onAndroidSdk(25)`), so the real SDK is constructed without booting.
    - Mutation-checked 5b–5d (`--no-watch-fs`, sources checksum-identical): 17 of 18 killed after tightening the activity-wait test to assert the 2 s timeout and re-running the request-lock mutant in a compiling form. One survivor changes no outcome: not dropping a destroyed activity's checker only leaks it (a destroyed activity is never resumed, so it is never handed out).
    - Final Coverage Report (10d-5) — Kover: Core gated 7213/7213 lines, 1977/1977 branches (1158 JUnit 5 + 302 Robolectric tests); **whole Core module 7227/7227 lines, 1979/1979 branches**; Push 134 and FCM 8 at 100 %; fixtures and `:app:compileDebugKotlin` green. Gaps: none. Pending for 10d-6: `RestConfig` and `InAppMessage` (gated, JUnit 5 tested) are still mocked as concrete classes in other JUnit 5 tests, so their coverage depends on test order — replace with real instances / stubs before flipping the rule.
  - 10d-6 Flip the Kover rule to the whole Core module at 100 % (Push and FCM already are).
    - ☑ Done. `koverReport` now excludes only `R` / `BuildConfig` (the gated package list and `-PkoverWholeModule` are gone); rule renamed "Core module is fully covered". Before flipping, the last order-dependent mocks of concrete gated classes were removed: actions that need the device id take the new `DeviceIdentity` interface (`RestConfig.kt`; `RestConfig` implements it, `SdkInfrastructure.restConfig` exposes it) and their tests mock the interface; `SessionTrackerTest` uses `stubInAppMessage()` instead of a mocked `InAppMessage`. Remaining concrete mocks (`SharedPrefsManager`, `FileManager`) are Robolectric-tested adapters mocked only in JUnit 5 tests, i.e. in the other JVM.
    - Final Coverage Report (10d-6) — Kover: **whole Core module 7227/7227 lines, 1979/1979 branches** (1158 JUnit 5 + 302 Robolectric tests); Push 134 and FCM 8 tests at 100 %; fixtures and `:app:compileDebugKotlin` green.
- 10e ☑ README / CHANGELOG (collected removals) / version bump.
  - Version 2.11.0 (decided 2026-10-16; `gradle.properties` `VERSION_NAME`, flows into `BuildConfig.SDK_VERSION`).
  - New `CHANGELOG.md`: changed behaviour, fixes, unchanged deprecations and every removed technically-public type — checked against `master` by diffing public top-level declarations (30 types), plus removed members and constants from the phase notes.
  - README setup rewritten to the current API: `Reteno.initWithConfig(RetenoConfig.Builder().accessKey(...).build())` in `Application.onCreate`, `Reteno.instance`, `RetenoNotifications.requestNotificationPermission()`; the access key comes from build configuration, not source. The old guide used `RetenoImpl(this, key)`, which throws. Step numbering and the LICENSE link fixed.
  - SEC3: `BuildConfig.SQL_PASSWORD` (unused since database encryption was removed) is no longer compiled into the AAR, and the build no longer reads `RetenoSdkCore/keys.properties`. The value shipped in every earlier release — raise with the security owner.
  - Gate: whole Core module 7227/7227 lines, 1979/1979 branches; Push 134 and FCM 8 at 100 %; fixtures, `:app:compileDebugKotlin` and release builds of Core, Push and FCM green.
  - Fixed after 10e (2026-10-16): `Util.formatSqlDateToTimestamp` reads SQLite `CURRENT_TIMESTAMP` text as UTC with an immutable `DateTimeFormatter` (`Locale.US`); it used a shared `SimpleDateFormat` in the device time zone. Row-to-row comparisons were unaffected (same offset on both sides), absolute times and concurrent reads were not. `deleteWrappedLinksByTime` compares `timeStamp < datetime(?)`, because the cutoff is ISO text (`…T…Z`) while the column holds `yyyy-MM-dd HH:mm:ss`; clicks from the cutoff day were deleted early. Both mutation-checked; gate 7226/7226 lines, 1979/1979 branches.
  - Code review after 10e (2026-10-16), all nine findings handled:
    1. ☑ `SwipeDismissBehavior` keeps the `ViewDragHelper` captured at release for the whole settle; it looked the helper up again through `child.parent` on every frame and crashed when the in-app was removed mid-swipe.
    2. ☑ `LauncherContext` forgets its pending continuation when the request is cancelled or `launcher.launch` throws (a later answer or `abandon()` resumed a finished coroutine). An identity check against a newer request was dropped: the checker's `Mutex` makes overlap impossible, and Android answers an overlapping permission request at once anyway.
    3. ☑ `PermissionActivityDelegate.requestChecker()` polls on the injected main dispatcher (the facade passes its `mainDispatcher`); callers come from any thread.
    4. ☑ Migrations are `ColumnMigration(table, column, statement)` and run only when `PRAGMA table_info` lacks the column; every upgrade used to report "duplicate column" errors for tables `onUpgrade` had just created. Schema SQL unchanged (D13). Tests: every released schema upgrades with no errors; a table left as a view still reports its failed migration.
    5. ☑ `readRows` treats a row whose read throws as unreadable (removed, rows after it still read); one broken row used to block its whole table on every sync.
    6. ☑ No change needed: a null `whereArgs` value binds as SQL `NULL`, so deleting a device/user/interaction/interaction request/in-app interaction without a row id answers `false` without throwing. Five tests pin it.
    7. ☑ `Util.isEncryptedDatabase` removed with its tests (CHANGELOG → Removed).
    8. ☑ `TopActivityTest` uses the shared `fixtures/AndroidSdk.kt` `onAndroidSdk`.
    9. ☑ AGENTS.md DB-test line describes `storeReleasedDatabase`; new learnings added.
    - Mutation checks: 1 (helper lookup through `child.parent`), 2 (both clears), 3 (`withContext` → `run`), 4 (skip removed, inverted, wrong column index, error log removed), 5 (per-row catch, the red test) — all killed.
    - Final Coverage Report — Kover: **whole Core module 7247/7247 lines, 1981/1981 branches** (1155 JUnit 5 + 315 Robolectric tests); Push 134 and FCM 8 tests at 100 %; fixtures (5) and `:app:compileDebugKotlin` green.
- 10f ☑ Composition split (2026-10-16): `ActionsLocator` (786 lines doing three jobs) replaced by `ActionGraph` + `ActionCore` + one `di/graph/<Feature>Graph` per `internal.action.<feature>` package + `SdkActionsRouter` (routing only). No public API or behaviour change for integrators.
  - Graph members are uniformly `by lazy` (graphs reference each other in cycles: sync ↔ inbox, user ↔ start/stop, in-app ↔ lifecycle); the 16 pass-through `by lazy { infrastructure.x }` wrappers and the facade's `backgroundSync by lazy` are gone.
  - `SdkComponent`: database wiring moved to `DatabaseManagers` (`SdkInfrastructure.db`, built by `databaseManagers(database)`); `InitGate` moved into `ActionCore`; `AndroidAppPlatform` and `PermissionActivityDelegate` built by the component; takes `Application`.
  - Facade: depends only on `FacadeInfrastructure` (`actions`, `configuration`, `permissions`). Push queries (`deviceId`, `defaultNotificationChannel`, `canPresentMessages`, `isActivityPresented`, `hasDataForSync`) are `SdkQueries` on `SdkActions`; `isActivityPresented` / `getDefaultNotificationChannel` now check `isOsSupported()` (answers unchanged on API < 26: no activity is tracked there and no channel is saved). Activity tracking starts in `boot()` through `AppPlatform.trackActivities()`. `backgroundSync()` is an internal facade function answering `NO_DATA` on unsupported OS. The unreadable-database fallback reports through `RemoteLog`.
  - `SessionTracker` stays in `SdkComponent` (it needs `events.logEvent`): moving it into the graph would turn the wiring tests' session-handler verifications into behaviour tests of `SessionTracker`.
  - Tests: `LocatorFixture` → `ActionGraphFixture` (`graph`, `sut = graph.router`), `ActionsLocator*Test` → `ActionGraph*Test`; new: router queries and database fallback, activity tracking on boot, `AndroidAppPlatform.trackActivities`, facade query forwarding and OS checks, background sync forwarding, unsynced data through one shared database (`SdkComponentTest`).
  - Final Coverage Report — Kover: **whole Core module 7418/7418 lines, 1987/1987 branches** (1477 tests); Push and FCM `koverVerifyDebug` green; fixtures, `:app:compileDebugKotlin`, `:RetenoSdkCore:compileReleaseUnitTestKotlin` green.
- 10g ☑ Platform split (2026-10-16): `AppPlatform` (13 unrelated methods, every consumer depending on all of them) replaced by four ports in `internal/platform`, each with its Android class in the same file: `AppEnvironment` / `AndroidAppEnvironment` (installed version, device description, log-event origin, bundled widget), `AppBroadcasts` (implemented by `AppBroadcaster`), `HostUi` / `AndroidHostUi` (activity tracking, lifecycle and fragment watchers, top package, in-app screen), `BackgroundWork` / `AndroidBackgroundWork` (WorkManager). No behaviour change.
  - Consumers take only what they use: `AppForegroundedAction` broadcasts + environment; `UpdatePushPermissionStatusAction` broadcasts; `TrackPermissionDialogAction`, `EnableAutoScreenTrackingAction`, `AppLifecycleTrackingImpl` host UI; `BootSdkAction`, `DataSync`, `BackgroundSyncAction` background work; `CurrentDevice`, `LoadPushInAppHtmlAction`, `RemoteLogRecorder` environment. `ActionCore` holds the four ports; `SdkComponent` builds them, sharing one `AppBroadcaster` between broadcasts and the in-app screen.
  - Tests: `AndroidAppPlatformTest` split into `AndroidAppEnvironmentTest`, `AndroidHostUiTest`, `AndroidBackgroundWorkTest` (test bodies unchanged); its broadcast test dropped (`AppBroadcasterTest` covers `send`). `FakeAppPlatform` implements all four ports; consumer tests that mocked `AppPlatform` mock the narrow port.
  - Final Coverage Report — Kover: **whole Core module 7433/7433 lines, 1987/1987 branches**; Push and FCM `koverVerifyDebug` green; fixtures, `:app:compileDebugKotlin`, `:RetenoSdkCore:compileReleaseUnitTestKotlin` green.
- 10h ☑ OS decision in the composition root (2026-10-16): `SdkComponent.actions` is the real router on a supported OS and `UnsupportedOsActions` (null object) below API 26. `RetenoInternalImpl`, `AppInboxImpl` and `RecommendationImpl` no longer check the OS (about 25 guards and the `isOsSupported` / `mainDispatcher` constructor parameters gone); the feature facades take plain `InboxActions` / `RecommendationActions` instead of `Lazy`.
  - "Run after init" moved from the facade into `SdkActions.runAfterInit` (router: run now, or on the main scope once the init gate opens, in call order; null object: drop the operation, so nothing waits forever on an old OS). `SdkActions.awaitInit` and the facade's `syncScope` are gone; `executeAfterInit` forwards to it.
  - Behaviour change (CHANGELOG → Changed, decided 2026-10-16): on API < 26 `setUserAttributes` / `setMultiAccountUserAttributes` / `setAnonymousUserAttributes` validate their input and throw like on supported versions. Everything else answers as before (state `false`, device id and channel `""`, `hasDataForSync` `true`, background sync `NO_DATA`, config kept). The facade now registers its lifecycle observer on every OS; `start`/`stop` are no-ops there.
  - Tests: `UnsupportedOsActionsTest` (state, query answers, background sync, config kept, `runAfterInit` never runs, every operation ignored without answering callbacks); `SdkComponentTest` picks router vs null object by OS; router `runAfterInit` order before init and immediate after init (`ActionGraphInitWiringTest`); the facade's and feature facades' unsupported-OS tests removed; `FakeSdkActions` holds `runAfterInit` operations until `markInitialized()`.
  - Final Coverage Report — Kover: **whole Core module 7441/7441 lines, 1949/1949 branches** (38 OS-check branches fewer); Push and FCM `koverVerifyDebug` green; fixtures, `:app:compileDebugKotlin`, `:RetenoSdkCore:compileReleaseUnitTestKotlin` green.
- 10i ☑ Queues in their graphs (2026-10-16): the four `SerialQueue`s left `SdkActionsRouter`, which now only routes. `InitGraph.initQueue` (main scope), `InAppGraph.inAppQueue`, `RecommendationGraph.eventsQueue`, and `DeviceGraph.contactQueue` (user and device updates, next to the shared `contactSession`); each is `by lazy`, so its loop starts on first use instead of when the graph is built. Not moved into actions: a queue keeps the order of fire-and-forget calls at submission, which a `suspend invoke` (or a `Mutex` inside it) can't, and the recommendation-events order is pinned by a test. No behaviour change.
  - Tests: existing order tests cover the moves (`should run submitted contact updates in order`, `should save submitted recommendation events one after another in call order`, init order in `ActionGraphInitWiringTest`); the contact queue failure test reads `graph.device.contactQueue`.
  - Final Coverage Report — Kover: **whole Core module 7444/7444 lines, 1949/1949 branches**; Push tests and `:app:compileDebugKotlin` green.
- 10j ☑ Run after init in the gate (2026-10-16): `InitGate.runAfterInit(scope, operation)` runs the operation at once when initialized, otherwise launches it on the given scope once the gate opens (call order kept, dropped if the scope is cancelled). The scope is a per-call argument, so `InitGate` stays a plain value holder. `SdkActionsRouter.runAfterInit` only forwards it with the main scope; `SdkActions.runAfterInit` stays the facade's port (Robolectric facade tests can't load the gated `InitGate`; `UnsupportedOsActions` still drops the operation). No behaviour change.
  - Tests: `InitGateTest` (held until init then call order, at once on the caller after init, held operation on the scope's dispatcher, dropped with a cancelled scope); `ActionGraphInitWiringTest` now pins that the router runs held operations on main (order moved to the gate test).
  - Final Coverage Report — Kover: **whole Core module 7444/7444 lines, 1949/1949 branches**; Push tests and `:app:compileDebugKotlin` green.
- 10k ☑ Router split by feature group (2026-10-16): `SdkActionsRouter` (49 members) deleted. `SdkActions` is now one property per group — `control` (`SdkControl`), `queries` (`SdkQueries`), `contact` (`ContactActions`), `events` (`EventActions`), `lifecycle` (`LifecycleActions`), `inApp` (`InAppActions`), `interactions` (`InteractionSubmitter`), `push` (`PushActions`), `sync` (`SyncActions`), `inbox`, `recommendation` — each interface in its `internal.action.<feature>` package. One router per group in `di/route/` (`InitRouter`, `QueryRouter`, `ContactRouter`, `EventRouter`, `LifecycleRouter`, `InAppRouter`, `InteractionRouter`, `PushRouter`, `SyncRouter`, `InboxRouter`, `RecommendationRouter`), built `by lazy` as `router` in the graph that owns it; `ActionGraph.actions` (`SdkActionsImpl`) holds them, `SdkComponent.actions` returns it on a supported OS. `submitRetenoLogEvent` left the facade port: `EventRouter` is the `LogEventQueue`, which `RemoteLogRecorder` now takes as `Lazy` (`ActionCore` is built before the graphs). `UnsupportedOsActions` is one null object per group in one file. The facade calls `actions.<group>.submit*`; `AppInboxImpl` / `RecommendationImpl` get `actions.inbox` / `actions.recommendation`. No behaviour change.
  - Tests: one fake per group (`FakeSdkControl`, `FakeContactActions`, …) sharing a `FakeCallLog`, put together by `FakeSdkActions`, so facade tests keep one ordered call list; `AppInboxImplTest` / `RecommendationImplTest` use only their group fake. Wiring tests address `sut.<group>`. New dispatcher pins (mutation-checked: each fails when its router or queue uses another scope): queued user configuration applied on main, screen tracking started on main, push permission receivers notified on io (`FakeAppPlatform.onWatchFragmentScreens` / `onSend`).
  - Final Coverage Report — Kover: **whole Core module 7523/7523 lines, 1949/1949 branches**; Push, FCM `koverVerifyDebug` and fixtures green; `:app:compileDebugKotlin`, `:RetenoSdkCore:compileReleaseUnitTestKotlin` green.
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
| D13 | How frozen is the DB layer? | **Decided 2026-10-15: data, not code.** Schemas, SQL, table/column names, stored formats, migrations' effect and the manager interfaces/behaviour stay; implementation code may be restructured for testability (one transaction helper, injectable open retries, unreachable defensive branches removed) and PII is dropped from DB log messages. Tests run on real SQLite incl. upgrades from old schema versions; DB packages join the 100 % gate. |
