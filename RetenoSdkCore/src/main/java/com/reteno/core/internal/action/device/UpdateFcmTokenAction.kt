package com.reteno.core.internal.action.device

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.log.Redact

internal class UpdateFcmTokenAction(
    private val configRepository: ConfigRepository,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val session: ContactSession,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(token: String): Result<Unit> = runner("UpdateFcmToken") {
        args("token" to { Redact.token(token) })
        update(token)
    }

    private suspend fun ActionTrace.update(token: String) {
        val stored = step("read stored push token", detail = { Redact.token(it) }) {
            configRepository.getStoredFcmToken()
        }
        if (token == stored) {
            note { "push token unchanged" }
            return
        }
        step("save push token to local settings") {
            configRepository.saveFcmToken(token)
        }
        currentDevice.save(this, token)
        step("request data sync") {
            sync.request()
        }
        session.markDeviceSent()
    }
}
