package com.reteno.core.di

import android.content.Context
import com.reteno.core.di.provider.*
import com.reteno.core.di.provider.controller.*
import com.reteno.core.di.provider.database.*
import com.reteno.core.di.provider.features.AppInboxProvider
import com.reteno.core.di.provider.features.RecommendationProvider
import com.reteno.core.di.provider.network.ApiClientProvider
import com.reteno.core.di.provider.network.RestClientProvider
import com.reteno.core.di.provider.repository.*

class ServiceLocator(context: Context, accessKey: String) {

    // TODO: Separate internal objects from externally exposed
    // TODO: Mark internal fields as internal

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider = SharedPrefsManagerProvider()

    private val deviceIdHelperProvider: DeviceIdHelperProvider = DeviceIdHelperProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdHelperProvider, accessKey)
    private val restClientProvider: RestClientProvider = RestClientProvider(restConfigProvider)

    private val apiClientProvider: ApiClientProvider = ApiClientProvider(restClientProvider)
    private val databaseProvider: DatabaseProvider = DatabaseProvider(context)

    val retenoDatabaseManagerDeviceProvider =
        RetenoDatabaseManagerDeviceProvider(databaseProvider)
    val retenoDatabaseManagerUserProvider =
        RetenoDatabaseManagerUserProvider(databaseProvider)
    val retenoDatabaseManagerInteractionProvider =
        RetenoDatabaseManagerInteractionProvider(databaseProvider)
    val retenoDatabaseManagerEventsProvider =
        RetenoDatabaseManagerEventsProvider(databaseProvider)
    val retenoDatabaseManagerAppInboxProvider =
        RetenoDatabaseManagerAppInboxProvider(databaseProvider)
    val retenoDatabaseManagerRecomEventsProvider =
        RetenoDatabaseManagerRecomEventsProvider(databaseProvider)

    val retenoDatabaseManagerProvider = RetenoDatabaseManagerProvider(
        retenoDatabaseManagerDeviceProvider,
        retenoDatabaseManagerUserProvider,
        retenoDatabaseManagerInteractionProvider,
        retenoDatabaseManagerEventsProvider,
        retenoDatabaseManagerAppInboxProvider,
        retenoDatabaseManagerRecomEventsProvider
    )

    /** Repository **/
    val configRepositoryProvider: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
            sharedPrefsManagerProvider,
            restConfigProvider
        )
    private val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerEventsProvider,
            configRepositoryProvider
        )

    private val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(
            apiClientProvider,
            configRepositoryProvider,
            retenoDatabaseManagerDeviceProvider,
            retenoDatabaseManagerUserProvider
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(apiClientProvider, retenoDatabaseManagerInteractionProvider)
    val interactionControllerProvider: InteractionControllerProvider =
        InteractionControllerProvider(configRepositoryProvider, interactionRepositoryProvider)

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider)

    private val appInboxRepositoryProvider: AppInboxRepositoryProvider =
        AppInboxRepositoryProvider(
            apiClientProvider,
            retenoDatabaseManagerAppInboxProvider,
            configRepositoryProvider
        )

    private val recommendationRepositoryProvider: RecommendationRepositoryProvider =
        RecommendationRepositoryProvider(retenoDatabaseManagerRecomEventsProvider, apiClientProvider)

    /** Controller **/
    val deeplinkControllerProvider: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)

    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProvider
        )

    internal val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    private val workManagerProvider: WorkManagerProvider = WorkManagerProvider(context)

    private val appInboxControllerProvider: AppInboxControllerProvider =
        AppInboxControllerProvider(appInboxRepositoryProvider)

    private val recommendationControllerProvider: RecommendationControllerProvider =
        RecommendationControllerProvider(recommendationRepositoryProvider)

    val scheduleControllerProvider: ScheduleControllerProvider =
        ScheduleControllerProvider(
            contactControllerProvider,
            interactionControllerProvider,
            eventsControllerProvider,
            appInboxControllerProvider,
            recommendationControllerProvider,
            workManagerProvider
        )

    /** Controller dependent **/
    val retenoActivityHelperProvider: RetenoActivityHelperProvider = RetenoActivityHelperProvider(eventsControllerProvider)

    val appInboxProvider: AppInboxProvider = AppInboxProvider(appInboxControllerProvider)

    internal val recommendationProvider: RecommendationProvider =
        RecommendationProvider(recommendationControllerProvider)
}