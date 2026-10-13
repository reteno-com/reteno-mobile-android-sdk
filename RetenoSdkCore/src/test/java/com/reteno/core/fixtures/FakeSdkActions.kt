package com.reteno.core.fixtures

import com.reteno.core.RetenoConfig
import com.reteno.core.di.SdkActions
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.action.sync.BackgroundSyncAction
import com.reteno.core.lifecycle.ScreenTrackingConfig

internal class FakeSdkActions : SdkActions {
    val calls = mutableListOf<String>()
    val configs = mutableListOf<RetenoConfig>()
    override var isStarted: Boolean = false

    override fun boot() {
        calls += "boot"
    }

    override fun applyUserConfig(config: RetenoConfig) {
        configs += config
        calls += "applyUserConfig"
    }

    override fun requestStart() {
        calls += "requestStart"
    }

    override fun requestStop() {
        calls += "requestStop"
    }

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        calls += "submitUserAttributes $externalUserId"
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        calls += "submitMultiAccountUserAttributes $externalUserId"
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        calls += "submitAnonymousUserAttributes"
    }

    override fun submitFcmToken(token: String) {
        calls += "submitFcmToken"
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        calls += "submitNotificationsEnabled $enabled"
    }

    override fun submitEvent(event: Event) {
        calls += "submitEvent ${event.eventTypeKey}"
    }

    override fun submitEcomEvent(event: EcomEvent) {
        calls += "submitEcomEvent"
    }

    override fun submitScreenView(screenName: String) {
        calls += "submitScreenView $screenName"
    }

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        calls += "submitRetenoLogEvent"
    }

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        calls += "submitLifecycleEventConfig"
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        calls += "submitAutoScreenTracking"
    }

    override fun submitPushPermissionStatus() {
        calls += "submitPushPermissionStatus"
    }

    override fun submitSessionDuration(durationMillis: Long) {
        calls += "submitSessionDuration $durationMillis"
    }

    override fun requestSync() {
        calls += "requestSync"
    }

    override val backgroundSync: BackgroundSyncAction
        get() = error("background sync is not part of facade tests")
}
