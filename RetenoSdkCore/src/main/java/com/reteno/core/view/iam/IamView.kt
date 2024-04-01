package com.reteno.core.view.iam

import android.app.Activity
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

interface IamView {

    fun isViewShown(): Boolean

    fun initialize(interactionId: String)

    fun initialize(inAppMessage: InAppMessage)

    fun resume(activity: Activity)

    fun pause(activity: Activity)

    fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?)
}