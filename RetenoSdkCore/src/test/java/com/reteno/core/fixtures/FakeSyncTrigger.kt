package com.reteno.core.fixtures

import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.core.internal.action.sync.SyncTrigger

internal class FakeSyncTrigger(var result: SyncResult = SyncResult.COMPLETED) : SyncTrigger {
    var requests = 0
        private set
    val immediateScopes = mutableListOf<SyncScope>()

    override fun request() {
        requests++
    }

    override suspend fun syncNow(scope: SyncScope): SyncResult {
        immediateScopes += scope
        return result
    }
}
