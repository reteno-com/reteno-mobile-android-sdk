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

    override val name: String = "PushUser"

    override suspend fun invoke(): Result<StepOutcome> = runner(name) {
        val first = pushRound()
        if (first == RoundOutcome.NOTHING_PENDING) {
            nothingToSend()
        } else {
            pushRemainingRounds(first)
        }
    }

    private suspend fun ActionTrace.pushRemainingRounds(first: RoundOutcome): StepOutcome {
        var outcome = first
        var round = 1
        while (outcome == RoundOutcome.MORE_PENDING && round < MAX_ROUNDS) {
            outcome = pushRound()
            round++
        }
        return if (outcome == RoundOutcome.ABORT) StepOutcome.ABORT else StepOutcome.CONTINUE
    }

    private suspend fun ActionTrace.pushRound(): RoundOutcome {
        val users = step("load users from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { usersDb.getUsers() }
        }
        val pending = users.filter { it.isSynchronizedWithBackend != BooleanDb.TRUE }.sortedBy { it.createdAt }
        if (pending.isEmpty()) return RoundOutcome.NOTHING_PENDING
        val run = pending.takeWhile { it.isSameContactAs(pending.first()) }
        val syncedRows = users.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }
        val push = pushFor(run, syncedRows.maxByOrNull { it.createdAt })
        if (push == null) {
            step("delete users without changes from local database") {
                withContext(dispatchers.io) { usersDb.deleteUsers(run) }
            }
            return nextRound(pending, run)
        }
        val result = step("send user to server") {
            api.request(HttpMethod.POST, ApiContract.MobileApi.User, push.request.toJson())
        }
        if (result is ApiResult.Failure) {
            if (result.isRepeatable) return RoundOutcome.ABORT
            step("drop user rejected by server from local database") {
                withContext(dispatchers.io) { usersDb.deleteUsers(run) }
            }
            return nextRound(pending, run)
        }
        step("mark user as synced in local database") {
            withContext(dispatchers.io) {
                usersDb.deleteUsers(syncedRows + run)
                usersDb.insertUser(push.full.toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE))
            }
        }
        return nextRound(pending, run)
    }

    private fun nextRound(pending: List<UserDb>, sent: List<UserDb>): RoundOutcome =
        if (pending.size > sent.size) RoundOutcome.MORE_PENDING else RoundOutcome.DONE

    private fun pushFor(run: List<UserDb>, synced: UserDb?): UserPush? {
        val merged = run.map { it.toRemote() }.reduce { accumulated, next -> next.createAccModel(accumulated) }
        if (synced == null || !synced.isSameContactAs(run.first()) || isOutdated(synced, run.first())) {
            return UserPush(request = merged, full = merged)
        }
        val syncedRemote = synced.toRemote()
        val diff = merged.createDiffModel(syncedRemote) ?: return null
        return UserPush(request = diff, full = merged.createAccModel(syncedRemote))
    }

    private fun UserDb.isSameContactAs(other: UserDb): Boolean =
        deviceId == other.deviceId && externalUserId == other.externalUserId

    private fun isOutdated(synced: UserDb, oldestPending: UserDb): Boolean =
        Util.isTimestampOutdated(synced.createdAt, oldestPending.createdAt)

    private class UserPush(val request: UserRemote, val full: UserRemote)

    private enum class RoundOutcome { DONE, MORE_PENDING, NOTHING_PENDING, ABORT }

    companion object {
        const val MAX_ROUNDS = 5
    }
}
