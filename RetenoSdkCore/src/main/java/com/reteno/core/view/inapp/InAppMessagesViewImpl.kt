package com.reteno.core.view.inapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.cardview.widget.CardView
import androidx.core.widget.PopupWindowCompat
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.InAppMessagesController
import com.reteno.core.features.inapp.InAppJsEvent
import com.reteno.core.features.inapp.InAppJsEventType
import com.reteno.core.features.inapp.InAppJsPayload
import com.reteno.core.features.inapp.RetenoAndroidHandler
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class InAppMessagesViewImpl(
    private val activityHelper: RetenoActivityHelper,
    private val inAppMessagesController: InAppMessagesController
) : InAppMessagesView {

    private val inAppShowScope = CoroutineScope(Dispatchers.Main.immediate)

    private val isViewShown = AtomicBoolean(false)

    private lateinit var parentLayout: FrameLayout
    private lateinit var popupWindow: PopupWindow
    private lateinit var cardView: CardView
    private lateinit var webView: WebView

    private val retenoAndroidHandler: RetenoAndroidHandler = object : RetenoAndroidHandler() {
        override fun onMessagePosted(event: String?) {
            /*@formatter:off*/ Logger.i(TAG, "onMessagePosted(): ", "event = [", event, "]")
            /*@formatter:on*/
            try {
                val jsEvent: InAppJsEvent = event?.fromJson<InAppJsEvent>() ?: return
                when (jsEvent.type) {
                    InAppJsEventType.WIDGET_INIT_SUCCESS -> onWidgetInitialized()
                    InAppJsEventType.OPEN_URL -> openUrl(jsEvent.payload)
                    InAppJsEventType.CLOSE_WIDGET -> closeWidget(jsEvent.payload)
                }
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.e(TAG, "onMessagePosted(): ", e)
                /*@formatter:on*/
            }
        }
    }

    private fun onWidgetInitialized() {
        /*@formatter:off*/ Logger.i(TAG, "onWidgetInitialized(): ", "")
        /*@formatter:on*/
        showInAppPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
    }

    private fun openUrl(payload: InAppJsPayload?) {
        /*@formatter:off*/ Logger.i(TAG, "openUrl(): ", "payload = [", payload, "]")
        /*@formatter:on*/
        val url = payload?.url ?: return
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        deepLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        OperationQueue.addUiOperation {
            activityHelper.currentActivity?.startActivity(deepLinkIntent)
        }
        teardown()
    }

    private fun closeWidget(payload: InAppJsPayload?) {
        /*@formatter:off*/ Logger.i(TAG, "closeWidget(): ", "payload = [", payload, "]")
        /*@formatter:on*/
        teardown()
    }

    override fun initialize(widgetId: String) {
        /*@formatter:off*/ Logger.i(TAG, "initialize(): ", "widgetId = [", widgetId, "]")
        /*@formatter:on*/
        try {
            teardown()
            inAppMessagesController.fetchInAppMessagesFullHtml(widgetId)
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "initialize(): ", e)
            /*@formatter:on*/
        }
    }

    override fun resume(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [", activity, "]") 
        /*@formatter:on*/
        if (isViewShown.get()) {
            if (!popupWindow.isShowing) {
                showInAppPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
            }
            return
        }

        createInAppInActivity(activity)
        inAppShowScope.launch {
            inAppMessagesController.fullHtmlStateFlow.collect { result ->
                ensureActive()
                if (result is ResultDomain.Success) {
                    uploadHtmlIntoWebView(result.body)
                }
            }
        }
    }

    override fun pause(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [", activity, "]") 
        /*@formatter:on*/
        inAppShowScope.coroutineContext.cancelChildren()
        if (isViewShown.get()) {
            popupWindow.dismiss()
        }
    }


    private fun showInAppPopupWindowOnceReady(attempts: Int) {
        /*@formatter:off*/ Logger.i(TAG, "showInAppPopupWindowOnceReady(): ", "attempts = [", attempts, "]")
        /*@formatter:on*/
        if (attempts < 0) {
            return
        }

        if (activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()) {
            OperationQueue.addUiOperation {
                activityHelper.currentActivity?.let(::showInAppPopupWindow)
            }
        } else {
            OperationQueue.addOperationAfterDelay({
                showInAppPopupWindowOnceReady(attempts - 1)
            }, DELAY_UI_MS)
        }
    }

    private fun createInAppInActivity(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "showInActivity(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        parentLayout = FrameLayout(activity)
        popupWindow = createPopupWindow(parentLayout)
        cardView = createCardView(activity)
        webView = createWebView(activity)

        addCardViewToParentLayout()
        addWebViewToCardView()
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
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Set the initial elevation of the CardView to 0dp if using Android 6 API 23
        //  Fixes bug when animating a elevated CardView class
        cardView.cardElevation = dpToPx(5).toFloat()
        cardView.radius = dpToPx(8).toFloat()
        cardView.clipChildren = false
        cardView.clipToPadding = false
        cardView.preventCornerOverlap = false
        cardView.setCardBackgroundColor(Color.TRANSPARENT)
        return cardView
    }

    private fun createWebView(activity: Activity): WebView {
        /*@formatter:off*/ Logger.i(TAG, "createWebView(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        val webView = WebView(activity)
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
        return webView
    }

    private fun addCardViewToParentLayout() {
        parentLayout.clipChildren = false
        parentLayout.clipToPadding = false
        parentLayout.addView(cardView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private fun addWebViewToCardView() {
        cardView.addView(webView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private fun uploadHtmlIntoWebView(fullHtml: String) {
        val encodedHtml = Base64.encodeToString(fullHtml.toByteArray(), Base64.NO_PADDING)
        webView.loadData(encodedHtml, MIME_TYPE, ENCODING)
        webView.addJavascriptInterface(retenoAndroidHandler, JS_INTERFACE_NAME)
    }

    private fun showInAppPopupWindow(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "showPopupWindow(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        val gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        val displayType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

        PopupWindowCompat.setWindowLayoutType(popupWindow, displayType)

        if (activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()) {
            popupWindow.showAtLocation(
                activity.window.decorView.rootView,
                gravity,
                0,
                0
            )
            isViewShown.set(true)
        }
    }

    private fun teardown() {
        /*@formatter:off*/ Logger.i(TAG, "teardown(): ", "")
        /*@formatter:on*/
        OperationQueue.addUiOperation {
            if (this::parentLayout.isInitialized) {
                parentLayout.removeAllViews()
            }
            if (this::popupWindow.isInitialized) {
                try {
                    popupWindow.dismiss()
                } catch (e: Exception) {
                }
            }
            if (this::cardView.isInitialized) {
                cardView.removeAllViews()
            }
            if (this::webView.isInitialized) {
                webView.removeAllViews()
                webView.removeJavascriptInterface(JS_INTERFACE_NAME)
            }
            inAppMessagesController.reset()
            isViewShown.set(false)
        }
    }

    companion object {
        private val TAG: String = InAppMessagesViewImpl::class.java.simpleName

        private const val DELAY_UI_MS = 200L
        private const val DELAY_UI_ATTEMPTS = 150

        private const val MIME_TYPE = "text/html"
        private const val ENCODING = "base64"
        private const val JS_INTERFACE_NAME = "RetenoAndroidHandler"

        private fun dpToPx(dp: Int): Int {
            return (dp * Resources.getSystem().displayMetrics.density).toInt()
        }
    }
}