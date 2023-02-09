package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

internal data class UserRemote(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("externalUserId")
    val externalUserId: String?,
    @SerializedName("userAttributes")
    val userAttributes: UserAttributesRemote? = null,
    @SerializedName("subscriptionKeys")
    val subscriptionKeys: List<String>? = null,
    @SerializedName("groupNamesInclude")
    val groupNamesInclude: List<String>? = null,
    @SerializedName("groupNamesExclude")
    val groupNamesExclude: List<String>? = null
)