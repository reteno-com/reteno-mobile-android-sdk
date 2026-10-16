package com.reteno.core.fixtures

import com.reteno.core.RetenoConfig
import com.reteno.core.internal.action.init.SdkControl

internal class FakeSdkControl(private val log: FakeCallLog = FakeCallLog()) : SdkControl {
    val configs = mutableListOf<RetenoConfig>()
    override var isStarted: Boolean = false
    override var isInitialized: Boolean = false
        private set
    private val waitingForInit = mutableListOf<() -> Unit>()

    override fun runAfterInit(operation: () -> Unit) {
        if (isInitialized) {
            operation()
            return
        }
        waitingForInit += operation
    }

    fun markInitialized() {
        isInitialized = true
        waitingForInit.forEach { it() }
        waitingForInit.clear()
    }

    override fun boot() {
        log.record("boot")
    }

    override fun applyUserConfig(config: RetenoConfig) {
        configs += config
        log.record("applyUserConfig")
    }

    override fun requestStart() {
        log.record("requestStart")
    }

    override fun requestStop() {
        log.record("requestStop")
    }

    override fun submitSessionDuration(durationMillis: Long) {
        log.record("submitSessionDuration $durationMillis")
    }
}
