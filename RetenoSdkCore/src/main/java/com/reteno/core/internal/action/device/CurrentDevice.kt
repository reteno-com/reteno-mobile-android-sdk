package com.reteno.core.internal.action.device

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.AppPlatform
import kotlinx.coroutines.withContext

internal class CurrentDevice(
    private val configRepository: ConfigRepository,
    private val devicesDb: RetenoDatabaseManagerDevice,
    private val platform: AppPlatform,
    private val dispatchers: RetenoDispatchers
) {

    suspend fun save(
        trace: ActionTrace,
        pushToken: String,
        pushSubscribed: Boolean = configRepository.isNotificationsEnabled()
    ): Boolean = trace.step("save current device to local database", detail = { saved -> if (saved) "saved" else "unchanged" }) {
        val device = platform.createDevice(configRepository.getDeviceId(), pushToken, pushSubscribed).toDb()
        withContext(dispatchers.io) {
            val known = devicesDb.getDevices().map { it.copy(rowId = null, createdAt = 0L, isSynchronizedWithBackend = null) }
            val isNew = device !in known
            if (isNew) devicesDb.insertDevice(device)
            isNew
        }
    }
}
