package com.reteno.core.domain.model.event

data class Events(
    val deviceId: String,
    val externalUserId: String? = null,
    val eventList: List<Event>
)