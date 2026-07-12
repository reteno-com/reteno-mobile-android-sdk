package com.reteno.core.data.local.model.event

import com.reteno.core.data.local.config.DeviceId

data class EventsDb(
    val deviceId: String,
    val externalUserId: String? = null,
    val eventList: List<EventDb>
) {
    constructor(deviceId: DeviceId, eventDb: EventDb) : this(
        deviceId = deviceId.id,
        externalUserId = deviceId.externalId,
        eventList = listOf(eventDb)
    )
}