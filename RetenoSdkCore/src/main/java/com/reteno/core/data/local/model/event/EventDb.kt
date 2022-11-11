package com.reteno.core.data.local.model.event

data class EventDb(
    val eventTypeKey: String,
    val occurred: String,
    val params: List<ParameterDb>? = null
)