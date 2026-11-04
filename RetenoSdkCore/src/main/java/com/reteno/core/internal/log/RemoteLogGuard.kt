package com.reteno.core.internal.log

internal object RemoteLogGuard {
    private val mutedOnThread = ThreadLocal<Boolean>()

    fun <T> muted(block: () -> T): T {
        val wasMuted = mutedOnThread.get()
        mutedOnThread.set(true)
        try {
            return block()
        } finally {
            mutedOnThread.set(wasMuted)
        }
    }

    fun unlessMuted(block: () -> Unit) {
        if (mutedOnThread.get() == true) return
        muted(block)
    }
}
