package com.reteno.core.data.local.config

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.internal.platform.DeviceIdSources
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

internal class DeviceIdHelper(
    private val sources: DeviceIdSources,
    private val sharedPrefsManager: SharedPrefsManager,
    private val retenoConfigProvider: () -> RetenoConfig
) {

    internal suspend fun withDeviceIdMode(currentDeviceId: DeviceId, deviceIdMode: DeviceIdMode): DeviceId {
        val idBody = idBodyFor(deviceIdMode) ?: return withDeviceIdMode(currentDeviceId, DeviceIdMode.RANDOM_UUID)
        return currentDeviceId.copy(idBody = idBody, mode = deviceIdMode)
    }

    private suspend fun idBodyFor(deviceIdMode: DeviceIdMode): String? = when (deviceIdMode) {
        DeviceIdMode.ANDROID_ID -> attempt { sources.androidId() }
        DeviceIdMode.APP_SET_ID -> attempt { sources.appSetId() }
        DeviceIdMode.RANDOM_UUID -> sharedPrefsManager.getDeviceIdUuid()
        DeviceIdMode.CLIENT_UUID -> clientDeviceId()
        DeviceIdMode.CACHE -> DeviceId.getIdBodyFrom(sharedPrefsManager.getLastStoredId())
    }

    private suspend fun clientDeviceId(): String? {
        val provider = retenoConfigProvider().userIdProvider ?: return null
        return attempt { awaitDeviceId(provider) }
    }

    private suspend fun awaitDeviceId(provider: DeviceIdProvider): String {
        while (true) {
            val deviceId = provider.getDeviceId()
            if (deviceId != null) return deviceId
            delay(CLIENT_ID_POLL_MILLIS)
        }
    }

    private suspend fun attempt(read: suspend () -> String?): String? = try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failed: Exception) {
        null
    }

    internal companion object {
        const val CLIENT_ID_POLL_MILLIS = 60L
    }
}
