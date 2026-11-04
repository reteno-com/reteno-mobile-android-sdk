package com.reteno.core.internal.action.device

import java.util.concurrent.atomic.AtomicBoolean

internal class ContactSession {

    private val deviceSent = AtomicBoolean(false)

    val isDeviceSent: Boolean
        get() = deviceSent.get()

    fun markDeviceSent() {
        deviceSent.set(true)
    }

    fun markDeviceNotSent() {
        deviceSent.set(false)
    }
}
