package com.reteno.core.internal.action.iam

import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal interface InAppActions {
    fun submitInAppPause(paused: Boolean)

    fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour)

    fun submitPushInAppPause(paused: Boolean)

    fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour)

    fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?)
}
