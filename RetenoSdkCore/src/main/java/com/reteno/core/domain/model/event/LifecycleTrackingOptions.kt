package com.reteno.core.domain.model.event

data class LifecycleTrackingOptions(
    val appLifecycleEnabled: Boolean = true,
    val foregroundLifecycleEnabled: Boolean = false,
    val pushSubscriptionEnabled: Boolean = true,
    val sessionStartEventsEnabled: Boolean = true,
    val sessionEndEventsEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = false,
            appLifecycleEnabled = true,
            pushSubscriptionEnabled = true,
            sessionStartEventsEnabled = true,
            sessionEndEventsEnabled = false
        )
        val ALL = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = true,
            appLifecycleEnabled = true,
            pushSubscriptionEnabled = true,
            sessionStartEventsEnabled = true,
            sessionEndEventsEnabled = true
        )
        val NONE = LifecycleTrackingOptions(
            foregroundLifecycleEnabled = false,
            appLifecycleEnabled = false,
            pushSubscriptionEnabled = false,
            sessionStartEventsEnabled = false,
            sessionEndEventsEnabled = false
        )
    }
}