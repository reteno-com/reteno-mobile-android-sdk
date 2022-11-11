package com.reteno.core.data.local.model.event

data class EventsDb(
    val deviceId: String,
    val externalUserId: String? = null,
    val eventList: List<EventDb>
)