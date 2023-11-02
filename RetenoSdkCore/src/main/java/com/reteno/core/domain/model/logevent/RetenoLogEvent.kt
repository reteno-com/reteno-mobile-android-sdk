package com.reteno.core.domain.model.logevent

import com.reteno.core.BuildConfig
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.util.DeviceInfo

data class RetenoLogEvent(
    var platformName: DeviceOS = DeviceOS.ANDROID,
    var osVersion: String = DeviceInfo.fetchOsVersion(),
    var version: String? = DeviceInfo.fetchAppVersion(),
    var device: String = DeviceInfo.fetchDeviceModel(),
    var sdkVersion: String = BuildConfig.SDK_VERSION,
    var deviceId: String? = null,
    var bundleId: String? = null,
    var logLevel: LogLevel = LogLevel.DEBUG,
    var errorMessage: String? = null
)