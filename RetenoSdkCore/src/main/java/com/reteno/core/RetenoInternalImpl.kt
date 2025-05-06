package com.reteno.core

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ScreenTrackingController
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.*
import com.reteno.core.util.Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED
import com.reteno.core.view.iam.IamView
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


class RetenoInternalImpl(
    val application: Application,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) : Reteno, RetenoInternalFacade {

    private val initWaitCondition = CompletableDeferred<Unit>()
    private val anrWaitCondition = CompletableDeferred<Unit>()
    private val syncScope = CoroutineScope(mainDispatcher + SupervisorJob())

    //TODO make this property private
    val serviceLocator: ServiceLocator = ServiceLocator(application)
    private val activityHelper: RetenoActivityHelper by lazy { serviceLocator.retenoActivityHelperProvider.get() }

    private val screenTrackingController: ScreenTrackingController by lazy { serviceLocator.screenTrackingControllerProvider.get() }
    private val contactController by lazy { serviceLocator.contactControllerProvider.get() }
    private val scheduleController by lazy { serviceLocator.scheduleControllerProvider.get() }
    private val eventController by lazy { serviceLocator.eventsControllerProvider.get() }
    private val iamController by lazy { serviceLocator.iamControllerProvider.get() }
    private val sessionHandler by lazy { serviceLocator.retenoSessionHandlerProvider.get() }
    private val appLifecycleController by lazy { serviceLocator.appLifecycleControllerProvider.get() }
    private val interactionController by lazy { serviceLocator.interactionControllerProvider.get() }
    private val databaseManager by lazy { serviceLocator.retenoDatabaseManagerProvider.get() }
    private val deeplinkController by lazy { serviceLocator.deeplinkControllerProvider.get() }

    override val appInbox by lazy { serviceLocator.appInboxProvider.get() }
    override val recommendation by lazy { serviceLocator.recommendationProvider.get() }
    private val iamView: IamView by lazy { serviceLocator.iamViewProvider.get() }

    private var isStarted: AtomicBoolean = AtomicBoolean(false)

    val isInitialized: Boolean
        get() = initWaitCondition.isCompleted

    init {
        initSdk()
    }

    override fun setConfig(config: RetenoConfig) {
        if (isInitialized) {
            Logger.i(TAG, "RetenoSDK was already initialized, skipping")
            return
        }
        Logger.i(TAG, "setConfig()")
        serviceLocator.setConfig(config)
        syncScope.launch(mainDispatcher) {
            anrWaitCondition.await()
            applyConfig(config)
            initWaitCondition.complete(Unit)
        }
    }

    @Deprecated(
        "Deprecated API, use static function Reteno.initWithConfig() instead",
        replaceWith = ReplaceWith("Reteno.initWithConfig(config)")
    )
    override fun initWith(config: RetenoConfig) {
        Reteno.initWithConfig(config)
    }

    private suspend fun applyConfig(config: RetenoConfig) = withContext(mainDispatcher) {
        try {
            appLifecycleOwner.lifecycle.addObserver(appLifecycleController)
            contactController.checkIfDeviceRegistered()
            pauseInAppMessages(config.isPausedInAppMessages)
            pausePushInAppMessages(config.isPausedPushInAppMessages)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "init(): ", t)
            /*@formatter:on*/
        }
    }

    private inline fun runAfterInit(crossinline operation: () -> Unit) {
        if (!isInitialized) {
            syncScope.launch(mainDispatcher) {
                initWaitCondition.await()
                operation()
            }
        } else {
            operation()
        }
    }

    private fun initSdk() {
        if (isOsVersionSupported()) {
            activityHelper.enableLifecycleCallbacks(application)
            appLifecycleOwner.lifecycle.addObserver(this@RetenoInternalImpl)
            syncScope.launch(ioDispatcher) {
                preventANR()
                anrWaitCondition.complete(Unit)
            }
        }
    }

    private fun preventANR() {
        runCatching {
            //Init workmanager singleton instance
            serviceLocator.initWorkManager()
            //Trick to wait for sharedPrefs initialization on background thread to prevent ANR
            serviceLocator.sharedPrefsManagerProvider.get().getEmail()
        }.getOrElse {
            Logger.e(TAG, "preventANR(): ", it)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "start(): ")
        /*@formatter:on*/
        if (!isStarted.getAndSet(true)) {
            iamController.getInAppMessages()
        }
        try {
            contactController.checkIfDeviceRequestSentThisSession()
            sessionHandler.start()
            startScheduler()
            iamView.start()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "start(): ", ex)
            /*@formatter:on*/
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "stop(): ")
        /*@formatter:on*/
        isStarted.set(false)
        try {
            sessionHandler.stop()
            stopPushScheduler()
            iamView.pause()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "stop(): ", ex)
            /*@formatter:on*/
        }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        if (externalUserId.isBlank()) {
            val exception = IllegalArgumentException("externalUserId should not be null or blank")
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): ", exception)
            /*@formatter:on*/
            throw exception
        }

        setUserAttributes(externalUserId, null)
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String, user: User?) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }

        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "], used = [" , user , "]")
        /*@formatter:on*/
        if (externalUserId.isBlank()) {
            val exception = IllegalArgumentException("externalUserId should not be null or blank")
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): ", exception)
            /*@formatter:on*/
            throw exception
        }

        try {
            contactController.setExternalIdAndUserData(externalUserId, user)
            if (isStarted.get()) {
                syncScope.launch {
                    delay(5000L) //There is a requirement to refresh segmentation in 5 sec after user change his attributes
                    iamController.refreshSegmentation()
                }
            }
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): externalUserId = [$externalUserId], user = [$user]", ex)
            /*@formatter:on*/
        }
    }

    override fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "setAnonymousUserAttributes(): ", "userAttributes = [", userAttributes, "]")
        /*@formatter:on*/
        try {
            contactController.setAnonymousUserAttributes(userAttributes)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "setAnonymousUserAttributes(): userAttributes = [$userAttributes]", ex)
            /*@formatter:on*/
        }
    }

    override fun logEvent(event: Event) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "logEvent(): ", "eventType = [" , event.eventTypeKey , "], date = [" , event.occurred , "], parameters = [" , event.params , "]")
        /*@formatter:on*/
        try {
            eventController.trackEvent(event)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logEvent(): event = [$event]", ex)
            /*@formatter:on*/
        }
    }

    override fun logEcommerceEvent(ecomEvent: EcomEvent) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "logEcommerceEvent(): ", "ecomEvent = [" , ecomEvent , "]")
        /*@formatter:on*/
        try {
            eventController.trackEcomEvent(ecomEvent)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logEcommerceEvent(): ecomEvent = [$ecomEvent]", ex)
            /*@formatter:on*/
        }
    }

    override fun logScreenView(screenName: String) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "logScreenView(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        try {
            eventController.trackScreenViewEvent(screenName)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logScreenView(): screenName = [$screenName]", ex)
            /*@formatter:on*/
        }
    }

    override fun logRetenoEvent(event: RetenoLogEvent) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "logRetenoEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/
        try {
            eventController.trackRetenoEvent(event)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logRetenoEvent(): event = [$event]", ex)
            /*@formatter:on*/
        }
    }

    override fun setLifecycleEventConfig(lifecycleTrackingOptions: LifecycleTrackingOptions) =
        runAfterInit {
            if (!isOsVersionSupported()) {
                return@runAfterInit
            }
            /*@formatter:off*/ Logger.i(TAG, "setLifecycleEventConfig(): ", "lifecycleEventConfig = [" , lifecycleTrackingOptions , "]")
        /*@formatter:on*/
            try {
                appLifecycleController.setLifecycleEventConfig(lifecycleTrackingOptions)
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "setLifecycleEventConfig(): lifecycleEventConfig = [$lifecycleTrackingOptions]", ex)
            /*@formatter:on*/
            }
        }

    override fun autoScreenTracking(config: ScreenTrackingConfig) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "autoScreenTracking(): ", "config = [" , config , "]")
        /*@formatter:on*/
        try {
            screenTrackingController.autoScreenTracking(config)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "autoScreenTracking(): config = [$config]", ex)
            /*@formatter:on*/
        }
    }

    override fun updatePushPermissionStatus() = runAfterInit {
        Logger.i(TAG, "updatePushPermissionStatus():")
        val intent =
            Intent(BROADCAST_ACTION_PUSH_PERMISSION_CHANGED).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val infoList = application.queryBroadcastReceivers(intent)
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                application.sendBroadcast(intent)
            }
        }
    }

    override fun pauseInAppMessages(isPaused: Boolean) = runAfterInit {
        Logger.i(TAG, "pauseInAppMessages(): isPaused = [$isPaused]")
        iamController.pauseInAppMessages(isPaused)
    }

    override fun pausePushInAppMessages(isPaused: Boolean) = runAfterInit {
        Logger.i(TAG, "pausePushInAppMessages(): isPaused = [$isPaused]")
        iamView.pauseIncomingPushInApps(isPaused)
    }

    override fun setPushInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
        Logger.i(TAG, "setPushInAppMessagesPauseBehaviour(): behaviour = [$behaviour]")
        iamView.setPauseBehaviour(behaviour)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) =
        runAfterInit {
            Logger.i(TAG, "setInAppLifecycleCallback(): inAppLifecycleCallback = [$inAppLifecycleCallback]")
            iamView.setInAppLifecycleCallback(inAppLifecycleCallback)
        }

    override fun forcePushData() = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "forcePushData(): ", "")
        /*@formatter:on*/
        try {
            scheduleController.forcePush()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "forcePushData(): ", ex)
            /*@formatter:on*/
        }
    }

    override fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "setInAppMessagesPauseBehaviour(): ", "behaviour = [" , behaviour , "]")
        /*@formatter:on*/
        try {
            iamController.setPauseBehaviour(behaviour)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "setInAppMessagesPauseBehaviour(): behaviour = [$behaviour]", ex)
            /*@formatter:on*/
        }
    }

    override fun onNewFcmToken(token: String) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        try {
            contactController.onNewFcmToken(token)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onNewFcmToken(): token = [$token]", ex)
            /*@formatter:on*/
        }
    }

    override fun recordInteraction(id: String, status: InteractionStatus) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "recordInteraction(): ", "status = [" , status , "]")
        /*@formatter:on*/
        try {
            interactionController.onInteraction(id, status)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "recordInteraction(): status = [$status]", ex)
            /*@formatter:on*/
        }
    }

    override fun canPresentMessages(): Boolean {
        if (!isOsVersionSupported()) {
            return false
        }
        /*@formatter:off*/ Logger.i(TAG, "canPresentMessages(): ")
        /*@formatter:on*/
        return try {
            activityHelper.canPresentMessages()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "canPresentMessages(): ", ex)
            /*@formatter:on*/
            false
        }
    }

    override fun getDeviceId(): String {
        if (!isOsVersionSupported()) {
            return ""
        }
        val result = try {
            contactController.getDeviceId()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "getDeviceId(): ", ex)
            /*@formatter:on*/
            ""
        }
        /*@formatter:off*/ Logger.i(TAG, "getDeviceId(): $result")
        /*@formatter:on*/
        return result
    }

    override fun hasDataForSync(): Boolean {
        if (!isOsVersionSupported()) {
            return true
        }
        val result = try {
            databaseManager.hasDataForSync()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "isDatabaseEmpty(): ", ex)
            /*@formatter:on*/
            true
        }
        /*@formatter:off*/ Logger.i(TAG, "isDatabaseEmpty(): $result")
        /*@formatter:on*/
        return result
    }

    override fun isActivityPresented(): Boolean {
        return activityHelper.currentActivity != null
    }

    override fun initializeIamView(interactionId: String) = runAfterInit {
        if (!isOsVersionSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "initializeIamView(): ", "interactionId = [" , interactionId , "]")
        /*@formatter:on*/
        try {
            iamView.initialize(interactionId)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "initializeIamView(): interactionId = [$interactionId]", ex)
            /*@formatter:on*/
        }
    }

    override fun getDefaultNotificationChannel(): String {
        return contactController.getDefaultNotificationChannel()
    }

    override fun saveDefaultNotificationChannel(channel: String) {
        contactController.saveDefaultNotificationChannel(channel)
    }

    override fun startScheduler() {
        scheduleController.startScheduler()
    }

    override fun notificationsEnabled(enabled: Boolean) {
        contactController.notificationsEnabled(enabled)
    }

    override fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String) {
        deeplinkController.deeplinkClicked(linkWrapped, linkUnwrapped)
    }

    override fun hasActiveTask(): Boolean {
        return activityHelper.hasActiveTask()
    }

    override fun getDefaultNotificationChannelConfig(): ((NotificationChannelCompat.Builder) -> Unit)? {
        return serviceLocator.currentConfig.defaultNotificationChannelConfig
    }

    override fun executeAfterInit(action: () -> Unit) = runAfterInit {
        action()
    }

    private fun stopPushScheduler() {
        scheduleController.stopScheduler()
    }

    /**
     * For testing purposes
     * DON'T EVER CALL THIS METHOD!
     */
    @Deprecated("DON'T EVER CALL THIS METHOD! It is for testing only")
    private fun testCrash() {
        try {
            throw NullPointerException("This is a test crash in SDK")
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "testCrash(): ", ex)
            /*@formatter:on*/
        }
    }

    companion object {
        private val TAG: String = RetenoInternalImpl::class.java.simpleName

        val instance: RetenoInternalImpl
            get() = Reteno.instance as RetenoInternalImpl

        fun swapInstance(instance: RetenoInternalImpl?) {
            Reteno.instanceInternal = instance
        }
    }
}