package com.reteno.core.internal.action.device

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.stepOrNull
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.AppEnvironment
import kotlinx.coroutines.withContext

internal class CurrentDevice(
    private val configRepository: ConfigRepository,
    private val devicesDb: RetenoDatabaseManagerDevice,
    private val environment: AppEnvironment,
    private val dispatchers: RetenoDispatchers
) {

    suspend fun save(trace: ActionTrace, pushToken: String, pushSubscribed: Boolean? = null): Boolean {
        val subscribed = pushSubscribed ?: trace.readNotificationsState()
        return trace.step(
            "save current device to local database",
            detail = { saved -> if (saved) "saved" else "unchanged" }
        ) {
            val device = environment.createDevice(configRepository.getDeviceId(), pushToken, subscribed).toDb()
            withContext(dispatchers.io) {
                val known = devicesDb.getDevices().map { it.withoutLocalState() }
                val isNew = device !in known
                if (isNew) devicesDb.insertDevice(device)
                isNew
            }
        }
    }

    private suspend fun ActionTrace.readNotificationsState(): Boolean =
        stepOrNull<Boolean>("read notifications state from the system", detail = { "enabled=$it" }) {
            environment.notificationsEnabled(defaultChannelId(configRepository.getDefaultNotificationChannel()))
        } ?: configRepository.isNotificationsEnabled()

    private fun defaultChannelId(storedChannel: String): String {
        val id = storedChannelId(storedChannel)
        return if (id.isNullOrBlank()) BUNDLED_DEFAULT_CHANNEL_ID else id
    }

    private fun storedChannelId(storedChannel: String): String? {
        val json = try {
            JsonParser.parseString(storedChannel)
        } catch (error: JsonParseException) {
            return null
        }
        if (!json.isJsonObject) return null
        val id = json.asJsonObject.get(KEY_CHANNEL_ID) ?: return null
        return if (id.isJsonPrimitive) id.asString else null
    }

    private fun DeviceDb.withoutLocalState(): DeviceDb =
        copy(rowId = null, createdAt = 0L, isSynchronizedWithBackend = null)
}

private const val BUNDLED_DEFAULT_CHANNEL_ID = "defaultId"
private const val KEY_CHANNEL_ID = "id"
