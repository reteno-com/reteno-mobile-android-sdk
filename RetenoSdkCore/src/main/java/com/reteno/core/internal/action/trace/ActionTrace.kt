package com.reteno.core.internal.action.trace

import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal interface ActionTrace {
    fun args(vararg values: Pair<String, Any?>)

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
    private val output: RetenoLogSink,
    private val clock: MonotonicClock,
    collapseOnSuccess: Boolean
) : ActionTrace {

    private val buffer = if (collapseOnSuccess) TraceBuffer(output) else null
    val sink: RetenoLogSink = buffer ?: output
    private val startedAt = clock.nanos()
    private val childCounter = AtomicInteger(0)
    private val started = AtomicBoolean(false)

    @Volatile
    private var startArgs = ""

    @Volatile
    private var lastStepFailure: Pair<String, List<Throwable>>? = null

    fun nextChildId(): String = "$id.${childCounter.incrementAndGet()}"

    fun failedStepOf(error: Throwable): String? {
        val (step, stepCauses) = lastStepFailure ?: return null
        val sharesCause = causeChain(error).any { cause -> stepCauses.any { it === cause } }
        return step.takeIf { sharesCause }
    }

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            emit(LogPriority.DEBUG) { TraceFormatter.start(depth, id, name, "") }
        }
    }

    override fun args(vararg values: Pair<String, Any?>) {
        if (started.compareAndSet(false, true)) {
            emit(LogPriority.DEBUG) {
                startArgs = format(values)
                TraceFormatter.start(depth, id, name, startArgs)
            }
        } else {
            emit(LogPriority.DEBUG) { TraceFormatter.note(depth, id, format(values)) }
        }
    }

    override suspend fun <T> step(name: String, detail: (T) -> String, block: suspend () -> T): T {
        ensureStarted()
        val stepStartedAt = clock.nanos()
        val value = try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            lastStepFailure = name to causeChain(error)
            emit(LogPriority.ERROR) {
                TraceFormatter.stepFailure(depth, id, name, Redact.summary(error), millisSince(stepStartedAt))
            }
            throw error
        }
        val traced = value as? TracedOutcome
        val text = { detail(value).ifEmpty { traced?.traceLabel.orEmpty() } }
        if (traced == null || traced.isSuccessful) {
            emit(LogPriority.DEBUG) { TraceFormatter.stepSuccess(depth, id, name, text(), millisSince(stepStartedAt)) }
        } else {
            emit(LogPriority.WARN) { TraceFormatter.stepFailure(depth, id, name, text(), millisSince(stepStartedAt)) }
        }
        return value
    }

    override fun note(message: () -> String) {
        ensureStarted()
        emit(LogPriority.DEBUG) { TraceFormatter.note(depth, id, message()) }
    }

    fun finishSuccess(value: Any?) {
        ensureStarted()
        val traced = value as? TracedOutcome
        if (traced == null || traced.isSuccessful) {
            val label = if (traced == null) SUCCESS else traced.traceLabel
            if (buffer != null && buffer.isQuiet) {
                output.log(LogPriority.DEBUG, TAG) {
                    TraceFormatter.collapsed(depth, id, name, startArgs, "✓ $label", millisSince(startedAt))
                }
                return
            }
            emit(LogPriority.DEBUG) { TraceFormatter.end(depth, id, name, "✓ $label", millisSince(startedAt)) }
        } else {
            emit(LogPriority.WARN) {
                TraceFormatter.end(depth, id, name, "✗ ${traced.traceLabel}", millisSince(startedAt))
            }
        }
        flushBuffer()
    }

    fun finishCancelled() {
        ensureStarted()
        emit(LogPriority.DEBUG) { TraceFormatter.end(depth, id, name, "⊘ CANCELLED", millisSince(startedAt)) }
        flushBuffer()
    }

    fun finishFailure(error: Throwable) {
        ensureStarted()
        emit(LogPriority.ERROR) {
            TraceFormatter.end(depth, id, name, "✗ FAILURE ${Redact.summary(error)}", millisSince(startedAt))
        }
        emit(LogPriority.ERROR) { Redact.stackTrace(error) }
        flushBuffer()
    }

    private fun flushBuffer() {
        buffer?.flush()
    }

    private fun format(values: Array<out Pair<String, Any?>>): String =
        values.joinToString(ARGS_SEPARATOR) { (key, value) -> "$key=${resolve(value)}" }

    private fun resolve(value: Any?): Any? = if (value is Function0<*>) value() else value

    private fun causeChain(error: Throwable): List<Throwable> =
        generateSequence(error) { it.cause }.take(MAX_CAUSE_DEPTH).toList()

    private fun millisSince(start: Long): Long = (clock.nanos() - start) / NANOS_IN_MILLI

    private fun emit(priority: LogPriority, line: () -> String) = sink.log(priority, TAG, line)

    companion object {
        const val TAG = "RetenoAction"
        private const val NANOS_IN_MILLI = 1_000_000L
        private const val MAX_CAUSE_DEPTH = 8
        private const val ARGS_SEPARATOR = "  "
        private const val SUCCESS = "SUCCESS"
    }
}
