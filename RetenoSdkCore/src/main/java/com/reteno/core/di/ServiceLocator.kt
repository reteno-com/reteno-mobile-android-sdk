package com.reteno.core.di

import android.content.Context
import com.reteno.core.di.provider.*

class ServiceLocator(context: Context, accessKey: String) {

    // TODO: Separate internal objects from externally exposed
    // TODO: Mark internal fields as internal

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider = SharedPrefsManagerProvider()

    private val deviceIdHelperProvider: DeviceIdHelperProvider = DeviceIdHelperProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdHelperProvider, accessKey)
    private val restClientProvider: RestClientProvider = RestClientProvider(restConfigProvider)

    private val apiClientProvider: ApiClientProvider = ApiClientProvider(restClientProvider)
    private val databaseProvider: DatabaseProvider = DatabaseProvider(context)
    val databaseManagerProvider: DatabaseManagerProvider = DatabaseManagerProvider(databaseProvider)

    /** Repository **/
    val configRepositoryProvider: ConfigRepositoryProvider =
        ConfigRepositoryProvider(
            sharedPrefsManagerProvider,
            restConfigProvider
        )
    private val eventsRepositoryProvider: EventsRepositoryProvider =
        EventsRepositoryProvider(
            apiClientProvider,
            databaseManagerProvider,
            configRepositoryProvider
        )

    private val contactRepositoryProvider: ContactRepositoryProvider =
        ContactRepositoryProvider(
            apiClientProvider,
            configRepositoryProvider,
            databaseManagerProvider
        )

    private val interactionRepositoryProvider: InteractionRepositoryProvider =
        InteractionRepositoryProvider(apiClientProvider, databaseManagerProvider)
    val interactionControllerProvider: InteractionControllerProvider =
        InteractionControllerProvider(configRepositoryProvider, interactionRepositoryProvider)

    private val deeplinkRepositoryProvider: DeeplinkRepositoryProvider =
        DeeplinkRepositoryProvider(apiClientProvider)

    private val appInboxRepositoryProvider: AppInboxRepositoryProvider =
        AppInboxRepositoryProvider(
            apiClientProvider,
            databaseManagerProvider,
            configRepositoryProvider
        )

    private val recommendationRepositoryProvider: RecommendationRepositoryProvider =
        RecommendationRepositoryProvider(databaseManagerProvider, apiClientProvider)

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