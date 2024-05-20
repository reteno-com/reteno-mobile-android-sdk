package com.reteno.core.domain.model.event

data class LifecycleTrackingOptions(
    val appLifecycleEnabled: Boolean = true,
    val pushSubscriptionEnabled: Boolean = true,
    val sessionEventsEnabled: Boolean = true
) {
    companion object {
        val ALL = LifecycleTrackingOptions()
        val NONE = LifecycleTrackingOptions(
            appLifecycleEnabled = false,
            pushSubscriptionEnabled = false,
            sessionEventsEnabled = false
        )
    }
}