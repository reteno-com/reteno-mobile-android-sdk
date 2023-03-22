package com.reteno.core.view.iam

import android.app.Activity

interface IamView {

    fun initialize(interactionId: String)

    fun resume(activity: Activity)

    fun pause(activity: Activity)
}