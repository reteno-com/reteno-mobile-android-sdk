package com.reteno.core.di

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.DeviceIdentity
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInboxImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDeviceImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEventsImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteractionImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessagesImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteractionImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEventImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEventsImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUserImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLinksImpl
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.data.remote.api.RestClientImpl
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ConfigRepositoryImpl
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.config.SdkConfigurationImpl
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.retenoScope
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.SessionTracker
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.internal.platform.AndroidAppPlatform
import com.reteno.core.internal.platform.AndroidDeviceIdSources
import com.reteno.core.internal.platform.FirebaseFcmTokenSource
import com.reteno.core.internal.platform.lazyAppWorkManager
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoActivityHelperImpl
import com.reteno.core.lifecycle.RetenoSessionHandler
import java.util.UUID

internal interface SdkInfrastructure {
    val prefs: SharedPrefsManager
    val restConfig: DeviceIdentity
    val restClient: RestClient
    val configRepository: ConfigRepository
    val fileManager: FileManager
    val activityHelper: RetenoActivityHelper
    val sessionHandler: RetenoSessionHandler
    val databaseManager: RetenoDatabaseManager
    val devicesDb: RetenoDatabaseManagerDevice
    val usersDb: RetenoDatabaseManagerUser
    val interactionsDb: RetenoDatabaseManagerInteraction
    val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction
    val eventsDb: RetenoDatabaseManagerEvents
    val inboxDb: RetenoDatabaseManagerAppInbox
    val inAppMessagesDb: RetenoDatabaseManagerInAppMessages
    val recomEventsDb: RetenoDatabaseManagerRecomEvents
    val wrappedLinksDb: RetenoDatabaseManagerWrappedLink
    val logEventsDb: RetenoDatabaseManagerLogEvent
}

internal interface FacadeInfrastructure {
    val activityHelper: RetenoActivityHelper
    val configRepository: ConfigRepository
    val databaseManager: RetenoDatabaseManager
    val configuration: SdkConfiguration
    val actions: SdkActions
}

internal class SdkComponent(
    private val context: Context,
    private val dispatchers: RetenoDispatchers
) : SdkInfrastructure, FacadeInfrastructure {

    private val initGate by lazy { InitGate() }
    private val lifecycleEventFilter by lazy { LifecycleEventFilter(LifecycleTrackingOptions.DEFAULT) }
    override val configuration: SdkConfiguration by lazy { SdkConfigurationImpl(lifecycleEventFilter) }
    private val retenoConfig = { requireNotNull(configuration.config) }
    private val logSink by lazy { AndroidLogSink() }
    private val scope by lazy { retenoScope(dispatchers, logSink) }

    override val prefs by lazy { SharedPrefsManager(context) }
    private val deviceIdHelper by lazy { DeviceIdHelper(AndroidDeviceIdSources(context), prefs, retenoConfig) }
    override val restConfig by lazy { RestConfig(prefs, deviceIdHelper) }
    override val restClient: RestClient by lazy { RestClientImpl(restConfig, retenoConfig, logSink) }
    override val configRepository: ConfigRepository by lazy { ConfigRepositoryImpl(prefs, restConfig, FirebaseFcmTokenSource()) }
    override val fileManager by lazy { FileManager(context, dispatchers) }
    override val activityHelper: RetenoActivityHelper by lazy { RetenoActivityHelperImpl() }

    private val database by lazy { RetenoDatabaseImpl(context) }
    override val devicesDb: RetenoDatabaseManagerDevice by lazy { RetenoDatabaseManagerDeviceImpl(database) }
    override val usersDb: RetenoDatabaseManagerUser by lazy { RetenoDatabaseManagerUserImpl(database) }
    override val interactionsDb: RetenoDatabaseManagerInteraction by lazy {
        RetenoDatabaseManagerInteractionImpl(database)
    }
    override val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction by lazy {
        RetenoDatabaseManagerInAppInteractionImpl(database)
    }
    override val eventsDb: RetenoDatabaseManagerEvents by lazy { RetenoDatabaseManagerEventsImpl(database) }
    override val inboxDb: RetenoDatabaseManagerAppInbox by lazy { RetenoDatabaseManagerAppInboxImpl(database) }
    override val inAppMessagesDb: RetenoDatabaseManagerInAppMessages by lazy {
        RetenoDatabaseManagerInAppMessagesImpl(database)
    }
    override val recomEventsDb: RetenoDatabaseManagerRecomEvents by lazy {
        RetenoDatabaseManagerRecomEventsImpl(database)
    }
    override val wrappedLinksDb: RetenoDatabaseManagerWrappedLink by lazy {
        RetenoDatabaseManagerWrappedLinksImpl(database)
    }
    override val logEventsDb: RetenoDatabaseManagerLogEvent by lazy { RetenoDatabaseManagerLogEventImpl(database) }
    override val databaseManager: RetenoDatabaseManager by lazy {
        RetenoDatabaseManagerImpl(
            devicesDb,
            usersDb,
            interactionsDb,
            eventsDb,
            inboxDb,
            recomEventsDb,
            wrappedLinksDb,
            logEventsDb,
            inAppInteractionsDb
        )
    }

    override val sessionHandler: RetenoSessionHandler by lazy {
        SessionTracker(
            prefs = prefs,
            filter = lifecycleEventFilter,
            logEvent = actions.logEvent,
            scope = scope,
            clock = System::currentTimeMillis,
            newSessionId = { UUID.randomUUID().toString() }
        )
    }

    override val actions: ActionsLocator by lazy {
        ActionsLocator(
            infrastructure = this,
            dispatchers = dispatchers,
            logSink = logSink,
            scope = scope,
            initGate = initGate,
            configuration = configuration,
            platform = AndroidAppPlatform(
                context = context,
                workManager = lazyAppWorkManager(context),
                lifecycleOwner = ProcessLifecycleOwner.get(),
                activityHelper = activityHelper
            ),
            lifecycleEventFilter = lifecycleEventFilter,
            clock = System::currentTimeMillis
        ).also { it.installLogging() }
    }
}
