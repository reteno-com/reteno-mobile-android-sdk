package com.reteno.core

import androidx.annotation.NonNull
import com.reteno.core.data.local.config.DeviceIdMode


interface Reteno {

    fun changeDeviceIdMode(deviceIdMode: DeviceIdMode, onIdChangedCallback: () -> Unit)

    fun setExternalDeviceId(@NonNull externalDeviceId: String)

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}