package com.reteno.core.fixtures

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.TraceFormatter
import kotlinx.coroutines.test.TestScope

internal data class FailureReport(
    val action: String,
    val step: String?,
    val errorType: Class<*>,
    val message: String?
) {
    constructor(action: String, step: String?, error: Throwable) : this(action, step, error.javaClass, error.message)
}

internal class TestActionRunner(scope: TestScope, enabled: Boolean = true) {
    private var nextRoot = 0
    val sink = FakeLogSink(isEnabled = enabled)
    val reports = mutableListOf<FailureReport>()
    val runner = ActionRunner(
        sink = sink,
        failureReporter = { action, step, error -> reports += FailureReport(action, step, error) },
        clock = scope.virtualClock(),
        newRootId = { "r${++nextRoot}" }
    )

    fun successfulTrace(name: String, args: String, vararg steps: String): List<String> =
        listOf(TraceFormatter.start(0, "r1", name, args)) +
            steps.map { TraceFormatter.stepSuccess(0, "r1", it, "", 0) } +
            TraceFormatter.end(0, "r1", name, "✓ SUCCESS", 0)
}
