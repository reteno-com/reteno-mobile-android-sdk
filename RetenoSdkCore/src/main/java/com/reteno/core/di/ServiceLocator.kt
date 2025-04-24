package com.reteno.core.di

import android.content.Context
import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.LogEventRepository
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.DeviceIdHelperProvider
import com.reteno.core.di.provider.RestConfigProvider
import com.reteno.core.di.provider.RetenoActivityHelperProvider
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.di.provider.WorkManagerProvider
import com.reteno.core.di.provider.controller.AppInboxControllerProvider
import com.reteno.core.di.provider.controller.AppLifecycleControllerProvider
import com.reteno.core.di.provider.controller.ContactControllerProvider
import com.reteno.core.di.provider.controller.DeeplinkControllerProvider
import com.reteno.core.di.provider.controller.EventsControllerProvider
import com.reteno.core.di.provider.controller.IamControllerProvider
import com.reteno.core.di.provider.controller.InteractionControllerProvider
import com.reteno.core.di.provider.controller.RecommendationControllerProvider
import com.reteno.core.di.provider.controller.ScheduleControllerProvider
import com.reteno.core.di.provider.controller.ScreenTrackingControllerProvider
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
import com.reteno.core.di.provider.repository.ContactRepositoryProvider
import com.reteno.core.di.provider.repository.DeeplinkRepositoryProvider
import com.reteno.core.di.provider.repository.EventsRepositoryProvider
import com.reteno.core.di.provider.repository.IamRepositoryProvider
import com.reteno.core.di.provider.repository.InteractionRepositoryProvider
import com.reteno.core.di.provider.repository.LogEventRepositoryProvider
import com.reteno.core.di.provider.repository.RecommendationRepositoryProvider
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.Dispatchers

