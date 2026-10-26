package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.RetenoLogSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.util.UUID

internal class ActionRunner(
    private val sink: RetenoLogSink,
    private val failureReporter: ActionFailureReporter,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val newRootId: () -> String = { UUID.randomUUID().toString().take(ROOT_ID_LENGTH) }
) {

    suspend operator fun <T> invoke(
        name: String,
        reportFailure: Boolean = true,
        collapseOnSuccess: Boolean = false,
        block: suspend ActionTrace.() -> T
    ): Result<T> {
        val parent = currentCoroutineContext()[ActionTraceElement]?.trace
        parent?.ensureStarted()
        val trace = if (parent == null) {
            ActionTraceImpl(newRootId(), depth = 0, name, sink, clock, collapseOnSuccess)
        } else {
            ActionTraceImpl(parent.nextChildId(), depth = parent.depth + 1, name, parent.sink, clock, collapseOnSuccess)
        }
        return try {
            val value = withContext(ActionTraceElement(trace)) { trace.block() }
            trace.finishSuccess(value)
            Result.success(value)
        } catch (cancellation: CancellationException) {
            trace.finishCancelled()
            throw cancellation
        } catch (error: Throwable) {
            trace.finishFailure(error)
            if (reportFailure) {
                failureReporter.report(name, trace.failedStepOf(error), error)
            }
            Result.failure(error)
        }
    }

    private companion object {
        const val ROOT_ID_LENGTH = 6
    }
}
