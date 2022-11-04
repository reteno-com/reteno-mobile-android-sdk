package com.reteno.core.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Event(
    @SerializedName("eventTypeKey")
    val eventTypeKey: String,
    @SerializedName("occurred")
    val occurred: LocalDateTime,
    @SerializedName("params")
    val params: List<Parameter>?
)