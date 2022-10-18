package com.reteno.core.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class EventBatch(
    @SerializedName("eventTypeKey")
    val eventTypeKey: String,
    @SerializedName("occurred")
    val occurred: LocalDateTime,
    @SerializedName("params")
    val params: Map<String, Any> = emptyMap()
)