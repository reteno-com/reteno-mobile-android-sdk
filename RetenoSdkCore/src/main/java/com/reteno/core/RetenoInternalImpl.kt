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
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.internal.action.user.UserAttributesValidator
import com.reteno.core.internal.coroutine.retenoDispatchers
import com.reteno.core.internal.lifecycle.SdkLifecycle
import com.reteno.core.internal.log.LoggerRemoteLog
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.permission.AndroidPermissionChecker
import com.reteno.core.util.Procedure
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicBoolean


class RetenoInternalImpl internal constructor(
    override val application: Application,
    private val appLifecycleOwner: LifecycleOwner,
    private val infrastructure: FacadeInfrastructure
) : Reteno, RetenoInternalFacade, SdkLifecycle {

    constructor(
        application: Application,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    ) : this(
        application = application,
        appLifecycleOwner = appLifecycleOwner,
        infrastructure = SdkComponent(application, retenoDispatchers(mainDispatcher, ioDispatcher))
    )

    private val actions get() = infrastructure.actions

    override val appInbox: AppInbox by lazy { AppInboxImpl(actions, LoggerRemoteLog) }
    override val recommendation: Recommendation by lazy { RecommendationImpl(actions, LoggerRemoteLog) }

    private val receivedConfigFromUser = AtomicBoolean(false)

    override val isStarted: Boolean
        get() = actions.isStarted

    val isInitialized: Boolean
        get() = actions.isInitialized

    init {
        initSdk()
    }

    override fun setConfig(config: RetenoConfig) {
        if (!receivedConfigFromUser.compareAndSet(false, true)) return
        actions.applyUserConfig(config)
    }

    @Deprecated(
        "Deprecated API, use static function Reteno.initWithConfig() instead",
        replaceWith = ReplaceWith("Reteno.initWithConfig(config)")
    )
    override fun initWith(config: RetenoConfig) {
        Reteno.initWithConfig(config)
    }

    private fun initSdk() {
        appLifecycleOwner.lifecycle.addObserver(this@RetenoInternalImpl)
        actions.boot()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    override fun start() {
        actions.requestStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    override fun stop() {
        actions.requestStop()
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String) {
        setUserAttributes(externalUserId, null)
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String, user: User?) {
        UserAttributesValidator.requireValidExternalUserId(externalUserId)
        UserAttributesValidator.requireValidMarketId(user?.userAttributes?.marketId)
        actions.runAfterInit { actions.submitUserAttributes(externalUserId, user) }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setMultiAccountUserAttributes(externalUserId: String, user: User?) {
        UserAttributesValidator.requireValidExternalUserId(externalUserId)
        UserAttributesValidator.requireValidMarketId(user?.userAttributes?.marketId)
        actions.runAfterInit { actions.submitMultiAccountUserAttributes(externalUserId, user) }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous) {
        UserAttributesValidator.requireValidMarketId(userAttributes.marketId)
        actions.runAfterInit { actions.submitAnonymousUserAttributes(userAttributes) }
    }

    override fun logEvent(event: Event) = actions.runAfterInit {
        actions.submitEvent(event)
    }

    override fun logEcommerceEvent(ecomEvent: EcomEvent) = actions.runAfterInit {
        actions.submitEcomEvent(ecomEvent)
    }

    override fun logScreenView(screenName: String) = actions.runAfterInit {
        actions.submitScreenView(screenName)
    }

    override fun setLifecycleEventConfig(lifecycleTrackingOptions: LifecycleTrackingOptions) =
        actions.runAfterInit {
            actions.submitLifecycleEventConfig(lifecycleTrackingOptions)
        }

    override fun autoScreenTracking(config: ScreenTrackingConfig) = actions.runAfterInit {
        actions.submitAutoScreenTracking(config)
    }

    override fun updatePushPermissionStatus() = actions.runAfterInit {
        actions.submitPushPermissionStatus()
    }

    override fun pauseInAppMessages(isPaused: Boolean) = actions.runAfterInit {
        actions.submitInAppPause(isPaused)
    }

    override fun pausePushInAppMessages(isPaused: Boolean) = actions.runAfterInit {
        actions.submitPushInAppPause(isPaused)
    }

    override fun setPushInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = actions.runAfterInit {
        actions.submitPushInAppPauseBehaviour(behaviour)
    }

    override fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?) =
        actions.runAfterInit {
            actions.submitInAppLifecycleCallback(inAppLifecycleCallback)
        }

    override fun forcePushData() = actions.runAfterInit {
        actions.requestSync()
    }

    override fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour) = actions.runAfterInit {
        actions.submitInAppPauseBehaviour(behaviour)
    }

    override fun onNewFcmToken(token: String) {
        actions.runAfterInit { actions.submitFcmToken(token) }
    }

    override fun recordInteraction(id: String, status: InteractionStatus, forcePush: Boolean) {
        actions.submitPushInteraction(id, status)
    }

    override fun recordInteractionAction(id: String, action: InteractionAction, forcePush: Boolean) =
        actions.runAfterInit {
            actions.submitInteractionClick(id, action, forcePush)
        }

    override fun canPresentMessages(): Boolean = actions.canPresentMessages()

    override fun getDeviceId(): String = actions.deviceId

    override fun hasDataForSync(): Boolean = actions.hasDataForSync()

    override fun isActivityPresented(): Boolean = actions.isActivityPresented()

    override fun initializeIamView(interactionId: String) = actions.runAfterInit {
        actions.submitIamFromPush(interactionId)
    }

    override fun getDefaultNotificationChannel(): String = actions.defaultNotificationChannel

    override fun saveDefaultNotificationChannel(channel: String) = actions.runAfterInit {
        actions.submitDefaultNotificationChannel(channel)
    }

    override fun notificationsEnabled(enabled: Boolean) = actions.runAfterInit {
        actions.submitNotificationsEnabled(enabled)
    }

    override fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String) = actions.runAfterInit {
        actions.submitDeeplinkClick(linkWrapped)
    }

    override fun getDefaultNotificationChannelConfig(): Procedure<NotificationChannelCompat.Builder>? {
        return infrastructure.configuration.config?.defaultNotificationChannelConfig
    }

    override fun executeAfterInit(action: () -> Unit) = actions.runAfterInit(action)

    override fun setSessionDuration(duration: Long) = actions.runAfterInit {
        actions.submitSessionDuration(duration)
    }

    override suspend fun requestPermissionChecker(): AndroidPermissionChecker? {
        return infrastructure.permissions.requestChecker()
    }

    internal suspend fun backgroundSync(): Result<BackgroundSyncOutcome> = actions.backgroundSync()

    companion object {
        @Deprecated("Use Reteno.instance", ReplaceWith("Reteno.instance", "com.reteno.core.Reteno"))
        val instance: RetenoInternalImpl
            get() = Reteno.instance as RetenoInternalImpl
    }
}