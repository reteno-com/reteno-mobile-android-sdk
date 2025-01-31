package com.reteno.core.view.iam.container

import android.app.Activity
import android.content.Context
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.features.iam.RetenoAndroidHandler

internal interface IamContainer {

    fun show(activity: Activity)
    fun dismiss()
    fun destroy()
    fun attachHtml(handler: RetenoAndroidHandler, html: String)

    companion object {
        fun create(context: Context, type: InAppLayoutType): IamContainer {
            return when (type) {
                InAppLayoutType.FULL -> FullscreenIamContainer(context)
                InAppLayoutType.SLIDE_UP -> SlideUpIamContainer()
            }
        }
    }
}