package com.reteno.core.domain.model.iam

import com.reteno.core.domain.model.event.Event

class EventWaitingForInApp(
    val waitingStartMillis: Long,
    val event: Event
)