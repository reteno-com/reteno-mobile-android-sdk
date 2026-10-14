# Reteno Android SDK — Agent Playbook

Android SDK in Kotlin (1.8.0, coroutines 1.6.4, AGP 8.1, Gradle 8.1). Modules: `RetenoSdkCore`, `RetenoSdkPush`, `RetenoSdkFcm`, sample `app`, and the test-only `RetenoSdkTestFixtures`.
The SDK is being refactored to one-file-per-operation Actions on coroutines; the plan and its decisions live in `docs/refactoring/coroutine-actions-plan.md`. Read it before touching an SDK flow.

## Hard constraints

- **Toolchain is frozen.** Do not bump Kotlin, coroutines, AGP or Gradle. No Kotlin 1.9+ features (`data object`, `entries`), no coroutines 1.7+ APIs (`TestScope.backgroundScope`, `runTest(timeout = …)`).
- **DB layer is frozen.** `RetenoDatabaseManager*` interfaces/impls, schemas, `DbUtil*` and local mappers stay unchanged for backward compatibility. Actions call them from `withContext(dispatchers.io)`.
- **Public API is frozen.** `Reteno`, `AppInbox`, `Recommendation`, `RetenoNotifications`, callbacks and public models stay source- and binary-compatible. Never add companions or members to public models for test convenience.
- Run Gradle with **JDK 17** (what CI uses): `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`. Gradle 8.1 cannot compile build scripts on newer JDKs and Robolectric 4.8.1 cannot read their class files.

## Core conventions

- **Never write comments** in new code (production or tests). Express intent through names: a `step("…")` name, an extracted function, a named value. Existing comments stay untouched. Exception: KDoc on public SDK API, which consumers read.
- New internal code lives under `com.reteno.core.internal.*` with `internal` visibility:
  - `internal.action.<feature>` — one Action per SDK operation
  - `internal.action.trace` — `ActionRunner`, `ActionTrace`, `TraceFormatter`, `ActionFailureReporter`, `MonotonicClock`
  - `internal.coroutine` — `RetenoDispatchers`, `retenoScope()`, `InitGate`
  - `internal.network` — `SuspendApiClient`, `ApiResult`
  - `internal.event` — `EventBus`
  - `internal.log` — `RetenoLogSink`, `AndroidLogSink`, `LogPriority`, `Redact`
- No `OperationQueue`, `PushOperationQueue`, `Executors`, `Handler` or `runBlocking` in new code.
- No `Reteno.instance` / `RetenoInternalImpl.instance` / `ServiceLocator` access inside an Action. Dependencies come through the constructor.
- `ServiceLocator` is deleted in Phase 10 (plan D12). Don't add providers or public members to it; new objects are built in `ActionsLocator`, which reads `ServiceLocator` only for legacy instances that haven't been migrated yet. Don't build new sample-app features on SDK internals; the sample app is rewritten against the public API at the end.

## Recipe: new Action

New actions are wired in `di/ActionsLocator.kt` (one `by lazy` property per action, built from `ServiceLocator` providers) and called from the facade through `actions.<name>(...)`.

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

- **An action's logic lives in its own file.** Its constructor takes real collaborators — DB managers, repositories, controllers, `RetenoSessionHandler`, `SharedPrefsManager`, `RestConfig`, `InitGate`, `SdkConfiguration`, `AppPlatform`, `RemoteLog`, other actions — and the action calls them inside its named steps. Function-typed dependencies are only for sources of time and identity (`clock`, `jitter`, id generators). Never build behaviour in a lambda inside `ActionsLocator` or `ServiceLocator`; the locator only constructs objects and routes facade calls onto queues and scopes. Android framework calls go through a named `AppPlatform` method.
- One file under `internal/action/<feature>/`; `suspend operator fun invoke(...): Result<T> = runner("Name") {` on one line, or with one parameter per line when it doesn't fit in 120 columns.
- **An action reads top to bottom like its spec.** Kotlin official style, 120-column limit, 4-space indent:
  - Every `step`/`stepOrNull` body, and every `runner(...)` body, starts on a new line and the closing `}` gets its own line — never `step("…") { call() }`. A dispatcher switch inside a step stays on one line when it fits (`withContext(dispatchers.io) { eventsDb.insertEvents(...) }`).
  - A conditional step is a braced block (`if (forcePush) {` / `step(…) {` …), never `if (x) step(…) { … }`; a step's result used in a condition goes into a named value first (`val inForeground = step(…) { … }`, then `if (inForeground)`).
  - When a `step(...)` call, `args(...)`, `api.request(...)`, a signature or a constructor call doesn't fit, put one argument per line; locator constructor calls that wrap use named arguments.
  - The wait for init and the server call are written in the action itself, not hidden in a shared helper; a few repeated lines are cheaper than an action you can't read in one pass.
  - One blank line after a class header and between members, none after a function's opening brace or before a closing one, never two in a row; files end with one newline.
  - Tests and fixtures follow the same layout: 120 columns (only a backticked test name may be longer), `runner`/`step` bodies on their own lines, wrapped calls one argument per line or a named `val expected`, one blank line before `whenn()` and `then()`, `SutFixture` first and `companion object` constants last. Legacy-style tests that are still waiting for their rewrite (Phase 8/10) are left as they are.
