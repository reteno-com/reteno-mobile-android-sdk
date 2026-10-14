package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toDb
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.user.UserRemote
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.util.Util
import kotlinx.coroutines.withContext

internal class PushUserStep(
    private val usersDb: RetenoDatabaseManagerUser,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushUser") {
        push()
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        val users = step("load users from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { usersDb.getUsers() }
        }
        val unsynced = users.filter { it.isSynchronizedWithBackend != BooleanDb.TRUE }
        if (unsynced.isEmpty()) return nothingToSend()
        val synced = users.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }.maxByOrNull { it.createdAt }
        val push = pushFor(unsynced, synced)
        if (push == null) {
            step("delete users without changes from local database") {
                withContext(dispatchers.io) { usersDb.deleteUsers(unsynced) }
            }
            return StepOutcome.CONTINUE
        }
        val result = step("send user to server") {
            api.request(HttpMethod.POST, ApiContract.MobileApi.User, push.request.toJson())
        }
        if (result is ApiResult.Failure && result.isRepeatable) return StepOutcome.ABORT
        step("mark user as synced in local database") {
            withContext(dispatchers.io) {
                usersDb.deleteUsers(users)
                usersDb.insertUser(push.full.toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE))
            }
        }
        return StepOutcome.CONTINUE
    }

    private fun pushFor(unsynced: List<UserDb>, synced: UserDb?): UserPush? {
        val ordered = unsynced.sortedBy { it.createdAt }
        val merged = ordered.map { it.toRemote() }.reduce { accumulated, next -> next.createAccModel(accumulated) }
        if (synced == null || Util.isTimestampOutdated(synced.createdAt, ordered.first().createdAt)) {
            return UserPush(request = merged, full = merged)
        }
        val syncedRemote = synced.toRemote()
        val diff = merged.createDiffModel(syncedRemote) ?: return null
        return UserPush(request = diff, full = merged.createAccModel(syncedRemote))
    }

    private class UserPush(val request: UserRemote, val full: UserRemote)
}
