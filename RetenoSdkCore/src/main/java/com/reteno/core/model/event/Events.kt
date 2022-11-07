package com.reteno.core.model.event

data class Events(
    val deviceId: String,
    val externalUserId: String? = null,
    val eventList: List<Event>
)