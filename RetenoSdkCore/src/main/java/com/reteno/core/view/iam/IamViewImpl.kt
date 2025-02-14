package com.reteno.core.view.iam

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import com.reteno.core.RetenoImpl
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.features.iam.IamJsPayload
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import com.reteno.core.view.iam.callback.InAppCloseAction
import com.reteno.core.view.iam.callback.InAppCloseData
import com.reteno.core.view.iam.callback.InAppData
import com.reteno.core.view.iam.callback.InAppErrorData
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import com.reteno.core.view.iam.callback.InAppSource
import com.reteno.core.view.iam.container.IamContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal class IamViewImpl(
    private val activityHelper: RetenoActivityHelper,
    private val iamController: IamController,
    private val interactionController: InteractionController,
    private val scheduleController: ScheduleController
) : IamView {
    private val iamShowScope = CoroutineScope(Dispatchers.Main.immediate)

    private val isViewShown = AtomicBoolean(false)

    private var inAppLifecycleCallback: InAppLifecycleCallback? = null

    private var inAppSource: InAppSource? = null
    private var interactionId: String? = null
    private var messageId: Long? = null
    private var messageInstanceId: Long? = null
    private var iamContainer: IamContainer? = null

    private var pauseIncomingPushInApps = AtomicBoolean(false)
    private var lastPushInteractionId: String? = null
    private var pauseBehaviour = InAppPauseBehaviour.POSTPONE_IN_APPS

    private val retenoAndroidHandler: RetenoAndroidHandler = object : RetenoAndroidHandler() {
        override fun onMessagePosted(event: String?) {
            /*@formatter:off*/ Logger.i(TAG, "onMessagePosted(): ", "event = [", event, "]")
            /*@formatter:on*/
            try {
                val jsEvent: IamJsEvent = event?.fromJson<IamJsEvent>() ?: return
                when (jsEvent.type) {
                    IamJsEventType.WIDGET_INIT_FAILED,
                    IamJsEventType.WIDGET_RUNTIME_ERROR -> onWidgetInitFailed(jsEvent)

                    IamJsEventType.WIDGET_INIT_SUCCESS -> {
                        val height = jsEvent.payload?.contentHeight
                        if (height != null) {
                            OperationQueue.addUiOperation {
                                val intHeight = height.filter { it.isDigit() }.toInt()
                                iamContainer?.onHeightDefined(dpToPx(intHeight))
                            }
                        }
                        onWidgetInitSuccess()
                    }

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

    override fun pauseIncomingPushInApps(isPaused: Boolean) {
        if (pauseIncomingPushInApps.getAndSet(isPaused) && !isPaused) {
            lastPushInteractionId?.let {
                if (pauseBehaviour == InAppPauseBehaviour.POSTPONE_IN_APPS) {
                    showIamPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
                }
                lastPushInteractionId = null
            }
        }
    }

    override fun setPauseBehaviour(behaviour: InAppPauseBehaviour) {
        pauseBehaviour = behaviour
        if (pauseBehaviour == InAppPauseBehaviour.SKIP_IN_APPS) {
            lastPushInteractionId = null
        }
    }

    private fun onWidgetInitSuccess() {
        /*@formatter:off*/ Logger.i(TAG, "onWidgetInitSuccess(): ", "")
        /*@formatter:on*/
        if (checkPauseState()) return
        showIamPopupWindowOnceReady(DELAY_UI_ATTEMPTS)
        inAppLifecycleCallback?.onDisplay(createInAppData())
        messageInstanceId?.let { instanceId ->
            val newInteractionId = UUID.randomUUID().toString()
            interactionId = newInteractionId
            interactionController.onInAppInteraction(
                InAppInteraction.createOpened(
                    newInteractionId,
                    instanceId
                )
            )
        }
    }

    private fun onWidgetInitFailed(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "onWidgetInitFailed(): ", "jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        inAppLifecycleCallback?.onError(createInAppErrorData())
        val tenantId = (interactionId ?: messageInstanceId?.toString()).orEmpty()
        iamController.widgetInitFailed(tenantId, jsEvent)
        messageInstanceId?.let { instanceId ->
            val newInteractionId = UUID.randomUUID().toString()
            interactionId = newInteractionId
            interactionController.onInAppInteraction(
                InAppInteraction.createFailed(
                    newInteractionId,
                    instanceId,
                    jsEvent.payload?.reason
                )
            )
        }
        teardown()
    }

    private fun openUrl(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "openUrl(): ", "interactionId = [", interactionId, "], jsEvent = [", jsEvent, "]")
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
        inAppLifecycleCallback?.beforeClose(createInAppCloseData(InAppCloseAction.CLOSE_BUTTON))
        teardown()
        inAppLifecycleCallback?.afterClose(createInAppCloseData(InAppCloseAction.CLOSE_BUTTON))
    }

    override fun isViewShown(): Boolean {
        return isViewShown.get()
    }


    override fun initialize(interactionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "initialize(): ", "widgetId = [", interactionId, "]")
        /*@formatter:on*/
        try {
            try {
                if (pauseIncomingPushInApps.get() && pauseBehaviour == InAppPauseBehaviour.SKIP_IN_APPS) {
                    return
                }
                if (isViewShown.get()) {
                    teardown()
                }
                this.interactionId = interactionId
                inAppSource = InAppSource.PUSH_NOTIFICATION
                messageId = null
                messageInstanceId = null

                OperationQueue.addUiOperation {
                    inAppLifecycleCallback?.beforeDisplay(createInAppData())
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
            inAppMessage.notifyShown()
            iamController.updateInAppMessage(inAppMessage)
            OperationQueue.addUiOperation {
                messageId = inAppMessage.messageId
                messageInstanceId = inAppMessage.messageInstanceId
                inAppSource = InAppSource.DISPLAY_RULES
                interactionId = null
                inAppLifecycleCallback?.beforeDisplay(createInAppData())
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

        iamController.inAppMessagesFlow
            .onEach { initialize(it) }
            .launchIn(iamShowScope)

        iamShowScope.launch {
            iamController.fullHtmlStateFlow.collect { result ->
                ensureActive()
                if (result is ResultDomain.Success) {
                    createIamContainer(result)
                }
            }
        }
    }

    private fun createIamContainer(result: ResultDomain.Success<IamFetchResult>) {
        iamContainer = IamContainer.create(
            context = RetenoImpl.instance.application,
            jsInterface = retenoAndroidHandler,
            iamFetchResult = result.body,
            dismissListener = {
                inAppLifecycleCallback?.beforeClose(createInAppCloseData(InAppCloseAction.DISMISSED))
                teardown()
                inAppLifecycleCallback?.afterClose(createInAppCloseData(InAppCloseAction.DISMISSED))
            }
        )
    }

    override fun pause(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        iamShowScope.coroutineContext.cancelChildren()
        if (isViewShown.get()) {
            iamContainer?.dismiss()
        }
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) {
        this.inAppLifecycleCallback = inAppLifecycleCallback
    }

    private fun checkPauseState(): Boolean {
        return pauseIncomingPushInApps.get().also {
            if (it) {
                lastPushInteractionId = interactionId
            }
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
                activityHelper.currentActivity?.let(::showIamContainer)
            }
        } else {
            OperationQueue.addOperationAfterDelay({
                showIamPopupWindowOnceReady(attempts - 1)
            }, DELAY_UI_MS)
        }
    }

    private fun showIamContainer(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "showIamPopupWindow(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        if (activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()) {
            iamContainer?.show(activity)
            isViewShown.set(true)
        }
    }

    private fun teardown() {
        /*@formatter:off*/ Logger.i(TAG, "teardown(): ", "")
        /*@formatter:on*/
        iamController.reset()
        interactionId = null
        messageId = null
        messageInstanceId = null
        OperationQueue.addUiOperation {
            iamContainer?.destroy()
            iamContainer = null
            isViewShown.set(false)
        }
    }

    private fun tryHandleCustomData(url: String?, customData: Map<String, String>?): Boolean {
        if (customData.isNullOrEmpty()) return false
        val bundle = Bundle()
        bundle.putString("url", url)
        inAppSource?.let { source ->
            bundle.putString("inapp_source", source.name)
            if (source == InAppSource.PUSH_NOTIFICATION) {
                interactionId?.let { bundle.putString("inapp_id", it) }
            } else {
                messageId?.let { bundle.putString("inapp_id", it.toString()) }
            }

        }

        customData.entries.forEach { entry ->
            bundle.putString(entry.key, entry.value)
        }

        val intent =
            Intent(Constants.BROADCAST_ACTION_CUSTOM_INAPP_DATA).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        intent.putExtras(bundle)

        val infoList = RetenoImpl.instance.application.queryBroadcastReceivers(intent)
        var customDataSent = false
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                RetenoImpl.instance.application.sendBroadcast(intent)
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

    private fun createInAppData(): InAppData {
        return when (inAppSource) {
            InAppSource.PUSH_NOTIFICATION -> InAppData(InAppSource.PUSH_NOTIFICATION, interactionId)
            InAppSource.DISPLAY_RULES -> InAppData(
                InAppSource.DISPLAY_RULES,
                messageInstanceId?.toString()
            )

            else -> InAppData(InAppSource.DISPLAY_RULES, messageInstanceId?.toString())
        }
    }

    private fun createInAppCloseData(closeAction: InAppCloseAction): InAppCloseData {
        return when (inAppSource) {
            InAppSource.PUSH_NOTIFICATION -> InAppCloseData(
                InAppSource.PUSH_NOTIFICATION,
                interactionId,
                closeAction
            )

            InAppSource.DISPLAY_RULES -> InAppCloseData(
                InAppSource.DISPLAY_RULES,
                messageInstanceId?.toString(),
                closeAction
            )

            else -> InAppCloseData(
                InAppSource.DISPLAY_RULES,
                messageInstanceId?.toString(),
                closeAction
            )
        }
    }

    private fun createInAppErrorData(): InAppErrorData {
        return when (inAppSource) {
            InAppSource.PUSH_NOTIFICATION -> InAppErrorData(
                InAppSource.PUSH_NOTIFICATION,
                interactionId,
                ERROR_MESSAGE
            )

            InAppSource.DISPLAY_RULES -> InAppErrorData(
                InAppSource.DISPLAY_RULES,
                messageInstanceId?.toString(),
                ERROR_MESSAGE
            )

            else -> InAppErrorData(
                InAppSource.DISPLAY_RULES,
                messageInstanceId?.toString(),
                ERROR_MESSAGE
            )
        }
    }

    companion object {
        private val TAG: String = IamViewImpl::class.java.simpleName

        private const val DELAY_UI_MS = 200L
        private const val DELAY_UI_ATTEMPTS = 150

        private const val ENCODING = "base64"
        internal const val JS_INTERFACE_NAME = "RetenoAndroidHandler"

        private const val ERROR_MESSAGE = "Failed to load In-App message."

        fun dpToPx(dp: Int): Int {
            return (dp * Resources.getSystem().displayMetrics.density).toInt()
        }
    }
}