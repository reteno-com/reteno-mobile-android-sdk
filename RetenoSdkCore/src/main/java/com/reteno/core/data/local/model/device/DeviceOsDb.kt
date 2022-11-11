package com.reteno.core.data.local.model.device

import com.reteno.core.domain.model.device.DeviceOS

enum class DeviceOsDb {
    ANDROID, IOS;

    companion object {
        fun fromString(value: String?): DeviceOsDb =
            when (value) {
                DeviceOS.ANDROID.toString() -> ANDROID
                DeviceOS.IOS.toString() -> IOS
                else -> ANDROID
            }
    }
}