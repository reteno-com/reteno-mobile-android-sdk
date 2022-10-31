package com.reteno.core.model.device

enum class DeviceOS {
    ANDROID, IOS;

    companion object {
        fun fromString(value: String): DeviceOS =
            when (value) {
                ANDROID.toString() -> ANDROID
                IOS.toString() -> IOS
                else -> ANDROID
            }
    }
}