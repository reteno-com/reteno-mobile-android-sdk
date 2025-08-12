package com.reteno.core.data.local.config

data class DeviceId(
    internal val idBody: String,
    internal val idSuffix: String? = null,
    internal val externalId: String? = null,
    internal val mode: DeviceIdMode = DeviceIdMode.ANDROID_ID,
    internal val email: String? = null,
    internal val phone: String? = null
) {
    internal val id: String = buildString {
        append(idBody)
        idSuffix?.let {
            append(":")
            append(it)
        }
    }
}