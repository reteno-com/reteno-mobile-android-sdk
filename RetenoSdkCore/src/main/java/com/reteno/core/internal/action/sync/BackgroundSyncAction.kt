package com.reteno.core.internal.action.sync

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal enum class BackgroundSyncOutcome(override val isSuccessful: Boolean) : TracedOutcome {
    SKIPPED_IN_FOREGROUND(true),
    NOT_INITIALIZED(false),
    NO_DATA(true),
    SYNCED(true),
    ABORTED(false)
}

internal class BackgroundSyncAction(
    private val isForeground: () -> Boolean,
    private val isInitialized: () -> Boolean,
    private val hasDataForSync: () -> Boolean,
    private val sync: SyncTrigger,
    private val cancelPeriodicWork: () -> Unit,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(): Result<BackgroundSyncOutcome> = runner("BackgroundSync") {
        syncInBackground()
    }

    private suspend fun ActionTrace.syncInBackground(): BackgroundSyncOutcome {
        if (step("check whether app is in foreground", detail = { "foreground=$it" }) { isForeground() }) {
            return BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND
        }
        if (!isInitialized()) {
            note { "sdk is not initialized yet" }
            return BackgroundSyncOutcome.NOT_INITIALIZED
        }
        val hasData = step("check local database for unsent data", detail = { "hasData=$it" }) {
            withContext(dispatchers.io) { hasDataForSync() }
        }
        if (!hasData) {
            step("cancel periodic background sync") { cancelPeriodicWork() }
            return BackgroundSyncOutcome.NO_DATA
        }
        return when (sync.syncNow(SyncScope.ALL)) {
            SyncResult.COMPLETED -> BackgroundSyncOutcome.SYNCED
            SyncResult.ABORTED,
            SyncResult.INCOMPLETE -> BackgroundSyncOutcome.ABORTED
        }
    }
}
