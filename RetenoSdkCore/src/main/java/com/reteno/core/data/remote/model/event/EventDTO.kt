package com.reteno.core.data.remote.model.event

import com.google.gson.annotations.SerializedName

data class EventDTO(
    @SerializedName("eventTypeKey")
    val eventTypeKey: String,
    @SerializedName("occurred")
    val occurred: String,
    @SerializedName("params")
    val params: List<ParameterDTO>? = null
)