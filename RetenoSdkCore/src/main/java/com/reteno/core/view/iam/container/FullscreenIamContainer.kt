package com.reteno.core.view.iam.container

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.cardview.widget.CardView
import androidx.core.widget.PopupWindowCompat
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.view.iam.AndroidInAppScreen

internal class FullscreenIamContainer(
    context: Context,
    jsInterface: RetenoAndroidHandler,
    iamFetchResult: IamFetchResult
) : IamContainer {

    private val webView = createInAppWebView(context.applicationContext, jsInterface, iamFetchResult.fullHtml)
    private val cardView = CardView(context.applicationContext).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        // Set the initial elevation of the CardView to 0dp if using Android 6 API 23
        //  Fixes bug when animating a elevated CardView class
        cardElevation = AndroidInAppScreen.dpToPx(5).toFloat()
        radius = AndroidInAppScreen.dpToPx(8).toFloat()
        clipChildren = false
        clipToPadding = false
        preventCornerOverlap = false
        setCardBackgroundColor(Color.TRANSPARENT)
        addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    private val parentLayout = FrameLayout(context.applicationContext).apply {
        clipChildren = false
        clipToPadding = false
        addView(cardView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    private val popupWindow = PopupWindow(
        parentLayout,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        true
    ).apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isTouchable = true
        // Required for getting fullscreen under notches working in portrait mode
        isClippingEnabled = false
    }

    override fun show(activity: Activity) {
        PopupWindowCompat.setWindowLayoutType(popupWindow, WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
        popupWindow.showAtLocation(activity.window.decorView.rootView, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 0)
    }

    override fun onHeightDefined(newHeight: Int) = Unit

    override fun dismiss() {
        popupWindow.dismiss()
    }

    override fun destroy() {
        parentLayout.removeAllViews()
        popupWindow.dismiss()
        cardView.removeAllViews()
        webView.removeAllViews()
        webView.removeJavascriptInterface(AndroidInAppScreen.JS_INTERFACE_NAME)
    }
}
