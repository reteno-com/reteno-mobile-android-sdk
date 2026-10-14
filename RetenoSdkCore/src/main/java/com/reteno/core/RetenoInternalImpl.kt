package com.reteno.core

import android.app.Application
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.di.SdkActions
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.appinbox.AppInbox
import com.reteno.core.features.appinbox.AppInboxImpl
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.features.recommendation.Recommendation
import com.reteno.core.features.recommendation.RecommendationImpl
import com.reteno.core.internal.action.user.UserAttributesValidator
import com.reteno.core.internal.coroutine.retenoDispatchers
import com.reteno.core.internal.lifecycle.SdkLifecycle
import com.reteno.core.internal.log.LoggerRemoteLog
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.permission.AndroidPermissionChecker
import com.reteno.core.permission.PermissionActivityDelegate
import com.reteno.core.util.Logger
import com.reteno.core.util.Procedure
import com.reteno.core.util.isOsVersionSupported
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


class RetenoInternalImpl internal constructor(
    override val application: Application,
    private val mainDispatcher: CoroutineDispatcher,
    private val appLifecycleOwner: LifecycleOwner,
    //TODO make this property private
    val serviceLocator: ServiceLocator,
    sdkActions: (ServiceLocator) -> SdkActions,
    private val isOsSupported: () -> Boolean
) : Reteno, RetenoInternalFacade, SdkLifecycle {

    constructor(
        application: Application,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    ) : this(
        application = application,
        mainDispatcher = mainDispatcher,
        appLifecycleOwner = appLifecycleOwner,
        serviceLocator = ServiceLocator(application, retenoDispatchers(mainDispatcher, ioDispatcher)),
        sdkActions = { it.actions },
        isOsSupported = ::isOsVersionSupported
    )

    private val syncScope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val actions by lazy { sdkActions(serviceLocator) }
    private val initGate get() = serviceLocator.initGate
    internal val backgroundSync by lazy { actions.backgroundSync }
    private val permissionDelegate = PermissionActivityDelegate(application)
    private val activityHelper: RetenoActivityHelper by lazy { serviceLocator.retenoActivityHelperProvider.get() }

    private val configRepository by lazy { serviceLocator.configRepositoryProvider.get() }
    private val databaseManager by lazy { serviceLocator.retenoDatabaseManagerProvider.get() }

    override val appInbox: AppInbox by lazy { AppInboxImpl(actions, LoggerRemoteLog, isOsSupported) }
    override val recommendation: Recommendation by lazy { RecommendationImpl(actions, LoggerRemoteLog, isOsSupported) }

    private val receivedConfigFromUser = AtomicBoolean(false)

    override val isStarted: Boolean
        get() = actions.isStarted

    val isInitialized: Boolean
        get() = initGate.isInitialized

    val isInitializedByUser: Boolean
        get() = initGate.isInitializedByUser

    init {
        initSdk()
    }

    override fun setConfig(config: RetenoConfig) {
        if (!receivedConfigFromUser.compareAndSet(false, true)) {
            Logger.i(TAG, "RetenoSDK was already initialized, skipping")
            return
        }
        if (!isOsSupported()) {
            serviceLocator.setConfig(config)
            return
        }
        actions.applyUserConfig(config)
    }

    @Deprecated(
        "Deprecated API, use static function Reteno.initWithConfig() instead",
        replaceWith = ReplaceWith("Reteno.initWithConfig(config)")
    )
    override fun initWith(config: RetenoConfig) {
        Reteno.initWithConfig(config)
    }

    private inline fun runAfterInit(crossinline operation: () -> Unit) {
        if (!isInitialized) {
            syncScope.launch(mainDispatcher) {
                initGate.awaitInit()
                operation()
            }
        } else {
            operation()
        }
    }

    private fun initSdk() {
        if (isOsSupported()) {
            activityHelper.enableLifecycleCallbacks(application)
            appLifecycleOwner.lifecycle.addObserver(this@RetenoInternalImpl)
            actions.boot()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    override fun start() {
        if (!isOsSupported()) return
        actions.requestStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    override fun stop() {
        if (!isOsSupported()) return
        actions.requestStop()
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String) {
        setUserAttributes(externalUserId, null)
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String, user: User?) {
        if (!isOsSupported()) return
        UserAttributesValidator.requireValidExternalUserId(externalUserId)
        UserAttributesValidator.requireValidMarketId(user?.userAttributes?.marketId)
        runAfterInit { actions.submitUserAttributes(externalUserId, user) }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setMultiAccountUserAttributes(externalUserId: String, user: User?) {
        if (!isOsSupported()) return
        UserAttributesValidator.requireValidExternalUserId(externalUserId)
        UserAttributesValidator.requireValidMarketId(user?.userAttributes?.marketId)
        runAfterInit { actions.submitMultiAccountUserAttributes(externalUserId, user) }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous) {
        if (!isOsSupported()) return
        UserAttributesValidator.requireValidMarketId(userAttributes.marketId)
        runAfterInit { actions.submitAnonymousUserAttributes(userAttributes) }
    }

    override fun logEvent(event: Event) = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        actions.submitEvent(event)
    }

    override fun logEcommerceEvent(ecomEvent: EcomEvent) = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        actions.submitEcomEvent(ecomEvent)
    }

    override fun logScreenView(screenName: String) = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        actions.submitScreenView(screenName)
    }

    override fun setLifecycleEventConfig(lifecycleTrackingOptions: LifecycleTrackingOptions) =
        runAfterInit {
            if (!isOsSupported()) {
                return@runAfterInit
            }
            actions.submitLifecycleEventConfig(lifecycleTrackingOptions)
        }

    override fun autoScreenTracking(config: ScreenTrackingConfig) = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        actions.submitAutoScreenTracking(config)
    }

    override fun updatePushPermissionStatus() = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitPushPermissionStatus()
    }

    override fun pauseInAppMessages(isPaused: Boolean) = runAfterInit {
        actions.submitInAppPause(isPaused)
    }

    override fun pausePushInAppMessages(isPaused: Boolean) = runAfterInit {
        Logger.i(TAG, "pausePushInAppMessages(): isPaused = [$isPaused]")
        if (!isOsSupported()) return@runAfterInit
        actions.submitPushInAppPause(isPaused)
    }

    override fun setPushInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
        Logger.i(TAG, "setPushInAppMessagesPauseBehaviour(): behaviour = [$behaviour]")
        if (!isOsSupported()) return@runAfterInit
        actions.submitPushInAppPauseBehaviour(behaviour)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) =
        runAfterInit {
            Logger.i(
                TAG,
                "setInAppLifecycleCallback(): inAppLifecycleCallback = [$inAppLifecycleCallback]"
            )
            if (!isOsSupported()) return@runAfterInit
            actions.submitInAppLifecycleCallback(inAppLifecycleCallback)
        }

    override fun forcePushData() = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        /*@formatter:off*/ Logger.i(TAG, "forcePushData(): ", "")
        /*@formatter:on*/
        try {
            actions.requestSync()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "forcePushData(): ", ex)
            /*@formatter:on*/
        }
    }

    override fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
        if (!isOsSupported()) {
            return@runAfterInit
        }
        actions.submitInAppPauseBehaviour(behaviour)
    }

    override fun onNewFcmToken(token: String) {
        if (!isOsSupported()) return
        runAfterInit { actions.submitFcmToken(token) }
    }

    override fun recordInteraction(id: String, status: InteractionStatus, forcePush: Boolean) {
        if (!isOsSupported()) return
        actions.submitPushInteraction(id, status)
    }

    override fun recordInteractionAction(id: String, action: InteractionAction, forcePush: Boolean) = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitInteractionClick(id, action, forcePush)
    }

    override fun canPresentMessages(): Boolean {
        if (!isOsSupported()) {
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
        if (!isOsSupported()) {
            return ""
        }
        val result = try {
            configRepository.getDeviceId().id
        } catch (ex: Throwable) {
            if (isInitialized) {
                /*@formatter:off*/ Logger.e(TAG, "getDeviceId(): ", ex)
                /*@formatter:on*/
            } else {
                Log.e(TAG, "getDeviceId(): ", ex)
            }
            ""
        }
        /*@formatter:off*/ Logger.i(TAG, "getDeviceId(): $result")
        /*@formatter:on*/
        return result
    }

    override fun hasDataForSync(): Boolean {
        if (!isOsSupported()) {
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
        if (!isOsSupported()) return@runAfterInit
        actions.submitIamFromPush(interactionId)
    }

    override fun getDefaultNotificationChannel(): String {
        return configRepository.getDefaultNotificationChannel()
    }

    override fun saveDefaultNotificationChannel(channel: String) = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitDefaultNotificationChannel(channel)
    }

    override fun notificationsEnabled(enabled: Boolean) = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitNotificationsEnabled(enabled)
    }

    override fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String) = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitDeeplinkClick(linkWrapped)
    }

    override fun getDefaultNotificationChannelConfig(): Procedure<NotificationChannelCompat.Builder>? {
        return serviceLocator.configuration.config?.defaultNotificationChannelConfig
    }

    override fun executeAfterInit(action: () -> Unit) = runAfterInit {
        action()
    }

    override fun setSessionDuration(duration: Long) = runAfterInit {
        if (!isOsSupported()) return@runAfterInit
        actions.submitSessionDuration(duration)
    }

    override suspend fun requestPermissionChecker(): AndroidPermissionChecker? {
        return permissionDelegate.requestChecker()
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