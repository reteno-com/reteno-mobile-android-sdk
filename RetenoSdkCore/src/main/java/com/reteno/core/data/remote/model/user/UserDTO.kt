package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

data class UserDTO(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("externalUserId")
    val externalUserId: String,
    @SerializedName("userAttributes")
    val userAttributes: UserAttributesDTO? = null,
    @SerializedName("subscriptionKeys")
    val subscriptionKeys: List<String>? = null,
    @SerializedName("groupNamesInclude")
    val groupNamesInclude: List<String>? = null,
    @SerializedName("groupNamesExclude")
    val groupNamesExclude: List<String>? = null
)