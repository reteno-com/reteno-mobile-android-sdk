package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal interface ActionTrace {
    suspend fun <T> step(name: String, detail: (T) -> String = { "" }, block: suspend () -> T): T

    fun note(message: () -> String)
}

internal class ActionTraceElement(val trace: ActionTraceImpl) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ActionTraceElement>
}

internal class ActionTraceImpl(
    val id: String,
    val depth: Int,
    private val name: String,
    private val sink: RetenoLogSink,
    private val clock: MonotonicClock
) : ActionTrace {

    private val startedAt = clock.nanos()
    private val childCounter = AtomicInteger(0)

    @Volatile
    private var lastStepFailure: Pair<String, Throwable>? = null

    fun nextChildId(): String = "$id.${childCounter.incrementAndGet()}"

    fun failedStepOf(error: Throwable): String? {
        val (step, stepError) = lastStepFailure ?: return null
        val causes = generateSequence(error) { it.cause }.take(MAX_CAUSE_DEPTH)
        return step.takeIf { causes.any { it === stepError } }
    }

    fun start(args: () -> String) = emit(LogPriority.DEBUG) {
        TraceFormatter.start(depth, id, name, args())
    }

    override suspend fun <T> step(name: String, detail: (T) -> String, block: suspend () -> T): T {
        val stepStartedAt = clock.nanos()
        val value = try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            lastStepFailure = name to error
            emit(LogPriority.ERROR) {
                TraceFormatter.stepFailure(depth, id, name, Redact.summary(error), millisSince(stepStartedAt))
            }
            throw error
        }
        emit(LogPriority.DEBUG) {
            TraceFormatter.stepSuccess(depth, id, name, detail(value), millisSince(stepStartedAt))
        }
        return value
    }

    override fun note(message: () -> String) = emit(LogPriority.DEBUG) {
        TraceFormatter.note(depth, id, message())
    }

    fun finishSuccess(label: String) = emit(LogPriority.DEBUG) {
        TraceFormatter.end(depth, id, name, "✓ $label", millisSince(startedAt))
    }

    fun finishCancelled() = emit(LogPriority.DEBUG) {
        TraceFormatter.end(depth, id, name, "⊘ CANCELLED", millisSince(startedAt))
    }

    fun finishFailure(error: Throwable) {
        emit(LogPriority.ERROR) {
            TraceFormatter.end(depth, id, name, "✗ FAILURE ${Redact.summary(error)}", millisSince(startedAt))
        }
        emit(LogPriority.ERROR) { Redact.stackTrace(error) }
    }

    private fun millisSince(start: Long): Long = (clock.nanos() - start) / NANOS_IN_MILLI

    private fun emit(priority: LogPriority, line: () -> String) = sink.log(priority, TAG, line)

    companion object {
        const val TAG = "RetenoAction"
        private const val NANOS_IN_MILLI = 1_000_000L
        private const val MAX_CAUSE_DEPTH = 8
    }
}
