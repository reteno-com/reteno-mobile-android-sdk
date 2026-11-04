package com.reteno.core.internal.action.trace

internal fun interface MonotonicClock {
    fun nanos(): Long
}

internal val SystemMonotonicClock = MonotonicClock { System.nanoTime() }
