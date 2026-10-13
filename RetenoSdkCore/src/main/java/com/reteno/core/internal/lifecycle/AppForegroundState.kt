package com.reteno.core.internal.lifecycle

internal class AppForegroundState {

    private var openedAt: Long? = null
    private var wasBackgrounded = false

    @Synchronized
    fun enterForeground(now: Long): Boolean? {
        if (openedAt != null) return null
        openedAt = now
        return wasBackgrounded.also { wasBackgrounded = false }
    }

    @Synchronized
    fun enterBackground(): Long? {
        val opened = openedAt ?: return null
        openedAt = null
        wasBackgrounded = true
        return opened
    }
}
