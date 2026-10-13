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
        step("save event to local database") { withContext(dispatchers.io) { eventsDb.insertEvents(EventsDb(restConfig.deviceId, event.toDb())) } }
        step("notify in-app message triggers") { eventBus.emit(event) }
    }
}
```

- One file under `internal/action/<feature>/`; `suspend operator fun invoke(...): Result<T> = runner("Name") { ... }` on one line.
- Arguments go first inside the block: `args("type" to event.eventTypeKey)`. The start line is printed at `args(...)` (or before the first step/note/child/end if there are none); `args` called later prints a `→ key=value` note. Values are formatted only when logging is on; pass `"key" to { expensive() }` for values that are costly or need `Redact`.
- Every unit of work is a named `step(...)`. A step name says **what happens and where**: verb + object + source/destination — `"save event to local database"`, `"load unsynced devices from local database"`, `"send device to server"`, `"notify in-app message triggers"`. Never a bare verb (`"insert event"`, `"emit"`).
- Server calls are named in plain words (`"send interactions to server"`). **Never put URLs, hosts, paths, headers or bodies in step names, args, details or notes.** Never derive a step name from `ApiContract.url`.
- Anything user-related in `args` / `detail` / `note` goes through `Redact` (`token`, `email`, `phone`, `fields`, `text`, `summary`, `stackTrace`). Never format PII yourself.
- `runner(...)` rethrows `CancellationException`, logs `✓ SUCCESS` / `✗ FAILURE` / `⊘ CANCELLED`, and reports exactly one remote log event per failed action (no args included). A child `runner(...)` inside a parent nests automatically (`[root.n]`).
- Validation the caller must see (blank `externalUserId`, invalid `marketId`) is done synchronously in the facade before deferring, not inside the Action.
- Host callbacks are invoked via `withContext(dispatchers.main)`.

## Logging

- Single logcat tag `RetenoAction` for action traces, `Reteno` for uncaught scope errors. Lines are capped at 1 000 chars; the sink splits messages above 4 000.
- Logging is on only when `BuildConfig.DEBUG || Util.isDebugView()` (`isLoggingEnabled()`); when off, no message lambda is evaluated.
- Legacy `RestClientImpl` logs only method, status and a redacted error summary. Keep it that way.

## Testing

Run: `./gradlew :RetenoSdkCore:testDebugUnitTest :RetenoSdkCore:koverVerifyDebug` (filter with `--tests 'com.reteno.core.internal.*'`). Fixture self-tests: `./gradlew :RetenoSdkTestFixtures:test`.

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

Coverage gate: Kover, **100 % line and branch** for the packages listed in `RetenoSdkCore/build.gradle` → `koverReport.androidReports('debug').filters.includes`. Add a package there when its phase is done. Never add exclusions; restructure code instead. **Kover is local only — never add Kover tasks to CI.** Run `koverVerifyDebug` locally before every PR and include the result in the Final Coverage Report.

## Gotchas learned so far

- **Long-running work in tests**: no `backgroundScope` on coroutines-test 1.6.4. Launch endless loops in `workScope()`; it is cancelled before `runUnitTest` returns, so `runTest` doesn't spin forever. Drive time with `runCurrent()` / `advanceTimeBy()`, never `advanceUntilIdle()` for such work.
- **Exception identity**: Gradle runs tests with assertions on, which enables coroutines stack-trace recovery — exceptions crossing a suspension point are re-created with the original as `cause`. Don't `assertSame` exceptions that crossed `withContext`/`launch`; assert type and message. Production code that matches exceptions must compare cause chains on both sides: an error thrown inside `withContext` within a step is copied at every boundary, and the copies only share the original as `cause` (see `ActionTraceImpl.failedStepOf`).
- **Unreachable Kotlin branches fail the 100 % branch gate**: `x?.let { nonNull } ?: y` and `a?.b?.c ?: d` with a non-null `b` compile to branches no test can reach. Use `val v = x ?: return y` or an explicit `if`.
- **`BuildConfig.DEBUG` is not a compile-time constant** (`Boolean.parseBoolean`), so `BuildConfig.DEBUG || x` has a branch the debug unit-test variant can't cover. Pass it as a defaulted parameter (`isLoggingEnabled(debugBuild = BuildConfig.DEBUG, …)`) and test the parameters.
- **Kover instrumentation breaks JUnit 3's `TestCase.assertEquals`** (AIOOBE) used by some legacy tests; `kover { excludeInstrumentation { packages('junit.framework', 'org.junit', 'org.robolectric', 'io.mockk') } }` is required in every module applying Kover.
- **`testFixtures` is a reserved Gradle dependency method** — the shared fixtures dependency group in `test_dependencies.gradle` is called `retenoTestFixtures()`.
- Kotlin modules can't see another module's `internal` declarations, so fakes of internal SDK types live in `RetenoSdkCore/src/test/.../fixtures`, not in `RetenoSdkTestFixtures`.
- Legacy `RetenoNotificationClickedReceiverTest.saveActionInteraction` fails depending on test order (an uncaught coroutine exception from an earlier test). It is replaced in Phase 5.

After finishing a feature: self code-review, fix findings, and add any new non-obvious learning to this file.
