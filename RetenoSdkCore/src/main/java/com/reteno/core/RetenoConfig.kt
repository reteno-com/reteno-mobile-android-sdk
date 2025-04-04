package com.reteno.core

import androidx.core.app.NotificationChannelCompat
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
class RetenoConfig private constructor(
    val isPausedInAppMessages: Boolean,
    val userIdProvider: DeviceIdProvider?,
    val lifecycleTrackingOptions: LifecycleTrackingOptions,
    val accessKey: String,
    val isPausedPushInAppMessages: Boolean,
    val defaultNotificationChannelConfig: ((NotificationChannelCompat.Builder) -> Unit)?,
    val platform: String
) {

    class Builder() {

        private var isPausedInApps: Boolean = false
        private var isPausedPushInApps: Boolean = false
        private var deviceIdProvider: DeviceIdProvider? = null
        private var lifecycleOptions = LifecycleTrackingOptions.ALL
        private var accessKey: String = ""
        private var currentPlatform: String = "Android"
        private var notificationChannelConfig: ((NotificationChannelCompat.Builder) -> Unit)? = null

        constructor(config: RetenoConfig) : this() {
            pauseInAppMessages(config.isPausedInAppMessages)
            pausePushInAppMessages(config.isPausedPushInAppMessages)
            customDeviceIdProvider(config.userIdProvider)
            accessKey(config.accessKey)
            lifecycleTrackingOptions(config.lifecycleTrackingOptions)
            config.defaultNotificationChannelConfig?.let { defaultNotificationChannelConfig(config.defaultNotificationChannelConfig) }
        }

        fun pauseInAppMessages(isPaused: Boolean): Builder {
            isPausedInApps = isPaused
            return this
        }

        fun pausePushInAppMessages(isPaused: Boolean): Builder {
            isPausedPushInApps = isPaused
            return this
        }

        fun customDeviceIdProvider(provider: DeviceIdProvider?): Builder {
            deviceIdProvider = provider
            return this
        }

        fun lifecycleTrackingOptions(options: LifecycleTrackingOptions): Builder {
            lifecycleOptions = options
            return this
        }

        fun accessKey(key: String): Builder {
            if (key.isBlank()) throw IllegalArgumentException("Access key can't be blank")
            accessKey = key
            return this
        }

        fun defaultNotificationChannelConfig(builder: (NotificationChannelCompat.Builder) -> Unit): Builder {
            notificationChannelConfig = builder
            return this
        }

        fun setPlatform(platform: String): Builder {
            currentPlatform = platform
            return this
        }

        fun build(): RetenoConfig {
            if (accessKey.isBlank()) throw IllegalStateException("Access key is blank. Use accessKey method to set access key.")
            return RetenoConfig(
                isPausedInAppMessages = isPausedInApps,
                isPausedPushInAppMessages = isPausedPushInApps,
                accessKey = accessKey,
                userIdProvider = deviceIdProvider,
                lifecycleTrackingOptions = lifecycleOptions,
                defaultNotificationChannelConfig = notificationChannelConfig,
                platform = currentPlatform
            )
        }
    }
}
