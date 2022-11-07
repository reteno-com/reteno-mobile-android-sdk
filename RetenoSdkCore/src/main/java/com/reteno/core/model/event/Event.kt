package com.reteno.core.model.event

import java.time.ZonedDateTime

data class Event(
    val eventTypeKey: String,
    val occurred: ZonedDateTime,
    val params: List<Parameter>? = null
)