package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

data class UserCustomFieldDTO(
    @SerializedName("key")
    val key: String,
    @SerializedName("value")
    val value: String?
)