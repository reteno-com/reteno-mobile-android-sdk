package com.reteno.core.di.route

import com.reteno.core.di.graph.SyncGraph
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.SyncActions

internal class SyncRouter(private val sync: SyncGraph) : SyncActions {

    override val backgroundSync: BackgroundSync
        get() = sync.backgroundSync

    override fun requestSync() {
        sync.dataSync.request()
    }
}
