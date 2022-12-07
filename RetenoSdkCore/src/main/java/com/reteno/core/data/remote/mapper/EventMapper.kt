package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.model.event.EventRemote
import com.reteno.core.data.remote.model.event.EventsRemote
import com.reteno.core.data.remote.model.event.ParameterRemote

internal fun EventsDb.toRemote() = EventsRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    eventList = eventList.map { it.toRemote() }
)

internal fun EventDb.toRemote() = EventRemote(
    eventTypeKey = eventTypeKey,
    occurred = occurred,
    params = params?.map { it.toRemote() }
)


internal fun ParameterDb.toRemote() = ParameterRemote(
    name = name,
    value = value
)