package com.reteno.core

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkManager
import com.reteno.core.di.ServiceLocator
import com.reteno.core.di.provider.RetenoConfigProvider
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
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
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


class RetenoImpl(
    val application: Application,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) : RetenoLifecycleCallbacks, Reteno, RetenoInternalFacade {

    private val initWaitCondition = CompletableDeferred<Unit>()
    private val anrWaitCondition = CompletableDeferred<Unit>()
    private val configProvider = RetenoConfigProvider(RetenoConfig())
    private val syncScope = CoroutineScope(mainDispatcher + SupervisorJob())

    //TODO make this property private
    val serviceLocator: ServiceLocator = ServiceLocator(application, configProvider)
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

    private var isStarted: Boolean = false

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
        syncScope.launch(mainDispatcher) {
            anrWaitCondition.await()
            applyConfig(config)
            initWaitCondition.complete(Unit)
        }
    }

    private suspend fun applyConfig(config: RetenoConfig) = withContext(mainDispatcher) {
        try {
            configProvider.setConfig(config)
            appLifecycleOwner.lifecycle.addObserver(appLifecycleController)
            contactController.checkIfDeviceRegistered()
            pauseInAppMessages(config.isPausedInAppMessages)
            pausePushInAppMessages(config.isPausedPushInAppMessages)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "init(): ", t)
            /*@formatter:on*/
        }
    }

    private inline fun awaitInit(crossinline operation: () -> Unit) {
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
            activityHelper.enableLifecycleCallbacks(application, this@RetenoImpl)
            syncScope.launch(ioDispatcher) {
                preventANR()
                anrWaitCondition.complete(Unit)
            }
        }
    }

    private fun preventANR() {
        runCatching {
            //Trick to wait for sharedPrefs initialization on background thread to prevent ANR
            serviceLocator.sharedPrefsManagerProvider.get().getEmail()
            //Init workmanager singleton instance
            WorkManager.getInstance(application)
        }.getOrElse {
            Logger.e(TAG, "preventANR(): ", it)
        }
    }

    override fun start(activity: Activity) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
        }
        /*@formatter:off*/ Logger.i(TAG, "start(): ", "activity = [", activity, "]")
        /*@formatter:on*/
        if (!isStarted) {
            isStarted = true
            fetchInAppMessages()
        }
    }

    override fun resume(activity: Activity) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
        }
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        try {
            contactController.checkIfDeviceRequestSentThisSession()
            sessionHandler.start()
            startScheduler()
            iamView.resume(activity)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "resume(): ", ex)
            /*@formatter:on*/
        }
    }

    override fun pause(activity: Activity) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
        }
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        try {
            sessionHandler.stop()
            stopPushScheduler()
            iamView.pause(activity)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "pause(): ", ex)
            /*@formatter:on*/
        }
    }

    override fun stop(activity: Activity) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
        }
        isStarted = false
        /*@formatter:off*/ Logger.i(TAG, "stop(): ", "activity = [", activity, "]")
        /*@formatter:on*/
    }

    private fun fetchInAppMessages() {
        iamController.getInAppMessages()
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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
    override fun setUserAttributes(externalUserId: String, user: User?) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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
            if (isStarted) {
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

    override fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun logEvent(event: Event) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun logEcommerceEvent(ecomEvent: EcomEvent) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun logScreenView(screenName: String) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun logRetenoEvent(event: RetenoLogEvent) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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
        awaitInit {
            if (!isOsVersionSupported()) {
                return@awaitInit
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

    override fun autoScreenTracking(config: ScreenTrackingConfig) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun updatePushPermissionStatus() = awaitInit {
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

    override fun pauseInAppMessages(isPaused: Boolean) = awaitInit {
        iamController.pauseInAppMessages(isPaused)
    }

    override fun pausePushInAppMessages(isPaused: Boolean) = awaitInit {
        iamView.pauseIncomingPushInApps(isPaused)
    }

    override fun setPushInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = awaitInit {
        iamView.setPauseBehaviour(behaviour)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) =
        awaitInit {
            iamView.setInAppLifecycleCallback(inAppLifecycleCallback)
        }

    override fun forcePushData() = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun onNewFcmToken(token: String) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun recordInteraction(id: String, status: InteractionStatus) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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

    override fun isDatabaseEmpty(): Boolean {
        if (!isOsVersionSupported()) {
            return true
        }
        val result = try {
            databaseManager.isDatabaseEmpty()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "isDatabaseEmpty(): ", ex)
            /*@formatter:on*/
            true
        }
        /*@formatter:off*/ Logger.i(TAG, "isDatabaseEmpty(): $result")
        /*@formatter:on*/
        return result
    }

    override fun initializeIamView(interactionId: String) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
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
        private val TAG: String = RetenoImpl::class.java.simpleName

        val instance: RetenoImpl
            get() = Reteno.instance as RetenoImpl

        fun swapInstance(instance: RetenoImpl) {
            Reteno.instanceInternal = instance
        }
    }
}