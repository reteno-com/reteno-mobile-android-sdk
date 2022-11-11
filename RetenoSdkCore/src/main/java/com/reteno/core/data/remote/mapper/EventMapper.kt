package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.model.event.EventRemote
import com.reteno.core.data.remote.model.event.EventsRemote
import com.reteno.core.data.remote.model.event.ParameterRemote
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.util.Util.formatToRemote

fun Events.toRemote() = EventsRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    eventList = eventList.map { it.toRemote() }
)

fun Event.toRemote() = EventRemote(
    eventTypeKey = eventTypeKey,
    occurred = occurred.formatToRemote(),
    params = params?.map { it.toRemote() }
)


fun Parameter.toRemote() = ParameterRemote(
    name = name,
    value = value
)

//==================================================================================================
fun EventsDb.toRemote() = EventsRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    eventList = eventList.map { it.toRemote() }
)

fun EventDb.toRemote() = EventRemote(
    eventTypeKey = eventTypeKey,
    occurred = occurred,
    params = params?.map { it.toRemote() }
)


fun ParameterDb.toRemote() = ParameterRemote(
    name = name,
    value = value
)