class ServiceLocator(
    context: Context
) {

    private val configProvider = RetenoConfigProvider()
    private val retenoActivityHelperProviderInternal: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider()
    val retenoActivityHelperProvider: ProviderWeakReference<RetenoActivityHelper>
        get() = retenoActivityHelperProviderInternal

    internal val sharedPrefsManagerProvider: SharedPrefsManagerProvider =
        SharedPrefsManagerProvider(context)
    private val workManagerProvider: WorkManagerProvider = WorkManagerProvider(context)

    private val deviceIdHelperProvider: DeviceIdHelperProvider =
        DeviceIdHelperProvider(context, sharedPrefsManagerProvider, configProvider)
    private val restConfigProvider: RestConfigProvider =
        RestConfigProvider(sharedPrefsManagerProvider, deviceIdHelperProvider, configProvider)
    private val restClientProvider: RestClientProvider =
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

    private val retenoDatabaseManagerLogEventProvider =
        RetenoDatabaseManagerLogEventProvider(databaseProvider)

    private val retenoDatabaseManagerRecomEventsProvider =
        RetenoDatabaseManagerRecomEventsProvider(databaseProvider)

    private val retenoDatabaseManagerWrappedLinksProvider =
        RetenoDatabaseManagerWrappedLinkProvider(databaseProvider)

    private val retenoDatabaseManagerInAppMessagesProvider =
        RetenoDatabaseManagerInAppMessagesProvider(databaseProvider)

    private val retenoDatabaseManagerInAppInteractionProvider =
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

    private val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerEventsProviderInternal,
            configRepositoryProviderInternal
        )

    private val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(
            apiClientProvider,
            configRepositoryProviderInternal,
            retenoDatabaseManagerDeviceProviderInternal,
            retenoDatabaseManagerUserProviderInternal
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerInteractionProviderInternal,
            retenoDatabaseManagerInAppInteractionProvider
        )

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider, retenoDatabaseManagerWrappedLinksProvider)

    private val appInboxRepositoryProvider: AppInboxRepositoryProvider =
        AppInboxRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerAppInboxProviderInternal,
            configRepositoryProviderInternal
        )

    private val recommendationRepositoryProvider: RecommendationRepositoryProvider =
        RecommendationRepositoryProvider(
            retenoDatabaseManagerRecomEventsProvider,
            apiClientProvider
        )

    private val iamRepositoryProvider: IamRepositoryProvider =
        IamRepositoryProvider(
            context,
            apiClientProvider,
            sharedPrefsManagerProvider,
            retenoDatabaseManagerInAppMessagesProvider,
            Dispatchers.IO
        )

    private val logEventRepositoryProviderInternal: LogEventRepositoryProvider =
        LogEventRepositoryProvider(
            retenoDatabaseManagerLogEventProvider,
            apiClientProvider
        )
    val logEventRepositoryProvider: ProviderWeakReference<LogEventRepository>
        get() = logEventRepositoryProviderInternal


    /** Controller **/
    private val deeplinkControllerProviderInternal: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)
    val deeplinkControllerProvider: ProviderWeakReference<DeeplinkController>
        get() = deeplinkControllerProviderInternal

    private val contactControllerProviderInternal: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProviderInternal
        )
    val contactControllerProvider: ProviderWeakReference<ContactController>
        get() = contactControllerProviderInternal

    private val interactionControllerProviderInternal: InteractionControllerProvider =
        InteractionControllerProvider(
            configRepositoryProviderInternal,
            interactionRepositoryProvider
        )
    val interactionControllerProvider: ProviderWeakReference<InteractionController>
        get() = interactionControllerProviderInternal

    internal val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider, logEventRepositoryProviderInternal)

    private val appInboxControllerProvider: AppInboxControllerProvider =
        AppInboxControllerProvider(appInboxRepositoryProvider)

    private val recommendationControllerProvider: RecommendationControllerProvider =
        RecommendationControllerProvider(recommendationRepositoryProvider)

    val scheduleControllerProvider: ProviderWeakReference<ScheduleController> =
        ScheduleControllerProvider(
            contactControllerProviderInternal,
            interactionControllerProviderInternal,
            eventsControllerProvider,
            appInboxControllerProvider,
            recommendationControllerProvider,
            deeplinkControllerProviderInternal,
            workManagerProvider
        )

    internal val screenTrackingControllerProvider: ScreenTrackingControllerProvider =
        ScreenTrackingControllerProvider(
            retenoActivityHelperProviderInternal,
            eventsControllerProvider
        )


    /** Controller dependent **/
    internal val appInboxProvider: AppInboxProvider = AppInboxProvider(appInboxControllerProvider)

    internal val recommendationProvider: RecommendationProvider =
        RecommendationProvider(recommendationControllerProvider)

    private val retenoSessionHandlerProviderInternal =
        RetenoSessionHandlerProvider(sharedPrefsManagerProvider)
    val retenoSessionHandlerProvider: ProviderWeakReference<RetenoSessionHandler>
        get() = retenoSessionHandlerProviderInternal

    internal val iamControllerProvider: IamControllerProvider = IamControllerProvider(
        iamRepositoryProvider,
        retenoSessionHandlerProviderInternal,
        eventsControllerProvider
    )

    private val appLifecycleControllerProviderInternal = AppLifecycleControllerProvider(
        configRepository = configRepositoryProviderInternal,
        eventController = eventsControllerProvider,
        configProvider = configProvider,
        sessionHandlerProvider = retenoSessionHandlerProviderInternal,
        activityHelperProvider = retenoActivityHelperProviderInternal,
        scheduleControllerProvider = scheduleControllerProvider,
        iamControllerProvider = iamControllerProvider
    )

    val appLifecycleControllerProvider: ProviderWeakReference<AppLifecycleController>
        get() = appLifecycleControllerProviderInternal

    private val iamViewProviderInternal: IamViewProvider = IamViewProvider(
        retenoActivityHelperProviderInternal,
        iamControllerProvider,
        interactionControllerProviderInternal,
        scheduleControllerProvider
    )

    val iamViewProvider: ProviderWeakReference<IamView> =
        iamViewProviderInternal

    internal val currentConfig: RetenoConfig
        get() = configProvider.get()

    fun initWorkManager() {
        workManagerProvider.get()
    }

    fun setConfig(config: RetenoConfig) {
        configProvider.setConfig(config)
    }
}