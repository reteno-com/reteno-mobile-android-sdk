package com.reteno.core.fixtures

import com.reteno.core.di.SdkQueries

internal class FakeSdkQueries : SdkQueries {
    override var deviceId: String = ""
    override var defaultNotificationChannel: String = ""
    var canPresent = false
    var activityPresented = false
    var dataForSync = false

    override fun canPresentMessages(): Boolean = canPresent

    override fun isActivityPresented(): Boolean = activityPresented

    override fun hasDataForSync(): Boolean = dataForSync
}
