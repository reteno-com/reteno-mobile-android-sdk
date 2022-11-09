package com.reteno.core.di

import android.content.Context
import com.reteno.core.di.provider.*

class ServiceLocator(context: Context) {

    // TODO: Separate internal objects from externally exposed
    // TODO: Mark internal fields as internal

    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider = SharedPrefsManagerProvider()

    private val deviceIdHelperProvider: DeviceIdHelperProvider = DeviceIdHelperProvider(sharedPrefsManagerProvider)
    private val restConfigProvider: RestConfigProvider = RestConfigProvider(deviceIdHelperProvider)

    val retenoActivityHelperProvider: RetenoActivityHelperProvider = RetenoActivityHelperProvider()
    private val apiClientProvider: ApiClientProvider = ApiClientProvider()
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

    /** Controller **/
    val deeplinkControllerProvider: DeeplinkControllerProvider =
        DeeplinkControllerProvider(deeplinkRepositoryProvider)

    val contactControllerProvider: ContactControllerProvider =
        ContactControllerProvider(
            contactRepositoryProvider,
            configRepositoryProvider
        )

    val eventsControllerProvider: EventsControllerProvider =
        EventsControllerProvider(eventsRepositoryProvider)

    val scheduleControllerProvider: ScheduleControllerProvider =
        ScheduleControllerProvider(
            contactControllerProvider,
            interactionControllerProvider,
            eventsControllerProvider
        )

    init {

    }
}