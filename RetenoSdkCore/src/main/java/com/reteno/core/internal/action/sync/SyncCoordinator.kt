package com.reteno.core.internal.action.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal interface SyncTrigger {
    fun request()

    suspend fun syncNow(scope: SyncScope = SyncScope.ALL): SyncResult
}

internal class SyncCoordinator(
    private val syncData: SyncDataAction,
    scope: CoroutineScope,
    private val awaitReady: suspend () -> Unit
) : SyncTrigger {

    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            while (true) {
                requests.receive()
                awaitReady()
                syncNow(SyncScope.ALL)
            }
        }
    }

    override fun request() {
        requests.trySend(Unit)
    }

    override suspend fun syncNow(scope: SyncScope): SyncResult =
        syncData(scope).getOrDefault(SyncResult.ABORTED)
}
