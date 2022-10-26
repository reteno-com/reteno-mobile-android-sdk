package com.reteno.core

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.model.user.User


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
     * Set the user ID and adds or modifies user attributes.
     *
     * @see com.reteno.core.model.user.User
     */
    fun setUserAttributes(externalUserId: String, user: User?)

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}