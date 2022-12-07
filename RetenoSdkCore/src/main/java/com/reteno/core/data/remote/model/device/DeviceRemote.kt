package com.reteno.core.data.remote.model.device

import com.google.gson.annotations.SerializedName


internal data class DeviceRemote(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("externalUserId")
    val externalUserId: String?,
    @SerializedName("pushToken")
    val pushToken: String?,
    @SerializedName("pushSubscribed")
    val pushSubscribed: Boolean?,
    @SerializedName("category")
    val category: DeviceCategoryRemote,
    @SerializedName("osType")
    val osType: DeviceOsRemote = DeviceOsRemote.ANDROID,
    @SerializedName("osVersion")
    val osVersion: String?,
    @SerializedName("deviceModel")
    val deviceModel: String?,
    @SerializedName("appVersion")
    val appVersion: String?,
    @SerializedName("languageCode")
    val languageCode: String?,
    @SerializedName("timeZone")
    val timeZone: String?,
    @SerializedName("advertisingId")
    val advertisingId: String?
)