package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.TracedOutcome
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.lifecycle.RetenoActivityHelper
import kotlinx.coroutines.withContext

internal enum class BackgroundSyncOutcome(override val isSuccessful: Boolean) : TracedOutcome {
    SKIPPED_IN_FOREGROUND(true),
    NOT_INITIALIZED(false),
    NO_DATA(true),
    SYNCED(true),
    ABORTED(false)
}

internal interface BackgroundSync {
    suspend operator fun invoke(): Result<BackgroundSyncOutcome>
}

internal class BackgroundSyncAction(
    private val activityHelper: RetenoActivityHelper,
    private val initGate: InitGate,
    private val databaseManager: RetenoDatabaseManager,
    private val sync: SyncTrigger,
    private val platform: AppPlatform,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : BackgroundSync {

    override suspend operator fun invoke(): Result<BackgroundSyncOutcome> = runner("BackgroundSync") {
        syncInBackground()
    }

    private suspend fun ActionTrace.syncInBackground(): BackgroundSyncOutcome {
        if (step("check whether app is in foreground", detail = { "foreground=$it" }) { activityHelper.canPresentMessages() }) {
            return BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND
        }
        if (!initGate.isInitialized) {
            note { "sdk is not initialized yet" }
            return BackgroundSyncOutcome.NOT_INITIALIZED
        }
        val hasData = step("check local database for unsent data", detail = { "hasData=$it" }) {
            withContext(dispatchers.io) { databaseManager.hasDataForSync() }
        }
        if (!hasData) {
            step("cancel periodic background sync") { platform.cancelBackgroundSync() }
            return BackgroundSyncOutcome.NO_DATA
        }
        return when (sync.syncNow(SyncScope.ALL)) {
            SyncResult.COMPLETED -> BackgroundSyncOutcome.SYNCED
            SyncResult.ABORTED,
            SyncResult.INCOMPLETE -> BackgroundSyncOutcome.ABORTED
        }
    }
}
