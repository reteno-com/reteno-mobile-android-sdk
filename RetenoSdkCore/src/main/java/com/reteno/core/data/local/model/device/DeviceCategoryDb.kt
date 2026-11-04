package com.reteno.core.data.local.model.device

import com.reteno.core.domain.model.device.DeviceCategory

enum class DeviceCategoryDb {
    MOBILE,
    TABLET;
    companion object {
        fun fromString(value: String?): DeviceCategoryDb =
            when (value) {
                DeviceCategory.MOBILE.toString() -> MOBILE
                DeviceCategory.TABLET.toString() -> TABLET
                else -> MOBILE
            }
    }
}
