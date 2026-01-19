package com.reteno.push.events

object NotificationInAppCustomDataReceived : SimpleNotificationEventProcessor<InAppCustomData>()

data class InAppCustomData(
    val url: String?,
    val source: String,
    val inAppId: String,
    val data: Map<String, String>
)