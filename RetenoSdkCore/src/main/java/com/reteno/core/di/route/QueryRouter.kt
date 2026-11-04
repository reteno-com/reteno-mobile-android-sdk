package com.reteno.core.di.route

import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.SdkQueries
import com.reteno.core.internal.log.RemoteLog

internal class QueryRouter(
    private val infrastructure: SdkInfrastructure,
    private val remoteLog: RemoteLog
) : SdkQueries {

    override val deviceId: String
        get() = infrastructure.configRepository.getDeviceId().id

    override val defaultNotificationChannel: String
        get() = infrastructure.configRepository.getDefaultNotificationChannel()

    override fun canPresentMessages(): Boolean = infrastructure.activityHelper.canPresentMessages()

    override fun isActivityPresented(): Boolean = infrastructure.activityHelper.currentActivity != null

    override fun hasDataForSync(): Boolean =
        try {
            infrastructure.db.databaseManager.hasDataForSync()
        } catch (error: Exception) {
            remoteLog.recordError("Failed to check local database for unsynced data", error)
            true
        }
}
