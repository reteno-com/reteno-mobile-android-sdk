package com.reteno.core.internal.action.device

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.log.Redact

internal class UseDeviceLanguageAction(
    private val configRepository: ConfigRepository,
    private val deviceLanguage: DeviceLanguage,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(): Result<Unit> = runner("UseDeviceLanguage") {
        returnToDetectedLanguage()
    }

    private suspend fun ActionTrace.returnToDetectedLanguage() {
        val forgotten = deviceLanguage.useDetected(this)
        if (!forgotten) {
            note { "no explicit device language set" }
            return
        }
        val token = step("read push token", detail = { Redact.token(it) }) {
            configRepository.getFcmToken()
        }
        currentDevice.save(this, token)
        step("request data sync") {
            sync.request()
        }
    }
}
