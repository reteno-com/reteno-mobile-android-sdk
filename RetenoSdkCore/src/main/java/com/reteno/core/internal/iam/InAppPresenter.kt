package com.reteno.core.internal.iam

import com.reteno.core.data.remote.mapper.fromJsonOrNull
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.features.iam.IamJsPayload
import com.reteno.core.features.iam.IamJsRawEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.iam.DisplayOffer
import com.reteno.core.internal.action.iam.InAppDisplay
import com.reteno.core.internal.action.iam.LoadInAppHtml
import com.reteno.core.internal.action.iam.LoadPushInAppHtml
import com.reteno.core.internal.action.iam.ReportWidgetFailure
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.view.iam.IamView
import com.reteno.core.view.iam.callback.InAppCloseAction
import com.reteno.core.view.iam.callback.InAppCloseData
import com.reteno.core.view.iam.callback.InAppData
import com.reteno.core.view.iam.callback.InAppErrorData
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import com.reteno.core.view.iam.callback.InAppSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class InAppPresenter(
    private val screen: InAppScreen,
    private val loadHtml: LoadInAppHtml,
    private val loadPushHtml: LoadPushInAppHtml,
    private val reportFailure: ReportWidgetFailure,
    private val interactions: InteractionSubmitter,
    private val remoteLog: RemoteLog,
    private val scope: CoroutineScope,
    private val dispatchers: RetenoDispatchers,
    private val newId: () -> String
) : IamView, InAppDisplay, InAppScreenEvents {

    @Volatile
    private var shown = false

    @Volatile
    private var callback: InAppLifecycleCallback? = null

    private var active = false
    private var pending: InAppMessage? = null
    private var current: InApp? = null
    private var held: InApp? = null
    private var waitingForStart: InApp? = null
    private var readiness: Job? = null
    private var pushPaused = false
    private var pauseBehaviour = InAppPauseBehaviour.POSTPONE_IN_APPS

    init {
        screen.attach(this)
    }

    override fun isViewShown(): Boolean = shown

    override suspend fun show(message: InAppMessage): DisplayOffer = withContext(dispatchers.main) {
        offer(message)
    }

    override fun initialize(inAppMessage: InAppMessage) = onMain {
        offer(inAppMessage)
    }

    override fun initialize(interactionId: String) = onMain {
        if (pushPaused && pauseBehaviour == InAppPauseBehaviour.SKIP_IN_APPS) return@onMain
        current?.let(::release)
        begin(PushInApp(interactionId))
    }

    override fun start() = onMain {
        active = true
        val waiting = waitingForStart
        if (waiting != null) {
            waitingForStart = null
            reveal(waiting)
        }
        offerPending()
    }

    override fun pause() = onMain {
        active = false
        stopWaitingForActivity()
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) {
        callback = inAppLifecycleCallback
    }

    override fun pauseIncomingPushInApps(isPaused: Boolean) = onMain {
        pushPaused = isPaused
        if (isPaused) return@onMain
        val inApp = held ?: return@onMain
        held = null
        revealWhenActive(inApp)
    }

    override fun setPauseBehaviour(behaviour: InAppPauseBehaviour) = onMain {
        pauseBehaviour = behaviour
        val inApp = held ?: return@onMain
        if (behaviour == InAppPauseBehaviour.SKIP_IN_APPS) {
            teardown(inApp)
        }
    }

    override fun onWidgetMessage(htmlId: String, message: String?) = onLoaded(htmlId) { inApp ->
        val event = readWidgetEvent(message) ?: return@onLoaded
        when (event.type) {
            IamJsEventType.WIDGET_INIT_SUCCESS -> onWidgetDisplayed(inApp, event.payload)
            IamJsEventType.WIDGET_INIT_FAILED,
            IamJsEventType.WIDGET_RUNTIME_ERROR -> onWidgetFailed(inApp, event)
            IamJsEventType.CLICK,
            IamJsEventType.OPEN_URL -> onLinkClicked(inApp, event)
            IamJsEventType.CLOSE_WIDGET -> close(inApp, InAppCloseAction.CLOSE_BUTTON)
        }
    }

    override fun onDismissed(htmlId: String) = onLoaded(htmlId) { inApp ->
        close(inApp, InAppCloseAction.DISMISSED)
    }

    override fun onActivityStarted() = onCurrent { inApp ->
        if (inApp.revealed) {
            presentWhenReady(inApp)
        }
    }

    override fun onActivityStopped() = onMain {
        screen.hide()
    }

    private fun offer(message: InAppMessage): DisplayOffer {
        if (current != null) return DisplayOffer.SKIPPED
        if (!active) {
            pending = message
            return DisplayOffer.WAITING_FOR_APP
        }
        begin(RuleInApp(message))
        return DisplayOffer.LOADING
    }

    private fun offerPending() {
        val message = pending ?: return
        if (!active || current != null) return
        pending = null
        begin(RuleInApp(message))
    }

    private fun begin(inApp: InApp) {
        current = inApp
        val loading = scope.launch(dispatchers.main, start = CoroutineStart.LAZY) {
            notifyHost("beforeDisplay") { it.beforeDisplay(inApp.data()) }
            val html = inApp.fetchHtml().getOrNull()
            if (html == null || !createScreen(inApp, html)) {
                teardown(inApp)
            }
        }
        inApp.loading = loading
        loading.start()
    }

    private fun createScreen(inApp: InApp, html: IamFetchResult): Boolean {
        inApp.htmlId = html.id
        return screen.create(html)
    }

    private fun onWidgetDisplayed(inApp: InApp, payload: IamJsPayload?) {
        resize(payload?.contentHeight)
        if (inApp.isPush && pushPaused) {
            holdPushInApp(inApp)
            return
        }
        revealWhenActive(inApp)
    }

    private fun holdPushInApp(inApp: InApp) {
        if (pauseBehaviour == InAppPauseBehaviour.SKIP_IN_APPS) {
            teardown(inApp)
            return
        }
        held = inApp
    }

    private fun revealWhenActive(inApp: InApp) {
        if (!active) {
            waitingForStart = inApp
            return
        }
        reveal(inApp)
    }

    private fun resize(contentHeight: String?) {
        if (contentHeight == null) return
        val heightDp = contentHeight.filter { it.isDigit() }.toIntOrNull() ?: return
        screen.resize(heightDp)
    }

    private fun reveal(inApp: InApp) {
        inApp.revealed = true
        presentWhenReady(inApp)
        notifyHost("onDisplay") { it.onDisplay(inApp.data()) }
        inApp.onRevealed()
    }

    private fun presentWhenReady(inApp: InApp) {
        stopWaitingForActivity()
        readiness = scope.launch(dispatchers.main) {
            repeat(READY_CHECK_ATTEMPTS) {
                if (screen.canPresent() && present(inApp)) {
                    return@launch
                }
                delay(READY_CHECK_DELAY_MILLIS)
            }
            if (!inApp.presented) {
                close(inApp, InAppCloseAction.DISMISSED)
            }
        }
    }

    private fun present(inApp: InApp): Boolean {
        val presented = screen.present()
        shown = presented
        if (presented) {
            inApp.presented = true
        }
        return presented
    }

    private fun onWidgetFailed(inApp: InApp, event: IamJsEvent) {
        notifyHost("onError") { it.onError(InAppErrorData(inApp.source, inApp.hostId, ERROR_MESSAGE)) }
        val tenantId = inApp.tenantId()
        scope.launch {
            reportFailure(tenantId, event)
        }
        inApp.onFailed(event.payload?.reason)
        teardown(inApp)
    }

    private fun onLinkClicked(inApp: InApp, event: IamJsEvent) {
        val payload = event.payload
        val interactionId = inApp.interactionId
        if (interactionId != null) {
            val action = InteractionAction(event.type.name, payload?.targetComponentId, payload?.url)
            interactions.submitInteractionClick(interactionId, action, forcePush = true)
        }
        if (payload != null && !sendCustomData(inApp, payload)) {
            openLink(payload.url)
        }
        teardown(inApp)
    }

    private fun sendCustomData(inApp: InApp, payload: IamJsPayload): Boolean {
        val customData = payload.customData
        if (customData.isNullOrEmpty()) return false
        val data = linkedMapOf<String, String>()
        val url = payload.url
        if (url != null) {
            data[KEY_URL] = url
        }
        data[KEY_SOURCE] = inApp.source.name
        data[KEY_ID] = inApp.customDataId
        data.putAll(customData)
        return screen.sendCustomData(data)
    }

    private fun openLink(url: String?) {
        if (url.isNullOrBlank()) return
        screen.openLink(url)
    }

    private fun close(inApp: InApp, action: InAppCloseAction) {
        val data = InAppCloseData(inApp.source, inApp.hostId, action)
        notifyHost("beforeClose") { it.beforeClose(data) }
        teardown(inApp)
        notifyHost("afterClose") { it.afterClose(data) }
    }

    private fun teardown(inApp: InApp) {
        release(inApp)
        offerPending()
    }

    private fun release(inApp: InApp) {
        inApp.loading.cancel()
        stopWaitingForActivity()
        current = null
        held = null
        waitingForStart = null
        shown = false
        screen.destroy()
    }

    private fun stopWaitingForActivity() {
        readiness?.cancel()
    }

    private fun notifyHost(callbackName: String, event: (InAppLifecycleCallback) -> Unit) {
        val host = callback ?: return
        runCatching {
            event(host)
        }.onFailure {
            remoteLog.error("In-app lifecycle callback $callbackName failed: ${it.javaClass.name}")
        }
    }

    private fun readWidgetEvent(message: String?): IamJsEvent? {
        val parsed = runCatching { message.orEmpty().fromJsonOrNull<IamJsRawEvent>() }.getOrNull() ?: return null
        val type = IamJsEventType.values().firstOrNull { it.name == parsed.type } ?: return null
        return IamJsEvent(type, parsed.payload)
    }

    private fun onMain(block: () -> Unit) {
        scope.launch(dispatchers.main) {
            block()
        }
    }

    private fun onCurrent(block: (InApp) -> Unit) = onMain {
        val inApp = current ?: return@onMain
        block(inApp)
    }

    private fun onLoaded(htmlId: String, block: (InApp) -> Unit) = onCurrent { inApp ->
        if (inApp.htmlId == htmlId) {
            block(inApp)
        }
    }

    private abstract class InApp {
        lateinit var loading: Job
        var htmlId: String? = null
        var revealed = false
        var presented = false
        var interactionId: String? = null
        abstract val source: InAppSource
        abstract val hostId: String
        abstract val customDataId: String
        abstract val isPush: Boolean

        abstract suspend fun fetchHtml(): Result<IamFetchResult>

        abstract fun onRevealed()

        abstract fun onFailed(reason: String?)

        fun tenantId(): String = interactionId ?: hostId

        fun data() = InAppData(source, hostId)
    }

    private inner class RuleInApp(private val message: InAppMessage) : InApp() {
        override val source = InAppSource.DISPLAY_RULES
        override val hostId = message.messageInstanceId.toString()
        override val customDataId = message.messageId.toString()
        override val isPush = false

        override suspend fun fetchHtml() = loadHtml(message)

        override fun onRevealed() {
            interactions.submitInAppShown(message)
            val opened = newId()
            interactionId = opened
            interactions.submitInAppInteraction(InAppInteraction.createOpened(opened, message.messageInstanceId))
        }

        override fun onFailed(reason: String?) {
            interactions.submitInAppInteraction(
                InAppInteraction.createFailed(newId(), message.messageInstanceId, reason)
            )
        }
    }

    private inner class PushInApp(pushInteractionId: String) : InApp() {
        override val source = InAppSource.PUSH_NOTIFICATION
        override val hostId = pushInteractionId
        override val customDataId = pushInteractionId
        override val isPush = true

        init {
            interactionId = pushInteractionId
        }

        override suspend fun fetchHtml() = loadPushHtml(hostId)

        override fun onRevealed() = Unit

        override fun onFailed(reason: String?) = Unit
    }

    internal companion object {
        const val READY_CHECK_DELAY_MILLIS = 200L
        const val READY_CHECK_ATTEMPTS = 150
        private const val ERROR_MESSAGE = "Failed to load In-App message."
        private const val KEY_URL = "url"
        private const val KEY_SOURCE = "inapp_source"
        private const val KEY_ID = "inapp_id"
    }
}
