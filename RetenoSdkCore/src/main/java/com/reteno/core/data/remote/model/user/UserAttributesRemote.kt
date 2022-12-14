package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

internal data class UserAttributesRemote(
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("languageCode")
    val languageCode: String? = null,
    @SerializedName("timeZone")
    val timeZone: String? = null,
    @SerializedName("address")
    val address: AddressRemote? = null,
    @SerializedName("fields")
    val fields: List<UserCustomFieldRemote>? = null,
)