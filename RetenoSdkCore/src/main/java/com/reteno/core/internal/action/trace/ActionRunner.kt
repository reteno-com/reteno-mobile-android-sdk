package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.RetenoLogSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.coroutineContext

internal class ActionRunner(
    private val sink: RetenoLogSink,
    private val failureReporter: ActionFailureReporter,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val newRootId: () -> String = { UUID.randomUUID().toString().take(ROOT_ID_LENGTH) }
) {

    suspend fun <T> run(
        name: String,
        args: () -> String = { "" },
        outcome: (T) -> String = { SUCCESS },
        block: suspend ActionTrace.() -> T
    ): Result<T> {
        val parent = coroutineContext[ActionTraceElement]?.trace
        val trace = if (parent == null) {
            ActionTraceImpl(newRootId(), depth = 0, name, sink, clock)
        } else {
            ActionTraceImpl(parent.nextChildId(), depth = parent.depth + 1, name, sink, clock)
        }
        trace.start(args)
        return try {
            val value = withContext(ActionTraceElement(trace)) { trace.block() }
            trace.finishSuccess(outcome(value))
            Result.success(value)
        } catch (cancellation: CancellationException) {
            trace.finishCancelled()
            throw cancellation
        } catch (error: Throwable) {
            trace.finishFailure(error)
            failureReporter.report(name, trace.failedStepOf(error), error)
            Result.failure(error)
        }
    }

    private companion object {
        const val SUCCESS = "SUCCESS"
        const val ROOT_ID_LENGTH = 6
    }
}
