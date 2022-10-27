package com.reteno.core

import androidx.annotation.NonNull
import com.reteno.core.data.local.config.DeviceIdMode


interface Reteno {

    fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: () -> Unit)

    fun setExternalDeviceId(@NonNull externalDeviceId: String)

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}