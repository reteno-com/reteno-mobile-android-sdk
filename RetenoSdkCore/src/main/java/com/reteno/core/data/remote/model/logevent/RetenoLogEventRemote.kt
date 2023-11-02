package com.reteno.core.data.remote.model.logevent

import com.google.gson.annotations.SerializedName
import com.reteno.core.data.remote.model.device.DeviceOsRemote

internal data class RetenoLogEventRemote(
    @SerializedName("platformName")
    var platformName: DeviceOsRemote,
    @SerializedName("osVeersion")
    var osVersion: String,
    @SerializedName("version")
    var version: String?,
    @SerializedName("device")
    var device: String,
    @SerializedName("sdkVersion")
    var sdkVersion: String,
    @SerializedName("deviceId")
    var deviceId: String?,
    @SerializedName("bundleId")
    var bundleId: String?,
    @SerializedName("logLevel")
    var logLevel: String,
    @SerializedName("errorMessage")
    var errorMessage: String?
)