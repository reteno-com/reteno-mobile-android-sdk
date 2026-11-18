package com.reteno.core.internal.action.device

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.Redact
import kotlinx.coroutines.withContext

internal interface RefreshDeviceLanguage {
    suspend operator fun invoke(): Result<Unit>
}

internal class RefreshDeviceLanguageAction(
    private val configRepository: ConfigRepository,
    private val devicesDb: RetenoDatabaseManagerDevice,
    private val deviceLanguage: DeviceLanguage,
    private val currentDevice: CurrentDevice,
    private val sync: SyncTrigger,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : RefreshDeviceLanguage {

    override suspend operator fun invoke(): Result<Unit> = runner("RefreshDeviceLanguage", collapseOnSuccess = true) {
        refresh()
    }

    private suspend fun ActionTrace.refresh() {
        val languageCode = deviceLanguage.resolve(this)
        val savedLanguageCode = step("read language of last saved device from local database") {
            withContext(dispatchers.io) { devicesDb.getDevices().maxByOrNull(::insertionOrder)?.languageCode }
        }
        if (savedLanguageCode == languageCode) {
            note { "device language unchanged" }
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

private fun insertionOrder(device: DeviceDb): Long {
    val rowId = device.rowId ?: return 0L
    return rowId.toLong()
}
