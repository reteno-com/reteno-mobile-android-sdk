@file:Suppress("FunctionName")

package com.reteno.core

import android.app.Application
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.identification.DeviceIdProvider


@Deprecated(
    message = "Deprecated API, use Reteno.instance instead",
    replaceWith = ReplaceWith(expression = "Reteno.instance")
)
fun RetenoImpl(application: Application): Reteno = Reteno.instance

@Deprecated(
    message = "Deprecated API, use static function Reteno.initWithConfig() instead",
    replaceWith = ReplaceWith(expression = "Reteno.initWith(config)")
)
fun RetenoImpl(
    application: Application,
    accessKey: String,
    config: RetenoConfig = RetenoConfig()
): Reteno {
    if (accessKey.isBlank()) {
        throw IllegalArgumentException("Access key can't be blank.")
    }

    Reteno.initWithConfig(
        RetenoConfig.Builder(config)
            .accessKey(accessKey)
            .build()
    )
    return Reteno.instance
}

@Deprecated(message = "Deprecated API, use RetenoConfig.Builder for creating config instance")
fun RetenoConfig(
    isPausedInAppMessages: Boolean = false,
    userIdProvider: DeviceIdProvider? = null,
    lifecycleTrackingOptions: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL,
    accessKey: String = "",
    isPausedPushInAppMessages: Boolean = false
): RetenoConfig {
    return RetenoConfig.Builder()
        .pauseInAppMessages(isPausedInAppMessages)
        .customDeviceIdProvider(userIdProvider)
        .lifecycleTrackingOptions(lifecycleTrackingOptions)
        .accessKey(accessKey)
        .pausePushInAppMessages(isPausedPushInAppMessages)
        .build()
}
