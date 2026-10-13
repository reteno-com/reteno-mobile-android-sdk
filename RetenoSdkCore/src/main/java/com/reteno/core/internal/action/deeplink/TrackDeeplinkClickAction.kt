package com.reteno.core.internal.action.deeplink

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class TrackDeeplinkClickAction(
    private val wrappedLinksDb: RetenoDatabaseManagerWrappedLink,
    private val sync: SyncTrigger,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(wrappedLink: String): Result<Unit> = runner("TrackDeeplinkClick") { track(wrappedLink) }

    private suspend fun ActionTrace.track(wrappedLink: String) {
        if (wrappedLink.isBlank()) {
            note { "no wrapped link, nothing to track" }
            return
        }
        step("save link click to local database") { withContext(dispatchers.io) { wrappedLinksDb.insertWrappedLink(wrappedLink) } }
        step("request data sync") { sync.request() }
    }
}
