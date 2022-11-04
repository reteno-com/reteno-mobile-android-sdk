package com.reteno.core.model.device

enum class DeviceCategory {
    MOBILE,
    TABLET;

    companion object {
        fun fromString(value: String?): DeviceCategory =
            when (value) {
                MOBILE.toString() -> MOBILE
                TABLET.toString() -> TABLET
                else -> MOBILE
            }
    }
}
