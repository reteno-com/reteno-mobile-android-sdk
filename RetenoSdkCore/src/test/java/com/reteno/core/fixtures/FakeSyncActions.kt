package com.reteno.core.fixtures

import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.SyncActions

internal class FakeSyncActions(private val log: FakeCallLog = FakeCallLog()) : SyncActions {
    var backgroundSyncAnswer: BackgroundSync? = null

    override val backgroundSync: BackgroundSync
        get() = checkNotNull(backgroundSyncAnswer) { "background sync is not part of facade tests" }

    override fun requestSync() {
        log.record("requestSync")
    }
}
