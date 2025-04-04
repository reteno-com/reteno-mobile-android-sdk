package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.util.Util.formatToRemoteExplicitMillis

fun Events.toDb() = EventsDb(
    deviceId = deviceId,
    externalUserId = externalUserId,
    eventList = eventList.map { it.toDb() }
)

internal fun Event.toDb() = EventDb(
    eventTypeKey = eventTypeKey,
    occurred = occurred.formatToRemoteExplicitMillis(),
    params = params?.map { it.toDb() }
)

internal fun Parameter.toDb() = ParameterDb(
    name = name,
    value = value
)