package com.reteno.core

import com.reteno.core.identification.DeviceIdProvider

/**
 * @param isPausedInAppMessages - indicates paused/resumed state for in-app messages
 * @param userIdProvider Provider that will return custom userId. In case if id provided with a delay,
 * Reteno SDK will wait till id is going to be non-null then will initialize itself
 * @property platform - current platform name (Note that this property is mutable for multiplatform usage
 * and it should not be changed in other use cases).
 * */
class RetenoConfig @JvmOverloads constructor(
    var isPausedInAppMessages: Boolean = false,
    val isLifecycleEventsEnabled: Boolean = true,
    val userIdProvider: DeviceIdProvider? = null
) {
    var platform: String = "Android"
}