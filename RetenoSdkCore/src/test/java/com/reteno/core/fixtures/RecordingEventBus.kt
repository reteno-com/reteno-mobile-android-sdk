package com.reteno.core.fixtures

import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.event.EventBus
import kotlinx.coroutines.flow.MutableSharedFlow

internal class RecordingEventBus {
    private val flow = MutableSharedFlow<Event>(replay = 64)
    val bus = EventBus(flow)

    val emitted: List<Event>
        get() = flow.replayCache
}
