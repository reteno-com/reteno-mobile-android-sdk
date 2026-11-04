package com.reteno.core.internal.action.user

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.RemoteLog
import kotlinx.coroutines.withContext

internal class SetAnonymousUserAttributesAction(
    private val configRepository: ConfigRepository,
    private val usersDb: RetenoDatabaseManagerUser,
    private val sync: SyncTrigger,
    private val remoteLog: RemoteLog,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(
        attributes: UserAttributesAnonymous
    ): Result<Unit> = runner("SetAnonymousUserAttributes") {
        args("attributes" to { attributes.traceFields() })
        save(attributes)
    }

    private suspend fun ActionTrace.save(attributes: UserAttributesAnonymous) {
        val validated = Validator.validateAnonymousUserAttributes(attributes)
        if (validated == null) {
            step("report rejected attributes to remote log") {
                remoteLog.info("SetAnonymousUserAttributes: attributes rejected by validation")
            }
            return
        }
        step("save anonymous user to local database") {
            withContext(dispatchers.io) {
                val user = User(validated.toUserAttributes()).toDb(configRepository.getDeviceId())
                usersDb.insertUser(user)
            }
        }
        step("request data sync") {
            sync.request()
        }
    }
}
