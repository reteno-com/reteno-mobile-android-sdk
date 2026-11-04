package com.reteno.core.internal.lifecycle

import com.reteno.core.internal.action.init.StartSdk
import com.reteno.core.internal.action.init.StopSdk
import com.reteno.core.internal.coroutine.InitGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class StartStopSequencer(
    private val scope: CoroutineScope,
    private val initGate: InitGate,
    private val startSdk: StartSdk,
    private val stopSdk: StopSdk,
    private val state: SdkRunState
) : SdkLifecycle {

    private var last: Job? = null
    private var pendingStart: Job? = null

    override val isStarted: Boolean
        get() = state.isStarted

    @Synchronized
    override fun start() {
        val job = after(last) { startSdk() }
        pendingStart = job
        last = job
    }

    @Synchronized
    override fun stop() {
        val previous = last
        pendingStart?.cancel()
        pendingStart = null
        last = after(previous) { stopSdk() }
    }

    private fun after(previous: Job?, block: suspend () -> Unit): Job =
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            previous?.join()
            initGate.awaitUserInit()
            block()
        }
}
