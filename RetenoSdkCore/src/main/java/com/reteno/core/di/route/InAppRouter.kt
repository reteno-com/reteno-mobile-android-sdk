package com.reteno.core.di.route

import com.reteno.core.di.graph.InAppGraph
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.iam.InAppActions
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class InAppRouter(private val inApp: InAppGraph) : InAppActions {

    override fun submitInAppPause(paused: Boolean) {
        inApp.inAppQueue.enqueue { inApp.pauseInApps(paused) }
    }

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        inApp.inAppQueue.enqueue { inApp.setInAppPauseBehaviour(behaviour) }
    }

    override fun submitPushInAppPause(paused: Boolean) {
        inApp.inAppPresenter.pauseIncomingPushInApps(paused)
    }

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        inApp.inAppPresenter.setPauseBehaviour(behaviour)
    }

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) {
        inApp.inAppPresenter.setInAppLifecycleCallback(callback)
    }
}
