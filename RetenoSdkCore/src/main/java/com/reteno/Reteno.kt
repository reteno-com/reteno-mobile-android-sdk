package com.reteno

import androidx.annotation.NonNull
import com.reteno.data.local.config.DeviceIdMode


interface Reteno {

    fun changeDeviceIdMode(deviceIdMode: DeviceIdMode)

    fun setExternalDeviceId(@NonNull externalDeviceId: String)

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}