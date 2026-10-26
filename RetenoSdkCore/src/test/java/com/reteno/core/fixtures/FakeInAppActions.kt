package com.reteno.core.fixtures

import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.iam.InAppActions
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class FakeInAppActions(private val log: FakeCallLog = FakeCallLog()) : InAppActions {

    override fun submitInAppPause(paused: Boolean) {
        log.record("submitInAppPause $paused")
    }

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        log.record("submitInAppPauseBehaviour $behaviour")
    }

    override fun submitPushInAppPause(paused: Boolean) {
        log.record("submitPushInAppPause $paused")
    }

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        log.record("submitPushInAppPauseBehaviour $behaviour")
    }

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) {
        log.record("submitInAppLifecycleCallback ${callback != null}")
    }
}