- Arguments go first inside the block: `args("type" to event.eventTypeKey)`. The start line is printed at `args(...)` (or before the first step/note/child/end if there are none); `args` called later prints a `→ key=value` note. Values are formatted only when logging is on; pass `"key" to { expensive() }` for values that are costly or need `Redact`.
- Every unit of work is a named `step(...)`. A step name says **what happens and where**: verb + object + source/destination — `"save event to local database"`, `"load unsynced devices from local database"`, `"send device to server"`, `"notify in-app message triggers"`. Never a bare verb (`"insert event"`, `"emit"`).
- Sync steps end an empty run with `nothingToSend()` (or `finishedAfter(handled)` in draining loops), so every step prints `→ nothing to send` when it sent nothing and stays quiet after it drained rows.
- Server calls are named in plain words (`"send interactions to server"`). **Never put URLs, hosts, paths, headers or bodies in step names, args, details or notes.** Never derive a step name from `ApiContract.url`.
- Anything user-related in `args` / `detail` / `note` goes through `Redact` (`token`, `email`, `phone`, `fields`, `text`, `summary`, `stackTrace`). Never format PII yourself.
- `runner(...)` rethrows `CancellationException`, logs `✓ SUCCESS` / `✗ FAILURE` / `⊘ CANCELLED`, and reports exactly one remote log event per failed action (no args included). A child `runner(...)` inside a parent nests automatically (`[root.n]`).
- Validation the caller must see (blank `externalUserId`, invalid `marketId`) is done synchronously in the facade before deferring, not inside the Action.
- Host callbacks are invoked via `withContext(dispatchers.main)`. Actions that answer a host `RetenoResultCallback` from a server call use `passResponseToHost` (`internal/action/inbox/HostResponse.kt`): a server failure goes to `onFailure(code, body, error)` and is only traced, never uploaded; a response that can't be read goes to `onFailure(null, null, error)` **and** fails the action, so it is reported once.
- A documented fallback is not a failure. When the host contract has one (recommendations' `onSuccessFallbackToJson`), read with `stepOrNull(…)` (`internal.action.trace`), answer the fallback callback and let the action succeed: the failed step stays in the trace, nothing is reported. Never `runCatching { step(…) }` for this: it swallows `CancellationException` and turns cancellation into the fallback.
- Operations that need the SDK configured (network, device id) wait with a named step `step("wait for sdk initialization")` (body `initGate.awaitInit()`) inside the action instead of being gated in a facade.

## Logging

- Single logcat tag `RetenoAction` for action traces, `Reteno` for uncaught scope errors. Lines are capped at 1 000 chars; the sink splits messages above 4 000.
- Logging is on only when `BuildConfig.DEBUG || Util.isDebugView()` (`isLoggingEnabled()`); when off, no message lambda is evaluated.
- Legacy `RestClientImpl` logs only method, status and a redacted error summary. Keep it that way.

## Testing

Run: `./gradlew :RetenoSdkCore:testDebugUnitTest :RetenoSdkCore:koverVerifyDebug` (filter with `--tests 'com.reteno.core.internal.*'`), `:RetenoSdkPush:testDebugUnitTest :RetenoSdkPush:koverVerifyDebug`, `:RetenoSdkFcm:testDebugUnitTest :RetenoSdkFcm:koverVerifyDebug`. Fixture self-tests: `./gradlew :RetenoSdkTestFixtures:test`.

Hard rules:
- **Never `runBlocking`.** Every test body is `runUnitTest { }` from `com.reteno.test`.
- `given()` / `whenn()` / `then()` are **required in every test**; `runUnitTest` fails the test if one is missing.
- **Fresh SUT and fresh mocks per test** via `private class SutFixture { val dep = mockk<…>(); val sut = X(dep) }`. No class-level mocks, no `@Before` state, no shared state. Global mocks (`mockkStatic`, `mockkObject`) are set up and torn down inside the test, in `try/finally` (see `RestClientImplLoggingTest.SutFixture.withCapturedLogs`).
- Test names are backticked sentences: `` `should abort sync when device push fails with repeatable error`() ``.
- JUnit 5 (`org.junit.jupiter.api.Test`, `Assertions`). Assert failure types with `is`.
- Robolectric tests (SQLite, Android framework behaviour) use JUnit 4 (`org.junit.Test` + `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [26])`) — they run through the vintage engine — but still use `runUnitTest` and given/whenn/then.
- Never write a local per-test-file builder that re-lists a model's fields. Shared stub factories go into `RetenoSdkCore/src/test/java/com/reteno/core/fixtures/` (internal types) or `RetenoSdkTestFixtures` (generic helpers).
- Do not edit `RetenoSdkTestFixtures/src/main/kotlin/com/reteno/test/Test.kt` semantics without updating its self-tests.

Fixtures:
- `com.reteno.test`: `runUnitTest`, `given`, `whenn`, `then`, `TestScope.workScope()`.
- `com.reteno.core.fixtures`: `FakeLogSink` (captures `entries` / `lines`), `TestScope.testDispatchers()`, `TestScope.virtualClock()` (virtual time drives step durations), `CountingDispatcher`.

Mandatory coverage (contract-first): before writing code, list the checklist — happy path, every failure branch (repeatable / non-repeatable / exception), cancellation, callback thread, sync trigger, log redaction where relevant — and finish with a **Final Coverage Report** mapping each item to a test. Report gaps explicitly.

Strict TDD: red → green → refactor. A compile failure of the new test counts as red. One behaviour per test.

Coverage gate: Kover, **100 % line and branch** for the packages listed in `RetenoSdkCore/build.gradle` → `koverReport.androidReports('debug').filters.includes`, and for the whole Push and FCM modules (only generated `R`/`BuildConfig` excluded). Add a package there when its phase is done. Never add exclusions; restructure code instead. **Kover is local only — never add Kover tasks to CI.** Run `koverVerifyDebug` locally before every PR and include the result in the Final Coverage Report.

## Gotchas learned so far

- **Long-running work in tests**: no `backgroundScope` on coroutines-test 1.6.4. Launch endless loops in `workScope()`; it is cancelled before `runUnitTest` returns, so `runTest` doesn't spin forever. Drive time with `runCurrent()` / `advanceTimeBy()`, never `advanceUntilIdle()` for such work.
- **Exception identity**: Gradle runs tests with assertions on, which enables coroutines stack-trace recovery — exceptions crossing a suspension point are re-created with the original as `cause`. Don't `assertSame` exceptions that crossed `withContext`/`launch`; assert type and message. Production code that matches exceptions must compare cause chains on both sides: an error thrown inside `withContext` within a step is copied at every boundary, and the copies only share the original as `cause` (see `ActionTraceImpl.failedStepOf`).
- **Unreachable Kotlin branches fail the 100 % branch gate**: `x?.let { nonNull } ?: y` and `a?.b?.c ?: d` with a non-null `b` compile to branches no test can reach. Use `val v = x ?: return y` or an explicit `if`.
- **Loops that only end by cancellation create unreachable branches**: `while (isActive) { …; delay() }` and `for (x in channel)` (channel never closed) each have an exit branch no test reaches. Use `while (true)` with a cancellable suspension (`delay`, `channel.receive()`).
- **`when` over a nullable enum** (`when (result.getOrNull()) { …, null -> }`) adds a branch Kover can't see covered; unwrap first (`val x = … ?: return fallback`) and `when` over the non-null enum.
- **`when (x) { is A -> …; is B -> … }` over a sealed *interface* compiles an extra `else` branch** that no test can reach. Handle one case and return early, then cast: `if (response is ApiResult.Failure) { …; return }; val body = (response as ApiResult.Success).body`.
- **`BuildConfig.DEBUG` is not a compile-time constant** (`Boolean.parseBoolean`), so `BuildConfig.DEBUG || x` has a branch the debug unit-test variant can't cover. Pass it as a defaulted parameter (`isLoggingEnabled(debugBuild = BuildConfig.DEBUG, …)`) and test the parameters.
- **Kover instrumentation breaks JUnit 3's `TestCase.assertEquals`** (AIOOBE) used by some legacy tests; `kover { excludeInstrumentation { packages('junit.framework', 'org.junit', 'org.robolectric', 'io.mockk') } }` is required in every module applying Kover.
- **Never mock a concrete class from a gated package.** Creating a mock of a class retransforms its bytecode and drops the Kover hits other tests recorded for it in the same JVM, so the gate fails depending on test order (found with `InitGate`, again with `DataSync`). Anything other code depends on and tests need to replace has an interface in the same file: actions get one named after the operation (`RegisterDevice` implemented by `RegisterDeviceAction`), other collaborators a role interface (`SdkConfiguration`/`SdkConfigurationImpl`, `SyncScheduler` implemented by `DataSync`). Dependents take the interface; tests mock the interface. Small value holders like `InitGate` stay concrete and are used for real in tests.
- **Robolectric loads its own copy of every class a test touches.** When a gated class is loaded in a Robolectric sandbox, Kover drops the hits that plain JUnit 5 tests record on the other copy afterwards, so coverage depends on test order (found with `ActionsLocator.backgroundSync`, lost after a facade test built a real `ActionsLocator`). Robolectric tests never build real `ActionsLocator`/actions (facade tests use the internal `RetenoInternalImpl` constructor with `FakeSdkActions`). Tests run in one JVM (no forking), so keep gated classes out of Robolectric tests unless the class is only tested there (`internal/platform`).
- **Core facade tests** construct `RetenoInternalImpl` through its internal constructor (`ServiceLocator`, `sdkActions`, `isOsSupported`) with `FakeSdkActions` and a `ManualLifecycleOwner`; gating is checked by opening `serviceLocator.initGate`.
- **Push and FCM reach the SDK only through `RetenoInternalEntryPoint.facade`** (`RetenoInternalFacade`, opt-in `@InternalRetenoApi`; modules opt in with `-opt-in=com.reteno.core.InternalRetenoApi`). Never `RetenoInternalImpl.instance` / `Reteno.instance` there. Push tests install `FakeRetenoFacade` with `withFakeFacade { }` (FCM tests a relaxed `mockk<RetenoInternalFacade>()`), which restores the SDK instance in `finally`. A new Push need is a new facade member, implemented in `RetenoInternalImpl` by deferring to an `SdkActions.submit*`.
- **`StateFlow.first {}` waiters are not resumed in FIFO order** once collector slots get reused. Anything that must release waiters in call order (the init gate, so API calls made before init keep their order) uses `CompletableDeferred`.
- **Tests that start the SDK also start the 30 s sync scheduler.** `LocatorFixture` gives `ActionsLocator` a `workScope()`-backed scope so such loops are cancelled when the test ends; never hand it a free-standing scope on the test dispatcher.
- **Never call `mockkConstructor(ServiceLocator::class)` again inside a test** after stubs were set on `anyConstructed<ServiceLocator>()` — it resets them (Core's legacy `BaseRobolectricTest`).
- **Every Robolectric SDK level is its own sandbox, and the last sandbox to load a class wins in Kover.** Push tests at `sdk = [22]`/`[31]` next to `[26]` dropped the `[26]` hits (coverage fell to ~70 %). Keep each module's gated classes in one sandbox (`@Config(sdk = [26])`) and cover version branches by pinning `Build.VERSION.SDK_INT` with `onAndroidSdk(sdk) { }` (`ReflectionHelpers`, restored in `finally`). Pin only code that doesn't call newer framework methods — e.g. build a `NotificationCompat.Builder` inside the pin but call `build()` outside it, or you get `NoSuchMethodError`. Robolectric 4.8.1 has no API 33, so Android 13 branches are pinned too.
- **Robolectric merges the library manifest into Push/FCM tests**: Push meta-data (`com.reteno.Receiver.*`) and receivers are visible to tests. Change meta-data through `shadowOf(packageManager).getInternalMutablePackageInfo(packageName).applicationInfo.metaData` (`applicationMetaData()` fixture); make a launch activity resolvable with `registerLaunchActivity()` (resolve info **and** `addActivityIfNotPresent`, or `checkActivities(true)` rejects the explicit launch intent).
- **`@JvmStatic` members of an `object` are static calls**: `mockkObject(Util)` doesn't intercept `Util.readFromRaw`; use `mockkStatic(Util::class)`.
- **Always `disconnect()` an `HttpURLConnection` you abandon** (size limit, early return). A connection left open kept the local test server blocked in `write` and hung the whole Push test task; test servers answer each socket inside `runCatching`.
- **Android unit tests compile against `android.jar`**: no `java.awt`, `javax.imageio` or `com.sun.net.httpserver`. Serve test HTTP with a `ServerSocket` and embed images as bytes.
- **Inline functions add their branches to the caller**: `fromJsonOrNull` (`isBlank()`), `runCatching { }.getOrNull()`, `orEmpty()` count as branches of the calling class, at every call site. Funnel them through one private function (`parseChannelData`) so one pair of tests covers them.
- **A view's UI scope is no place for persistence.** `IamViewImpl.pause()` cancels `iamShowScope` children on ON_STOP, so a recording launched there can be lost; views hand interactions to `InteractionSubmitter` (`ActionsLocator`'s long-lived scope).
- **Contact operations run through `actions.contactQueue`** (`SerialQueue`), so user/device updates are applied in call order. Validation that must reach the caller (`UserAttributesValidator`) runs in the facade *before* enqueueing.
- **Don't switch Kover to JaCoCo** (`useJacoco()`): it doesn't filter coroutine state-machine branches and reports dozens of unreachable branches in every suspend function.
- **`testFixtures` is a reserved Gradle dependency method** — the shared fixtures dependency group in `test_dependencies.gradle` is called `retenoTestFixtures()`.
- Kotlin modules can't see another module's `internal` declarations, so fakes of internal SDK types live in `RetenoSdkCore/src/test/.../fixtures`, not in `RetenoSdkTestFixtures`.
- **Never give a collaborator property the name of a member function.** In `DataSync`, `private val clearOldData: ClearOldDataAction` next to `fun clearOldData()` made `clearOldData()` inside the class call the member function, not the action, so it relaunched itself every 3 s forever. Name action properties after the action (`clearOldDataAction`).
- **`step` infers `T` from the action's return type.** In `suspend operator fun invoke(): Result<Unit> = runner("X") { step("…", detail = { "n=$it" }) { count() } }` the last step becomes `step<Unit>` and `detail` prints `kotlin.Unit`. Pin it: `step<Int>(…)`.
- **A nullable callback that is always set when reached** (`callback?.invoke()` after a guard that implies it's set) is an unreachable branch. Unwrap it earlier, on a path tests do reach: `val show = callback ?: return`.
- **Android framework access goes through `AppPlatform`** (`internal/platform/`). Adapters there are tested with Robolectric; actions and `ActionsLocator` get plain functions or `FakeAppPlatform`, never a `Context`.
- **Push/Fcm tests can't see Core `internal` types** (`ActionsLocator`, `DataSync`, …). Push tests verify what Push calls on the facade; Core's `RetenoInternalImplTest` verifies what the facade does with it. Push and FCM tests are all Robolectric (JUnit 4) — don't mix plain JUnit 5 tests of the same classes in, see the sandbox rule above.
- `ServiceLocator` owns `ActionsLocator` (`serviceLocator.actions`). Legacy repositories/controllers that must trigger a sync take plain function types (`requestSync: () -> Unit`, `syncContactNow: suspend () -> Unit`), never internal types, because several of those legacy classes are `public`.
- Everything persisted as a remote log event goes through `Redact.uploadable` / `Redact.uploadableStackTrace` inside `Logger` (values after `=` and URLs/hosts are dropped). Don't bypass `Logger` to create `RetenoLogEvent`s.

After finishing a feature: self code-review, fix findings, and add any new non-obvious learning to this file.
