package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.event.EventDTO
import com.reteno.core.data.remote.model.event.EventsDTO
import com.reteno.core.data.remote.model.event.ParameterDTO
import com.reteno.core.model.event.Event
import com.reteno.core.model.event.Events
import com.reteno.core.model.event.Parameter
import com.reteno.core.util.Util.formatToRemote

fun Events.toRemote(): EventsDTO {
    return EventsDTO(
        deviceId = deviceId,
        externalUserId = externalUserId,
        eventList = eventList.map { it.toRemote() }
    )
}

fun Event.toRemote(): EventDTO {
    return EventDTO(
        eventTypeKey = eventTypeKey,
        occurred = occurred.formatToRemote(),
        params = params?.map { it.toRemote() }
    )
}

fun Parameter.toRemote(): ParameterDTO {
    return ParameterDTO(
        name = name,
        value = value
    )
}