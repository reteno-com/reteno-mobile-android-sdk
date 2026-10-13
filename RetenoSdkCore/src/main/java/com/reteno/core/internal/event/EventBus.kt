package com.reteno.core.internal.event

import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.event.eventFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal class EventBus(
    private val flow: MutableSharedFlow<Event> = eventFlow
) {
    val events: SharedFlow<Event>
        get() = flow

    suspend fun emit(event: Event) {
        flow.emit(event)
    }
}
