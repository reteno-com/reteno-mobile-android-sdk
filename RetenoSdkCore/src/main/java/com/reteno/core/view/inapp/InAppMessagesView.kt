package com.reteno.core.view.inapp

import android.app.Activity

interface InAppMessagesView {

    fun initialize(widgetId: String)

    fun resume(activity: Activity)

    fun pause(activity: Activity)
}