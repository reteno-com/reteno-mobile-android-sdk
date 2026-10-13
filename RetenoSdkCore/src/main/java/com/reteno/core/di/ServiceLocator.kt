package com.reteno.core.di

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.base.Provider
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.DeviceIdHelperProvider
import com.reteno.core.di.provider.InteractionCacheProvider
import com.reteno.core.di.provider.RestConfigProvider
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.WorkManagerProvider
import com.reteno.core.di.provider.controller.AppInboxControllerProvider
import com.reteno.core.di.provider.controller.DeeplinkControllerProvider
import com.reteno.core.di.provider.controller.IamControllerProvider
import com.reteno.core.di.provider.controller.InteractionControllerProvider
import com.reteno.core.di.provider.controller.RecommendationControllerProvider
import com.reteno.core.di.provider.database.DatabaseProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerAppInboxProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerDeviceProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerEventsProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppInteractionProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInAppMessagesProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerInteractionProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerLogEventProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerRecomEventsProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerUserProvider
import com.reteno.core.di.provider.database.RetenoDatabaseManagerWrappedLinkProvider
import com.reteno.core.di.provider.features.AppInboxProvider
import com.reteno.core.di.provider.features.IamViewProvider
import com.reteno.core.di.provider.features.RecommendationProvider
import com.reteno.core.di.provider.features.RetenoSessionHandlerProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import com.reteno.core.di.provider.network.RestClientProvider
import com.reteno.core.di.provider.repository.AppInboxRepositoryProvider
import com.reteno.core.di.provider.repository.ConfigRepositoryProvider
import com.reteno.core.di.provider.repository.DeeplinkRepositoryProvider
import com.reteno.core.di.provider.repository.FileManagerProvider
import com.reteno.core.di.provider.repository.IamRepositoryProvider
import com.reteno.core.di.provider.repository.InteractionRepositoryProvider
import com.reteno.core.di.provider.repository.RecommendationRepositoryProvider
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.action.sync.DataSync
import com.reteno.core.internal.config.SdkConfigurationImpl
import com.reteno.core.internal.coroutine.AndroidRetenoDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.retenoScope
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.internal.log.LoggerRemoteLog
import com.reteno.core.internal.platform.AndroidAppPlatform
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.Dispatchers

