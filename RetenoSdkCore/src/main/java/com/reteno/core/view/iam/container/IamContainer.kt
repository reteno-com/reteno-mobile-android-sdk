package com.reteno.core.view.iam.container

import android.app.Activity
import android.content.Context
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.view.iam.container.slideup.SlideUpIamContainer

internal interface IamContainer {

    fun show(activity: Activity)
    fun onHeightDefined(newHeight: Int)
    fun dismiss()
    fun destroy()

    fun interface IamDismissListener {
        fun onIamDismissed()
    }

    companion object {
        fun create(
            context: Context,
            iamFetchResult: IamFetchResult,
            jsInterface: RetenoAndroidHandler,
            dismissListener: IamDismissListener
        ): IamContainer {
            return when (iamFetchResult.layoutType) {
                InAppLayoutType.FULL -> FullscreenIamContainer(context, jsInterface, iamFetchResult)
                InAppLayoutType.SLIDE_UP -> SlideUpIamContainer(context, jsInterface, iamFetchResult, dismissListener)
            }
        }
    }
}