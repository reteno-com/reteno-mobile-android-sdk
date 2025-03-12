package com.reteno.core.view.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

interface IamView {

    fun isViewShown(): Boolean

    fun initialize(interactionId: String)

    fun initialize(inAppMessage: InAppMessage)

    fun start()

    fun pause()

    fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?)

    fun pauseIncomingPushInApps(isPaused: Boolean)

    fun setPauseBehaviour(behaviour: InAppPauseBehaviour)
}