package com.reteno.core.internal.action.device

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.Redact
import kotlinx.coroutines.withContext

internal interface EnsureDeviceRegisteredThisSession {
    suspend operator fun invoke(): Result<Unit>
}

internal class EnsureDeviceRegisteredThisSessionAction(
    private val configRepository: ConfigRepository,
    private val devicesDb: RetenoDatabaseManagerDevice,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val session: ContactSession,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : EnsureDeviceRegisteredThisSession {

    override suspend operator fun invoke(): Result<Unit> = runner("EnsureDeviceRegisteredThisSession") {
        ensure()
    }

    private suspend fun ActionTrace.ensure() {
        if (session.isDeviceSent) {
            note { "device already sent this session" }
            return
        }
        step("forget synced devices in local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) {
                val synced = devicesDb.getDevices().filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }
                if (synced.isNotEmpty()) devicesDb.deleteDevices(synced)
                synced.size
            }
        }
        val token = step("read push token", detail = { Redact.token(it) }) {
            configRepository.getFcmToken()
        }
        currentDevice.save(this, token)
        step("request data sync") {
            sync.request()
        }
        session.markDeviceSent()
    }
}
