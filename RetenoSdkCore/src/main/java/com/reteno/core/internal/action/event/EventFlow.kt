package com.reteno.core.internal.action.event

import com.reteno.core.domain.model.event.Event
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

val eventFlow = MutableSharedFlow<Event>(
    extraBufferCapacity = 10,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
