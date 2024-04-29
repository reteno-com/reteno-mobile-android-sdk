package com.reteno.core.identification

fun interface DeviceIdProvider {
    fun getDeviceId(): String?
}