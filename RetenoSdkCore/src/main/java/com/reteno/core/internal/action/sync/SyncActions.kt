package com.reteno.core.internal.action.sync

internal interface SyncActions {
    val backgroundSync: BackgroundSync

    fun requestSync()
}
