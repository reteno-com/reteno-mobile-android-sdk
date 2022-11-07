package com.reteno.core

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.model.event.Parameter
import com.reteno.core.model.user.User
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

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}