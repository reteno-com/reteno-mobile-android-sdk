package com.reteno.core.view.iam

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
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
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.features.iam.IamJsPayload
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal class IamViewImpl(
    private val activityHelper: RetenoActivityHelper,
    private val iamController: IamController,
) : IamView {

    private val reteno by lazy {
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }
    private val interactionController by lazy {
        reteno.serviceLocator.interactionControllerProvider.get()
    }
    private val scheduleController by lazy {
        reteno.serviceLocator.scheduleControllerProvider.get()
    }

    private val iamShowScope = CoroutineScope(Dispatchers.Main.immediate)

    private val isViewShown = AtomicBoolean(false)

    private var interactionId: String? = null
    private var messageInstanceId: Long? = null
    private lateinit var parentLayout: FrameLayout
    private lateinit var popupWindow: PopupWindow
    private lateinit var cardView: CardView
    private lateinit var webView: WebView

    private var initViewOnResume = false

    private val retenoAndroidHandler: RetenoAndroidHandler = object : RetenoAndroidHandler() {
        override fun onMessagePosted(event: String?) {
            /*@formatter:off*/ Logger.i(TAG, "onMessagePosted(): ", "event = [", event, "]")
            /*@formatter:on*/
            try {
                val jsEvent: IamJsEvent = event?.fromJson<IamJsEvent>() ?: return
                when (jsEvent.type) {
                    IamJsEventType.WIDGET_INIT_FAILED,
                    IamJsEventType.WIDGET_RUNTIME_ERROR -> onWidgetInitFailed(jsEvent)

                    IamJsEventType.WIDGET_INIT_SUCCESS -> onWidgetInitSuccess()

                    IamJsEventType.CLICK,
                    IamJsEventType.OPEN_URL -> openUrl(jsEvent)

                    IamJsEventType.CLOSE_WIDGET -> closeWidget(jsEvent.payload)
                }
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.e(TAG, "exception onMessagePosted(): ", e)
                /*@formatter:on*/
            }
        }
    }

    private fun onWidgetInitSuccess() {
        /*@formatter:off*/ Logger.i(TAG, "onWidgetInitSuccess(): ", "")
        /*@formatter:on*/
        showIamPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
        messageInstanceId?.let { instanceId ->
            val newInteractionId = UUID.randomUUID().toString()
            interactionId = newInteractionId
            interactionController.onInAppInteraction(InAppInteraction.createOpened(newInteractionId, instanceId))
        }
    }

    private fun onWidgetInitFailed(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "onWidgetInitFailed(): ", "jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        iamController.widgetInitFailed(jsEvent)
        messageInstanceId?.let { instanceId ->
            val newInteractionId = UUID.randomUUID().toString()
            interactionId = newInteractionId
            interactionController.onInAppInteraction(InAppInteraction.createFailed(newInteractionId, instanceId, jsEvent.payload?.reason))
        }
    }

    private fun openUrl(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "openUrl(): ", "jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        interactionId?.let { interaction ->
            interactionController.onInteractionIamClick(
                interaction,
                InteractionAction(
                    jsEvent.type.name,
                    jsEvent.payload?.targetComponentId,
                    jsEvent.payload?.url
                )
            )
            scheduleController.forcePush()
        }

        jsEvent.payload?.let {
            val isCustomDataSent = tryHandleCustomData(it.url, it.customData)
            if (isCustomDataSent.not()) tryHandleUrl(jsEvent)
        }

        teardown()
    }

    private fun closeWidget(payload: IamJsPayload?) {
        /*@formatter:off*/ Logger.i(TAG, "closeWidget(): ", "payload = [", payload, "]")
        /*@formatter:on*/
        teardown()
    }

    override fun isViewShown(): Boolean {
        return isViewShown.get()
    }


    override fun initialize(interactionId: String) {
        this.interactionId = interactionId
        /*@formatter:off*/ Logger.i(TAG, "initialize(): ", "widgetId = [", interactionId, "]")
        /*@formatter:on*/
        try {
            try {
                if (isViewShown.get()) {
                    teardown()
                }

                OperationQueue.addUiOperation {
                    activityHelper.currentActivity.let { activity ->
                        if (activity != null) {
                            initViewOnResume = false
                            createIamInActivity(activity)
                        } else {
                            initViewOnResume = true
                        }
                    }
                    iamController.fetchIamFullHtml(interactionId)
                }
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.e(TAG, "initialize(): ", e)
                /*@formatter:on*/
            }
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "initialize(): ", e)
            /*@formatter:on*/
        }
    }

    override fun initialize(inAppMessage: InAppMessage) {
        if (isViewShown.get()) return
        /*@formatter:off*/ Logger.i(TAG, "initialize(): ", "inAppMessageId = [", inAppMessage.messageId, "], messageInstanceId = [", inAppMessage.messageInstanceId, "]")
        /*@formatter:on*/
        try {
            //teardown()
            OperationQueue.addUiOperation {
                activityHelper.currentActivity?.let {
                    createIamInActivity(it)
                }
                messageInstanceId = inAppMessage.messageInstanceId
                iamController.fetchIamFullHtml(inAppMessage.content)
            }
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "initialize(): ", e)
            /*@formatter:on*/
        }
    }

    override fun resume(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        if (isViewShown.get()) {
            showIamPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
            return
        }

        if (initViewOnResume) {
            createIamInActivity(activity)
            initViewOnResume = false
        }

        iamShowScope.launch {
            iamController.fullHtmlStateFlow.collect { result ->
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
        iamShowScope.coroutineContext.cancelChildren()
        if (isViewShown.get()) {
            popupWindow.dismiss()
        }
    }


    private fun showIamPopupWindowOnceReady(attempts: Int) {
        /*@formatter:off*/ Logger.i(TAG, "showIamPopupWindowOnceReady(): ", "attempts = [", attempts, "]")
        /*@formatter:on*/
        if (attempts < 0) {
            return
        }

        if (activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()) {
            OperationQueue.addUiOperation {
                activityHelper.currentActivity?.let(::showIamPopupWindow)
            }
        } else {
            OperationQueue.addOperationAfterDelay({
                showIamPopupWindowOnceReady(attempts - 1)
            }, DELAY_UI_MS)
        }
    }

    private fun createIamInActivity(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "createIamInActivity(): ", "activity = [", activity, "]")
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
        /*@formatter:off*/ Logger.i(TAG, "addCardViewToParentLayout(): ", "")
        /*@formatter:on*/
        parentLayout.clipChildren = false
        parentLayout.clipToPadding = false
        parentLayout.addView(cardView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private fun addWebViewToCardView() {
        /*@formatter:off*/ Logger.i(TAG, "addWebViewToCardView(): ", "")
        /*@formatter:on*/
        cardView.addView(webView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private fun uploadHtmlIntoWebView(fullHtml: String) {
        /*@formatter:off*/ Logger.i(TAG, "uploadHtmlIntoWebView(): ")
        /*@formatter:on*/
//        val encodedHtml = Base64.encodeToString(fullHtml.toByteArray(), Base64.NO_PADDING)
//        webView.loadData(encodedHtml, MIME_TYPE, ENCODING)
        webView.loadDataWithBaseURL("",fullHtml, MIME_TYPE,"UTF-8","")
        webView.addJavascriptInterface(retenoAndroidHandler, JS_INTERFACE_NAME)
    }

    private fun showIamPopupWindow(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "showIamPopupWindow(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        val gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        val displayType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

        PopupWindowCompat.setWindowLayoutType(popupWindow, displayType)

        if (activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()) {
            /*@formatter:off*/ Logger.i(TAG, "showIamPopupWindow(): ", "Start showing")
            /*@formatter:on*/
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
        iamController.reset()
        interactionId = null
        messageInstanceId = null

        OperationQueue.addUiOperation {
            if (this::parentLayout.isInitialized) {
                parentLayout.removeAllViews()
            }
            if (this::popupWindow.isInitialized) {
                try {
                    popupWindow.dismiss()
                } catch (e: Exception) {
                    /*@formatter:off*/ Logger.e(TAG, "teardown(): popupWindow.dismiss() ", e)
                    /*@formatter:on*/
                }
            }
            if (this::cardView.isInitialized) {
                cardView.removeAllViews()
            }
            if (this::webView.isInitialized) {
                webView.removeAllViews()
                webView.removeJavascriptInterface(JS_INTERFACE_NAME)
            }
            isViewShown.set(false)
        }
    }

    private fun tryHandleCustomData(url: String?, customData: Map<String, String>?): Boolean {
        val bundle = Bundle()
        bundle.putString("url", url)
        customData?.entries?.forEach { entry ->
            bundle.putString(entry.key, entry.value)
        }

        val intent =
            Intent(Constants.BROADCAST_ACTION_CUSTOM_INAPP_DATA).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        intent.putExtras(bundle)

        val infoList = RetenoImpl.application.queryBroadcastReceivers(intent)
        var customDataSent = false
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                RetenoImpl.application.sendBroadcast(intent)
                customDataSent = true
            }
        }

        return customDataSent
    }

    private fun tryHandleUrl(jsEvent: IamJsEvent) {
        jsEvent.payload?.url.takeUnless { it.isNullOrBlank() }?.let {
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
            deepLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                OperationQueue.addUiOperation {
                    activityHelper.currentActivity?.startActivity(deepLinkIntent)
                }
            } catch (e: Throwable) {
                Logger.e(TAG, "openUrl()", e)
            }
        }
    }

    companion object {
        private val TAG: String = IamViewImpl::class.java.simpleName

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