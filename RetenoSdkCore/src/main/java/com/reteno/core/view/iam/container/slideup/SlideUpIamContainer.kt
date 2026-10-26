package com.reteno.core.view.iam.container.slideup

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.contains
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.view.iam.AndroidInAppScreen
import com.reteno.core.view.iam.container.IamContainer
import com.reteno.core.view.iam.container.IamContainer.IamDismissListener
import com.reteno.core.view.iam.container.createInAppWebView

internal class SlideUpIamContainer(
    context: Context,
    jsInterface: RetenoAndroidHandler,
    fetchResult: IamFetchResult,
    iamDismissListener: IamDismissListener
) : IamContainer {

    private val fromBottom = fetchResult.layoutParams.position == Position.BOTTOM
    private val parentLayout = CoordinatorLayout(context.applicationContext)
    private val webView = createInAppWebView(context.applicationContext, jsInterface, fetchResult.fullHtml)

    init {
        val edge = if (fromBottom) SwipeGesture.Edge.BOTTOM else SwipeGesture.Edge.TOP
        val layoutParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
            CoordinatorLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            behavior = SwipeDismissBehavior<WebView>(SwipeGesture(edge)) { iamDismissListener.onIamDismissed() }
            gravity = if (fromBottom) Gravity.BOTTOM else Gravity.TOP
        }
        parentLayout.addView(webView, layoutParams)
    }

    override fun show(activity: Activity) {
        val content: ViewGroup = activity.findViewById(android.R.id.content)
        if (content.contains(parentLayout)) return
        activity.addContentView(
            parentLayout,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        webView.doOnNextLayout { view ->
            view.translationY = slideInStart(view.height, fromBottom)
            view.animate().translationY(0f).setInterpolator(LinearInterpolator()).setDuration(SLIDE_IN_MILLIS).start()
        }
    }

    override fun onHeightDefined(newHeight: Int) {
        webView.updateLayoutParams<CoordinatorLayout.LayoutParams> { height = newHeight }
    }

    override fun dismiss() {
        (parentLayout.parent as? ViewGroup)?.removeView(parentLayout)
    }

    override fun destroy() {
        //To stop propagating unexpected scroll event to Activity convent View
        parentLayout.isVisible = false
        dismiss()
        parentLayout.removeAllViews()
        webView.removeJavascriptInterface(AndroidInAppScreen.JS_INTERFACE_NAME)
    }

    private companion object {
        const val SLIDE_IN_MILLIS = 200L
    }
}

internal fun slideInStart(height: Int, fromBottom: Boolean): Float =
    if (fromBottom) height.toFloat() else -height.toFloat()
