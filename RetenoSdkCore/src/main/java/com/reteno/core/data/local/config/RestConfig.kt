package com.reteno.core.data.local.config

import com.reteno.core.data.local.sharedpref.SharedPrefsManager

internal interface DeviceIdentity {

    val deviceId: DeviceId

    suspend fun initDeviceId(deviceIdMode: DeviceIdMode)
}

internal class RestConfig(
    private val sharedPrefsManager: SharedPrefsManager,
    private val deviceIdHelper: DeviceIdHelper
) : DeviceIdentity {

    @Volatile
    override var deviceId: DeviceId = DeviceId("")
        private set

    override suspend fun initDeviceId(deviceIdMode: DeviceIdMode) {
        deviceId = deviceIdHelper.withDeviceIdMode(deviceId, deviceIdMode).copy(
            externalId = sharedPrefsManager.getExternalUserId(),
            idSuffix = sharedPrefsManager.getDeviceIdSuffix(),
            email = sharedPrefsManager.getEmail(),
            phone = sharedPrefsManager.getPhone()
        )
    }

    internal fun setDeviceIdSuffix(suffix: String?) {
        sharedPrefsManager.saveDeviceIdSuffix(suffix)
        deviceId = deviceId.copy(idSuffix = suffix)
    }

    internal fun setExternalUserId(externalUserId: String?) {
        sharedPrefsManager.saveExternalUserId(externalUserId)
        deviceId = deviceId.copy(externalId = externalUserId?.ifBlank { "" })
    }

    internal fun setDeviceEmail(email: String?) {
        sharedPrefsManager.saveDeviceEmail(email)
        deviceId = deviceId.copy(email = email)
    }

    internal fun setDevicePhone(phone: String?) {
        sharedPrefsManager.saveDevicePhone(phone)
        deviceId = deviceId.copy(phone = phone)
    }
}
