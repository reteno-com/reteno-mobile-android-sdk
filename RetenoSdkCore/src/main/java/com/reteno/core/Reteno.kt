package com.reteno.core

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.user.User
import com.reteno.core.lifecycle.ScreenTrackingConfig


interface Reteno {

    /**
     * Sets the type of device ID to use. Default: [DeviceIdMode.ANDROID_ID]
     *
     * @see com.reteno.core.data.local.config.DeviceIdMode
     */
    fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: () -> Unit)

    /**
     *  Set the user ID.
     */
    fun setUserAttributes(externalUserId: String)

    /**
     * Set the user ID and add or modify user attributes.
     *
     * @see com.reteno.core.domain.model.user
     */
    fun setUserAttributes(externalUserId: String, user: User?)

    /**
     *  Tracking events
     *  @param event model to track
     */
    fun logEvent(event: Event)

    /**
     *  Tracking screen view events.
     *  Call this method when screen (fragment, activity, view) appeared for user
     *  @param screenName to track
     */
    fun logScreenView(screenName: String)

    /**
     * Enables automatic screen tracking. Screen view event happens on Fragment's onStart() by default
     * lifecycle callback.
     * @see com.reteno.core.lifecycle.ScreenTrackingConfig for additional configuration
     *
     * @param config - parameters for auto screen tracking
     */
    fun autoScreenTracking(config: ScreenTrackingConfig)


    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [com.reteno.core.domain.controller.ScheduleController.Companion.FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePushData()

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}