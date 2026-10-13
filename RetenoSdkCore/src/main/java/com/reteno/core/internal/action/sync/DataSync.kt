package com.reteno.core.internal.action.sync

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DataSync(
    private val coordinator: SyncCoordinator,
    private val clearOldData: suspend () -> Unit,
    private val enqueueBackgroundSync: () -> Unit,
    private val isDebugView: () -> Boolean,
    private val jitter: () -> Long,
    private val scope: CoroutineScope,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncTrigger by coordinator {

    private var scheduler: Job? = null

    @Synchronized
    fun startScheduler() {
        scheduler?.cancel()
        val period = if (isDebugView()) DEBUG_VIEW_DELAY else REGULAR_DELAY
        scheduler = scope.launch {
            delay(period + jitter())
            while (true) {
                coordinator.request()
                delay(period)
            }
        }
        scope.launch {
            runner("EnqueueBackgroundSync") {
                step("schedule periodic background sync") {
                    withContext(dispatchers.io) { enqueueBackgroundSync() }
                }
            }
        }
    }

    @Synchronized
    fun stopScheduler() {
        scheduler?.cancel()
        scheduler = null
    }

    fun clearOldData() {
        scope.launch {
            delay(CLEAR_OLD_DATA_DELAY)
            clearOldData.invoke()
        }
    }

    companion object {
        const val REGULAR_DELAY = 30_000L
        const val DEBUG_VIEW_DELAY = 10_000L
        const val RANDOM_DELAY = 10_000L
        const val CLEAR_OLD_DATA_DELAY = 3_000L
    }
}
