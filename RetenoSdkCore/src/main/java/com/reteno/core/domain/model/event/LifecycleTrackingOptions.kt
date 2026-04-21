package com.reteno.core.domain.model.event

data class LifecycleTrackingOptions(
    val appLifecycleEnabled: Boolean = true,
    val foregroundLifecycleEnabled: Boolean = false,
    val pushSubscriptionEnabled: Boolean = true,
    val sessionEventsEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = false,
            appLifecycleEnabled = true,
            pushSubscriptionEnabled = true,
            sessionEventsEnabled = true
        )
        val ALL = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = true,
            appLifecycleEnabled = true,
            pushSubscriptionEnabled = true,
            sessionEventsEnabled = true
        )
        val NONE = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = false,
            appLifecycleEnabled = false,
            pushSubscriptionEnabled = false,
            sessionEventsEnabled = false
        )
    }
}