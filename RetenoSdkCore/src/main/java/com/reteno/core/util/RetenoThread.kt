package com.reteno.core.util

import android.os.Process
import java.util.concurrent.atomic.AtomicInteger

internal class RetenoThread(
    runnable: Runnable
) : Thread(runnable, THREAD_PREFIX_NAME + SEQUENCE_GENERATOR.getAndIncrement()) {

    companion object {
        private val SEQUENCE_GENERATOR = AtomicInteger(1)
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.run()
    }

}