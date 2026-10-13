package com.reteno.core.internal.action.device

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.log.Redact

internal class UpdateNotificationsEnabledAction(
    private val configRepository: ConfigRepository,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val session: ContactSession,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(enabled: Boolean): Result<Unit> = runner("UpdateNotificationsEnabled") {
        args("enabled" to enabled)
        update(enabled)
    }

    private suspend fun ActionTrace.update(enabled: Boolean) {
        val stored = step("read stored notifications state", detail = { "enabled=$it" }) { configRepository.isNotificationsEnabled() }
        if (enabled == stored) {
            note { "notifications state unchanged" }
            return
        }
        session.markDeviceSent()
        step("save notifications state to local settings") { configRepository.saveNotificationsEnabled(enabled) }
        val token = step("read push token", detail = { Redact.token(it) }) { configRepository.getFcmToken() }
        currentDevice.save(this, token, pushSubscribed = enabled)
        step("request data sync") { sync.request() }
    }
}
