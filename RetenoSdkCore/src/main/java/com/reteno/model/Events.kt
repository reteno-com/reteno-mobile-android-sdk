package com.reteno.model

import com.google.gson.annotations.SerializedName

data class Events(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("externalUserId")
    val externalUserId: String? = null,
    @SerializedName("events")
    val events: List<Event>
)