package com.reteno.core.view.iam

import android.app.Activity
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent

interface IamView {

    fun initialize(interactionId: String)

    fun initialize(inAppMessage: InAppMessage?, inAppMessageContent: InAppMessageContent?)

    fun resume(activity: Activity)

    fun pause(activity: Activity)
}