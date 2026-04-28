package com.reteno.core.view.iam.container

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.cardview.widget.CardView
import androidx.core.widget.PopupWindowCompat
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.IamViewImpl

internal class FullscreenIamContainer(
    context: Context,
    jsInterface: RetenoAndroidHandler,
    private val iamFetchResult: IamFetchResult
) : IamContainer {
    private val parentLayout = FrameLayout(context.applicationContext)
    private val popupWindow = createPopupWindow(parentLayout)
    private val cardView = createCardView(context.applicationContext)
    private val webView = createWebView(context.applicationContext, jsInterface)

    init {
        addCardViewToParentLayout()
        addWebViewToCardView()
    }

    private fun addCardViewToParentLayout() {
        /*@formatter:off*/ Logger.i(TAG, "addCardViewToParentLayout(): ", "")
        /*@formatter:on*/
        parentLayout.clipChildren = false
        parentLayout.clipToPadding = false
        parentLayout.addView(
            cardView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun addWebViewToCardView() {
        /*@formatter:off*/ Logger.i(TAG, "addWebViewToCardView(): ", "")
        /*@formatter:on*/
        cardView.addView(
            webView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun createPopupWindow(parentLayout: FrameLayout): PopupWindow {
        /*@formatter:off*/ Logger.i(TAG, "createPopupWindow(): ", "parentLayout = [", parentLayout, "]")
        /*@formatter:on*/

        val popupWindow = PopupWindow(
            parentLayout,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isTouchable = true
        // Required for getting fullscreen under notches working in portrait mode
        popupWindow.isClippingEnabled = false
        return popupWindow
    }

    private fun createCardView(context: Context): CardView {
        /*@formatter:off*/ Logger.i(TAG, "createCardView(): ", "context = [", context, "]")
        /*@formatter:on*/
        val cardView = CardView(context)
        cardView.layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        // Set the initial elevation of the CardView to 0dp if using Android 6 API 23
        //  Fixes bug when animating a elevated CardView class
        cardView.cardElevation = IamViewImpl.dpToPx(5).toFloat()
        cardView.radius = IamViewImpl.dpToPx(8).toFloat()
        cardView.clipChildren = false
        cardView.clipToPadding = false
        cardView.preventCornerOverlap = false
        cardView.setCardBackgroundColor(Color.TRANSPARENT)
        return cardView
    }

    private fun createWebView(context: Context, jsInterface: RetenoAndroidHandler): WebView {
        /*@formatter:off*/ Logger.i(TAG, "createWebView(): ", "context = [", context, "]")
        /*@formatter:on*/
        val webView = WebView(context)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WEB VIEW TAG",
                    consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                )
                return true
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
        }
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(jsInterface, IamViewImpl.JS_INTERFACE_NAME)
        webView.loadDataWithBaseURL("", iamFetchResult.fullHtml, "text/html", "UTF-8", "")
        return webView
    }

    override fun dismiss() {
        try {
            popupWindow.dismiss()
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "dismiss(): popupWindow.dismiss() ", e)
            /*@formatter:on*/
        }
    }

    override fun destroy() {
        try {
            parentLayout.removeAllViews()
            popupWindow.dismiss()
            cardView.removeAllViews()
            webView.removeAllViews()
            webView.removeJavascriptInterface(IamViewImpl.JS_INTERFACE_NAME)
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "destroy(): popupWindow.dismiss() ", e)
            /*@formatter:on*/
        }
    }

    override fun show(activity: Activity) {
        try {
            val gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            val displayType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            PopupWindowCompat.setWindowLayoutType(popupWindow, displayType)
            popupWindow.showAtLocation(
                activity.window.decorView.rootView,
                gravity,
                0,
                0
            )
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "show(): popupWindow.show() ", e)
            /*@formatter:on*/
        }
    }

    override fun onHeightDefined(newHeight: Int) {
    }

    companion object {
        private val TAG: String = FullscreenIamContainer::class.java.simpleName
    }
}