package com.reteno.core.data.local.model.logevent

import com.reteno.core.data.local.model.device.DeviceOsDb

data class RetenoLogEventDb(
    val rowId: String? = null,
    var platformName: DeviceOsDb = DeviceOsDb.ANDROID,
    var osVersion: String,
    var version: String?,
    var device: String,
    var sdkVersion: String,
    var deviceId: String?,
    var bundleId: String?,
    var logLevel: LogLevelDb,
    var errorMessage: String?
)