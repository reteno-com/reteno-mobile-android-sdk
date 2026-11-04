package com.reteno.core.fixtures

import com.reteno.core.internal.lifecycle.SdkLifecycle

internal class FakeSdkLifecycle(
    override var isStarted: Boolean = true,
    private val onMain: () -> Boolean = { true }
) : SdkLifecycle {
    val calls = mutableListOf<String>()

    override fun stop() {
        calls += if (onMain()) "stop on main" else "stop off main"
    }

    override fun start() {
        calls += if (onMain()) "start on main" else "start off main"
    }
}
