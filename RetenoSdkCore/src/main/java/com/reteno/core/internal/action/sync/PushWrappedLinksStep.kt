package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class PushWrappedLinksStep(
    private val wrappedLinksDb: RetenoDatabaseManagerWrappedLink,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushWrappedLinks") {
        push()
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        repeat(MAX_LINKS) { handled ->
            val link = step("load link click from local database") {
                withContext(dispatchers.io) { wrappedLinksDb.getWrappedLinks(1).firstOrNull() }
            } ?: return finishedAfter(handled)
            if (link.startsWith(SECURE_SCHEME)) {
                val result = step("send link click to tracking server") {
                    api.request(HttpMethod.HEAD, ApiContract.Custom(link))
                }
                if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
            } else {
                note { "link is not https, dropping it" }
            }
            step("delete link click from local database") {
                withContext(dispatchers.io) { wrappedLinksDb.deleteWrappedLinks(1) }
            }
        }
        return StepOutcome.CONTINUE
    }

    companion object {
        const val MAX_LINKS = 50
        private const val SECURE_SCHEME = "https://"
    }
}