class ServiceLocator internal constructor(
    context: Context,
    private val dispatchers: RetenoDispatchers
) {

    constructor(context: Context) : this(context, AndroidRetenoDispatchers)

    internal val actions: ActionsLocator by lazy {
        val logSink = AndroidLogSink()
        ActionsLocator(
            serviceLocator = this,
            dispatchers = dispatchers,
            logSink = logSink,
            scope = retenoScope(dispatchers, logSink),
            initGate = initGate,
            configuration = configuration,
            platform = AndroidAppPlatform(
                context = context,
                workManager = workManagerProvider,
                lifecycleOwner = ProcessLifecycleOwner.get(),
                activityHelper = retenoActivityHelperProvider.get()
            ),
            lifecycleEventFilter = lifecycleEventFilter,
            remoteLog = LoggerRemoteLog,
            clock = System::currentTimeMillis
        )
    }
    private val dataSync: () -> DataSync = { actions.dataSync }
    internal val lifecycleEventFilter = LifecycleEventFilter(LifecycleTrackingOptions.DEFAULT)
    internal val initGate = InitGate()

    private val configProvider = RetenoConfigProvider()
    internal val configuration = SdkConfigurationImpl(configProvider, lifecycleEventFilter)
    private val retenoActivityHelperProviderInternal: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider()
    val retenoActivityHelperProvider: ProviderWeakReference<RetenoActivityHelper>
        get() = retenoActivityHelperProviderInternal

    internal val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider(context)
    internal val workManagerProvider: WorkManagerProvider = WorkManagerProvider(context)

    private val deviceIdHelperProvider: DeviceIdHelperProvider =
        DeviceIdHelperProvider(context, sharedPrefsManagerProvider, configProvider)
    internal val restConfigProvider: RestConfigProvider =
        RestConfigProvider(sharedPrefsManagerProvider, deviceIdHelperProvider)
    internal val restClientProvider: RestClientProvider =
        RestClientProvider(restConfigProvider, configProvider)

    private val apiClientProvider: ApiClientProvider = ApiClientProvider(restClientProvider)
    private val databaseProvider: DatabaseProvider = DatabaseProvider(context)

    /** DatabaseManagerProviders **/
    private val retenoDatabaseManagerDeviceProviderInternal =
        RetenoDatabaseManagerDeviceProvider(databaseProvider)
    val retenoDatabaseManagerDeviceProvider: ProviderWeakReference<RetenoDatabaseManagerDevice>
        get() = retenoDatabaseManagerDeviceProviderInternal

    private val retenoDatabaseManagerUserProviderInternal =
        RetenoDatabaseManagerUserProvider(databaseProvider)
    val retenoDatabaseManagerUserProvider: ProviderWeakReference<RetenoDatabaseManagerUser>
        get() = retenoDatabaseManagerUserProviderInternal

    private val retenoDatabaseManagerInteractionProviderInternal =
        RetenoDatabaseManagerInteractionProvider(databaseProvider)
    val retenoDatabaseManagerInteractionProvider: ProviderWeakReference<RetenoDatabaseManagerInteraction>
        get() = retenoDatabaseManagerInteractionProviderInternal

    private val retenoDatabaseManagerEventsProviderInternal =
        RetenoDatabaseManagerEventsProvider(databaseProvider)
    val retenoDatabaseManagerEventsProvider: ProviderWeakReference<RetenoDatabaseManagerEvents>
        get() = retenoDatabaseManagerEventsProviderInternal

    private val retenoDatabaseManagerAppInboxProviderInternal =
        RetenoDatabaseManagerAppInboxProvider(databaseProvider)
    val retenoDatabaseManagerAppInboxProvider: ProviderWeakReference<RetenoDatabaseManagerAppInbox>
        get() = retenoDatabaseManagerAppInboxProviderInternal

    internal val retenoDatabaseManagerLogEventProvider =
        RetenoDatabaseManagerLogEventProvider(databaseProvider)

    internal val retenoDatabaseManagerRecomEventsProvider =
        RetenoDatabaseManagerRecomEventsProvider(databaseProvider)

    internal val retenoDatabaseManagerWrappedLinksProvider =
        RetenoDatabaseManagerWrappedLinkProvider(databaseProvider)

    private val retenoDatabaseManagerInAppMessagesProvider =
        RetenoDatabaseManagerInAppMessagesProvider(databaseProvider)

    internal val retenoDatabaseManagerInAppInteractionProvider =
        RetenoDatabaseManagerInAppInteractionProvider(databaseProvider)

    internal val retenoDatabaseManagerProvider = RetenoDatabaseManagerProvider(
        retenoDatabaseManagerDeviceProviderInternal,
        retenoDatabaseManagerUserProviderInternal,
        retenoDatabaseManagerInteractionProviderInternal,
        retenoDatabaseManagerEventsProviderInternal,
        retenoDatabaseManagerAppInboxProviderInternal,
        retenoDatabaseManagerRecomEventsProvider,
        retenoDatabaseManagerWrappedLinksProvider,
        retenoDatabaseManagerLogEventProvider,
        retenoDatabaseManagerInAppInteractionProvider
    )

    /** Repository **/
    private val configRepositoryProviderInternal: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
            context,
            sharedPrefsManagerProvider,
            restConfigProvider
        )
    val configRepositoryProvider: ProviderWeakReference<ConfigRepository>
        get() = configRepositoryProviderInternal

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(
            retenoDatabaseManagerInteractionProviderInternal,
            retenoDatabaseManagerInAppInteractionProvider,
            dataSync
        )

    internal val interactionCacheProvider: InteractionCacheProvider =
        InteractionCacheProvider(
            retenoDatabaseManagerInteractionProviderInternal,
        )

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(retenoDatabaseManagerWrappedLinksProvider, dataSync)

    internal val appInboxRepositoryProvider: AppInboxRepositoryProvider =
        AppInboxRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerAppInboxProviderInternal,
            configRepositoryProviderInternal,
            dataSync
        )

    private val recommendationRepositoryProvider: RecommendationRepositoryProvider =
        RecommendationRepositoryProvider(
            retenoDatabaseManagerRecomEventsProvider,
            apiClientProvider
        )

    private val fileManagerProvider:FileManagerProvider = FileManagerProvider(context)

    private val iamRepositoryProvider: IamRepositoryProvider =
        IamRepositoryProvider(
            context,
            fileManagerProvider,
            apiClientProvider,
            sharedPrefsManagerProvider,
            retenoDatabaseManagerInAppMessagesProvider,
            Dispatchers.IO
        )


    /** Controller **/
    private val deeplinkControllerProviderInternal: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)
    val deeplinkControllerProvider: ProviderWeakReference<DeeplinkController>
        get() = deeplinkControllerProviderInternal


    private val interactionControllerProviderInternal: InteractionControllerProvider =
        InteractionControllerProvider(
            configRepositoryProviderInternal,
            interactionRepositoryProvider
        )
    val interactionControllerProvider: ProviderWeakReference<InteractionController>
        get() = interactionControllerProviderInternal

    private val appInboxControllerProvider: AppInboxControllerProvider =
        AppInboxControllerProvider(appInboxRepositoryProvider)

    private val recommendationControllerProvider: RecommendationControllerProvider =
        RecommendationControllerProvider(recommendationRepositoryProvider)


    /** Controller dependent **/
    internal val appInboxProvider: AppInboxProvider = AppInboxProvider(appInboxControllerProvider)

    internal val recommendationProvider: RecommendationProvider =
        RecommendationProvider(recommendationControllerProvider)

    private val retenoSessionHandlerProviderInternal =
        RetenoSessionHandlerProvider(sharedPrefsManagerProvider, lifecycleEventFilter) { actions.logEvent }
    val retenoSessionHandlerProvider: Provider<RetenoSessionHandler>
        get() = retenoSessionHandlerProviderInternal

    internal val iamControllerProvider: IamControllerProvider = IamControllerProvider(
        iamRepositoryProvider,
        retenoSessionHandlerProviderInternal,
        retenoDatabaseManagerUserProviderInternal
    )

    private val iamViewProviderInternal: IamViewProvider = IamViewProvider(
        retenoActivityHelperProviderInternal,
        iamControllerProvider,
        interactionControllerProviderInternal,
        dataSync
    )

    val iamViewProvider: ProviderWeakReference<IamView> =
        iamViewProviderInternal

    internal val currentConfig: RetenoConfig
        get() = configProvider.get()

    fun initWorkManager() {
        workManagerProvider.get()
    }

    fun setConfig(config: RetenoConfig) {
        configuration.useUserConfig(config)
    }
}