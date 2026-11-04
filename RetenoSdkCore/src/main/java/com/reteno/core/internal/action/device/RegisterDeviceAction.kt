package com.reteno.core.internal.action.device

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.log.Redact

internal interface RegisterDevice {
    suspend operator fun invoke(): Result<SyncResult>
}

internal class RegisterDeviceAction(
    private val configRepository: ConfigRepository,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val session: ContactSession,
    private val runner: ActionRunner
) : RegisterDevice {

    override suspend operator fun invoke(): Result<SyncResult> = runner("RegisterDevice") {
        val token = step("read push token", detail = { Redact.token(it) }) {
            configRepository.getFcmToken()
        }
        currentDevice.save(this, token)
        val result = step("send device and user to server") {
            sync.syncNow(SyncScope.CONTACT)
        }
        session.markDeviceSent()
        result
    }
}
