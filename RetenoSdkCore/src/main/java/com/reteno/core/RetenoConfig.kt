package com.reteno.core

import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.identification.DeviceIdProvider

/**
 * @param isPausedInAppMessages - indicates paused/resumed state for in-app messages
 * @param isPausedPushInAppMessages - indicates paused/resumed state for in-app messages from push notifications
 * @param userIdProvider Provider that will return custom userId. In case if id provided with a delay,
 * Reteno SDK will wait till id is going to be non-null then will initialize itself
 * @property platform - current platform name (Note that this property is mutable for multiplatform usage
 * and it should not be changed in other use cases).
 * @property lifecycleTrackingOptions - behavior of automatic app lifecycle event tracking, see [Reteno.setLifecycleEventConfig] to learn more
 * @property accessKey - reteno access key
 * */
data class RetenoConfig @JvmOverloads constructor(
    val accessKey: String = "",
    val isPausedInAppMessages: Boolean = false,
    val userIdProvider: DeviceIdProvider? = null,
    val lifecycleTrackingOptions: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL,
    val isPausedPushInAppMessages: Boolean = false,
) {
    var platform: String = "Android"
}