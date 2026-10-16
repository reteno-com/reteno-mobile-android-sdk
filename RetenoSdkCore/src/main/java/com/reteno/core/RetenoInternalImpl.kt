package com.reteno.core

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.di.FacadeInfrastructure
import com.reteno.core.di.SdkComponent
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
    private val infrastructure: FacadeInfrastructure,
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
        infrastructure = SdkComponent(application, retenoDispatchers(mainDispatcher, ioDispatcher)),
        isOsSupported = ::isOsVersionSupported
    )

    private val syncScope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val actions get() = infrastructure.actions
    internal val backgroundSync by lazy { actions.backgroundSync }
    private val permissionDelegate = PermissionActivityDelegate(application, mainDispatcher)
    private val activityHelper: RetenoActivityHelper get() = infrastructure.activityHelper

    private val configRepository get() = infrastructure.configRepository
    private val databaseManager get() = infrastructure.databaseManager

    override val appInbox: AppInbox by lazy { AppInboxImpl(lazy { actions }, LoggerRemoteLog, isOsSupported) }
    override val recommendation: Recommendation by lazy {
        RecommendationImpl(lazy { actions }, LoggerRemoteLog, isOsSupported)
    }

    private val receivedConfigFromUser = AtomicBoolean(false)

    override val isStarted: Boolean
        get() = isOsSupported() && actions.isStarted

    val isInitialized: Boolean
        get() = isOsSupported() && actions.isInitialized

    init {
        initSdk()
    }

    override fun setConfig(config: RetenoConfig) {
        if (!receivedConfigFromUser.compareAndSet(false, true)) return
        if (!isOsSupported()) {
            infrastructure.configuration.useUserConfig(config)
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
        if (!isOsSupported()) return
        if (!isInitialized) {
            syncScope.launch(mainDispatcher) {
                actions.awaitInit()
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
        actions.submitEvent(event)
    }

    override fun logEcommerceEvent(ecomEvent: EcomEvent) = runAfterInit {
        actions.submitEcomEvent(ecomEvent)
    }

    override fun logScreenView(screenName: String) = runAfterInit {
        actions.submitScreenView(screenName)
    }

    override fun setLifecycleEventConfig(lifecycleTrackingOptions: LifecycleTrackingOptions) =
        runAfterInit {
            actions.submitLifecycleEventConfig(lifecycleTrackingOptions)
        }

    override fun autoScreenTracking(config: ScreenTrackingConfig) = runAfterInit {
        actions.submitAutoScreenTracking(config)
    }

    override fun updatePushPermissionStatus() = runAfterInit {
        actions.submitPushPermissionStatus()
    }

    override fun pauseInAppMessages(isPaused: Boolean) = runAfterInit {
        actions.submitInAppPause(isPaused)
    }

    override fun pausePushInAppMessages(isPaused: Boolean) = runAfterInit {
        actions.submitPushInAppPause(isPaused)
    }

    override fun setPushInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
        actions.submitPushInAppPauseBehaviour(behaviour)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) =
        runAfterInit {
            actions.submitInAppLifecycleCallback(inAppLifecycleCallback)
        }

    override fun forcePushData() = runAfterInit {
        actions.requestSync()
    }

    override fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = runAfterInit {
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
        actions.submitInteractionClick(id, action, forcePush)
    }

    override fun canPresentMessages(): Boolean {
        if (!isOsSupported()) return false
        return activityHelper.canPresentMessages()
    }

    override fun getDeviceId(): String {
        if (!isOsSupported()) return ""
        return configRepository.getDeviceId().id
    }

    override fun hasDataForSync(): Boolean {
        if (!isOsSupported()) return true
        return try {
            databaseManager.hasDataForSync()
        } catch (e: Exception) {
            Logger.e(TAG, "hasDataForSync(): ", e)
            true
        }
    }

    override fun isActivityPresented(): Boolean {
        return activityHelper.currentActivity != null
    }

    override fun initializeIamView(interactionId: String) = runAfterInit {
        actions.submitIamFromPush(interactionId)
    }

    override fun getDefaultNotificationChannel(): String {
        return configRepository.getDefaultNotificationChannel()
    }

    override fun saveDefaultNotificationChannel(channel: String) = runAfterInit {
        actions.submitDefaultNotificationChannel(channel)
    }

    override fun notificationsEnabled(enabled: Boolean) = runAfterInit {
        actions.submitNotificationsEnabled(enabled)
    }

    override fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String) = runAfterInit {
        actions.submitDeeplinkClick(linkWrapped)
    }

    override fun getDefaultNotificationChannelConfig(): Procedure<NotificationChannelCompat.Builder>? {
        return infrastructure.configuration.config?.defaultNotificationChannelConfig
    }

    override fun executeAfterInit(action: () -> Unit) = runAfterInit {
        action()
    }

    override fun setSessionDuration(duration: Long) = runAfterInit {
        actions.submitSessionDuration(duration)
    }

    override suspend fun requestPermissionChecker(): AndroidPermissionChecker? {
        return permissionDelegate.requestChecker()
    }

    companion object {
        private const val TAG = "RetenoInternalImpl"

        @Deprecated("Use Reteno.instance", ReplaceWith("Reteno.instance", "com.reteno.core.Reteno"))
        val instance: RetenoInternalImpl
            get() = Reteno.instance as RetenoInternalImpl
    }
}