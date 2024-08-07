package com.reteno.core.data.local.config

data class DeviceId(
    internal val id: String,
    internal val externalId: String? = null,
    internal val mode: DeviceIdMode = DeviceIdMode.ANDROID_ID,
    internal val email: String? = null,
    internal val phone: String? = null
)