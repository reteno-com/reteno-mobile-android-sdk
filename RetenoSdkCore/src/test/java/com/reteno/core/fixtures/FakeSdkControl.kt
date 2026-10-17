package com.reteno.core.fixtures

import com.reteno.core.RetenoConfig
import com.reteno.core.internal.action.init.SdkControl

internal class FakeSdkControl(private val log: FakeCallLog = FakeCallLog()) : SdkControl {
    val configs = mutableListOf<RetenoConfig>()
    override var isStarted: Boolean = false
    override var isInitialized: Boolean = false
        private set
    private var isInitializedByUser: Boolean = false
    private val waiting = mutableListOf<HeldOperation>()

    override fun runAfterInit(operation: () -> Unit) = runWhenReady(HeldOperation(operation, needsUser = false))

    override fun runAfterUserInit(operation: () -> Unit) = runWhenReady(HeldOperation(operation, needsUser = true))

    fun markInitialized(byUser: Boolean = true) {
        isInitialized = true
        isInitializedByUser = isInitializedByUser || byUser
        val ready = waiting.filter { it.isReady() }
        waiting.removeAll(ready)
        ready.forEach { it.operation() }
    }

    private fun runWhenReady(held: HeldOperation) {
        if (held.isReady()) {
            held.operation()
        } else {
            waiting += held
        }
    }

    private fun HeldOperation.isReady() = if (needsUser) isInitializedByUser else isInitialized

    private class HeldOperation(val operation: () -> Unit, val needsUser: Boolean)

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
