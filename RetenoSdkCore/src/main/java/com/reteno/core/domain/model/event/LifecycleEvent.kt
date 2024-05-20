package com.reteno.core.domain.model.event

data class LifecycleEvent(
    val type: LifecycleEventType,
    val event: Event
)