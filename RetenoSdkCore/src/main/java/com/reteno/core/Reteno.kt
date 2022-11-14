package com.reteno.core

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.User
import java.time.ZonedDateTime


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
     * @see com.reteno.core.model.user.User
     */
    fun setUserAttributes(externalUserId: String, user: User?)

    /**
     *  Tracking events
     *  @param eventType key for event type
     *  @param date time when event occurred
     *  @param parameters additional custom event parameters
     */
    // TODO for testing, need review in future!
    fun logEvent(eventType: String, date: ZonedDateTime, parameters: List<Parameter>?)


    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [com.reteno.core.domain.controller.ScheduleController.Companion.FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePushData()

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}