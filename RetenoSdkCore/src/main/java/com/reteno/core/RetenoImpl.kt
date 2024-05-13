package com.reteno.core

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import com.reteno.core.di.ServiceLocator
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.domain.controller.ScreenTrackingController
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.*
import com.reteno.core.util.Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED
import com.reteno.core.util.Constants.BROADCAST_ACTION_RETENO_APP_RESUME
import com.reteno.core.view.iam.IamView
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class RetenoImpl(
    application: Application,
    config: RetenoConfig,
    private val asyncScope: CoroutineScope,
    private val delayInitialization: Boolean
) : RetenoLifecycleCallbacks, Reteno {

    init {
        /*@formatter:off*/ Logger.i(TAG, "RetenoImpl(): ", "context = [" , application , "]")
        /*@formatter:on*/
        Companion.application = application
    }

    private val configProvider = RetenoConfigProvider(config)
    val serviceLocator: ServiceLocator = ServiceLocator(application, configProvider)
    private val activityHelper: RetenoActivityHelper by lazy { serviceLocator.retenoActivityHelperProvider.get() }

    private val screenTrackingController: ScreenTrackingController by lazy { serviceLocator.screenTrackingControllerProvider.get() }
    private val contactController by lazy { serviceLocator.contactControllerProvider.get() }
    private val scheduleController by lazy { serviceLocator.scheduleControllerProvider.get() }
    private val eventController by lazy { serviceLocator.eventsControllerProvider.get() }
    private val iamController by lazy { serviceLocator.iamControllerProvider.get() }
    private val sessionHandler by lazy { serviceLocator.retenoSessionHandlerProvider.get() }
    private val appLifecycleController by lazy { serviceLocator.appLifecycleControllerProvider.get() }

    override val appInbox by lazy { serviceLocator.appInboxProvider.get() }
    override val recommendation by lazy { serviceLocator.recommendationProvider.get() }
    private val iamView: IamView by lazy { serviceLocator.iamViewProvider.get() }

    @Volatile
    private var initContinuation: Continuation<RetenoConfig>? = null
    private var initDeferred: Deferred<Unit>? = null

    val isInitialized: Boolean
        get() = initDeferred?.isCompleted == true || initDeferred == null

    init {
        initSdk(config)
    }

    @JvmOverloads
    constructor(
        application: Application,
        accessKey: String,
        config: RetenoConfig = RetenoConfig()
    ) : this(
        application = application,
        config = config.copy(accessKey = accessKey),
        asyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        delayInitialization = false
    )

    /**
     * This constructor is created for delayed initialization of deviceId, do not use it unless
     * you actually need to provide API key and config later on in the app */
    constructor(application: Application) : this(
        application = application,
        delayInitialization = true,
        config = RetenoConfig(),
        asyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )

    override fun start(activity: Activity) = awaitInit {
        if (!isOsVersionSupported()) {
            return@awaitInit
        }
        /*@formatter:off*/ Logger.i(TAG, "start(): ", "activity = [", activity, "]")
        /*@formatter:on*/
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
            appLifecycleController.start()
            startPushScheduler()
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
            appLifecycleController.stop()
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
        /*@formatter:off*/ Logger.i(TAG, "stop(): ", "activity = [", activity, "]")
        /*@formatter:on*/
    }

    private fun fetchInAppMessages() {
        iamController.getInAppMessages()
    }

    private fun sendAppResumeBroadcast() {
        val intent =
            Intent(BROADCAST_ACTION_RETENO_APP_RESUME).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val infoList = application.queryBroadcastReceivers(intent)
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                application.sendBroadcast(intent)
            }
        }
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

    override fun updatePushPermissionStatus() {
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

    override fun pauseInAppMessages(isPaused: Boolean) {
        iamController.pauseInAppMessages(isPaused)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) {
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

    override fun initWith(config: RetenoConfig) {
        if (initContinuation == null) throw IllegalStateException("RetenoSDK was already initialized")
        initContinuation?.resume(config)
        initContinuation = null
    }

    override fun onNewFcmToken(token: String) = awaitInit {
        contactController.onNewFcmToken(token)
    }

    private fun initSdk(config: RetenoConfig) {
        if (isOsVersionSupported()) {
            activityHelper.enableLifecycleCallbacks(this@RetenoImpl)
            if (delayInitialization) {
                initDeferred = asyncScope.async {
                    withContext(Dispatchers.Main) {
                        val result = suspendCoroutine {
                            initContinuation = it
                        }
                        start(result)
                    }
                }
            } else {
                asyncScope.launch {
                    start(config)
                }
            }
        }
    }

    private suspend fun start(config: RetenoConfig) {
        configProvider.setConfig(config)
        clearOldData()
        initMetadata()
        try {
            contactController.checkIfDeviceRegistered()
            sendAppResumeBroadcast()
            pauseInAppMessages(config.isPausedInAppMessages)
            fetchInAppMessages()
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "init(): ", t)
            /*@formatter:on*/
        }
    }

    private fun initMetadata() {
        appLifecycleController.initMetadata()
    }

    private fun clearOldData() {
        scheduleController.clearOldData()
    }

    private fun startPushScheduler() {
        scheduleController.startScheduler()
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

    private inline fun awaitInit(crossinline operation: () -> Unit) {
        if (initDeferred?.isCompleted == false) {
            asyncScope.launch(Dispatchers.Main) {
                initDeferred?.await()
                operation()
            }
        } else {
            operation()
        }
    }

    companion object {
        private val TAG: String = RetenoImpl::class.java.simpleName

        lateinit var application: Application
            private set
    }
}