package com.reteno.core.di

import android.content.Context
import com.reteno.core.data.local.database.manager.*
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.di.provider.*
import com.reteno.core.di.provider.controller.*
import com.reteno.core.di.provider.database.*
import com.reteno.core.di.provider.features.AppInboxProvider
import com.reteno.core.di.provider.features.RecommendationProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import com.reteno.core.di.provider.network.RestClientProvider
import com.reteno.core.di.provider.repository.*
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.lifecycle.RetenoActivityHelper

class ServiceLocator(context: Context, accessKey: String) {

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider = SharedPrefsManagerProvider()
    private val workManagerProvider: WorkManagerProvider = WorkManagerProvider(context)

    private val deviceIdHelperProvider: DeviceIdHelperProvider = DeviceIdHelperProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdHelperProvider, accessKey)
    private val restClientProvider: RestClientProvider = RestClientProvider(restConfigProvider)

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

    private val retenoDatabaseManagerRecomEventsProvider =
        RetenoDatabaseManagerRecomEventsProvider(databaseProvider)

    internal val retenoDatabaseManagerProvider = RetenoDatabaseManagerProvider(
        retenoDatabaseManagerDeviceProviderInternal,
        retenoDatabaseManagerUserProviderInternal,
        retenoDatabaseManagerInteractionProviderInternal,
        retenoDatabaseManagerEventsProviderInternal,
        retenoDatabaseManagerAppInboxProviderInternal,
        retenoDatabaseManagerRecomEventsProvider
    )

    /** Repository **/
    private val configRepositoryProviderInternal: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
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
        InteractionRepositoryProvider(apiClientProvider, retenoDatabaseManagerInteractionProviderInternal)


    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider)

    private val appInboxRepositoryProvider: AppInboxRepositoryProvider =
        AppInboxRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerAppInboxProviderInternal,
            configRepositoryProviderInternal
        )

    private val recommendationRepositoryProvider: RecommendationRepositoryProvider =
        RecommendationRepositoryProvider(retenoDatabaseManagerRecomEventsProvider, apiClientProvider)

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
        InteractionControllerProvider(configRepositoryProviderInternal, interactionRepositoryProvider)
    val interactionControllerProvider: ProviderWeakReference<InteractionController>
        get() = interactionControllerProviderInternal

    internal val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

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
            workManagerProvider
        )

    /** Controller dependent **/
    private val retenoActivityHelperProviderInternal: RetenoActivityHelperProvider =
        RetenoActivityHelperProvider(eventsControllerProvider)
    val retenoActivityHelperProvider: ProviderWeakReference<RetenoActivityHelper>
        get() = retenoActivityHelperProviderInternal

    internal val appInboxProvider: AppInboxProvider = AppInboxProvider(appInboxControllerProvider)

    internal val recommendationProvider: RecommendationProvider =
        RecommendationProvider(recommendationControllerProvider)


}