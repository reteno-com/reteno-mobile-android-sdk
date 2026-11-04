package com.reteno.push.fixtures

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.RetenoInternalFacade
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.permission.AndroidPermissionChecker
import com.reteno.core.util.Procedure

internal class FakeRetenoFacade : RetenoInternalFacade {

    val calls = mutableListOf<String>()
    var channelConfig: Procedure<NotificationChannelCompat.Builder>? = null
    var storedChannel: String = ""
    var storedChannelFailure: Throwable? = null
    var activityPresented = false
    var permissionChecker: AndroidPermissionChecker? = null
    var failure: Throwable? = null

    override val application: Application = ApplicationProvider.getApplicationContext()

    override fun onNewFcmToken(token: String) {
        record("onNewFcmToken $token")
    }

    override fun recordInteraction(id: String, status: InteractionStatus, forcePush: Boolean) {
        record("recordInteraction $id $status forcePush=$forcePush")
    }

    override fun recordInteractionAction(id: String, action: InteractionAction, forcePush: Boolean) {
        record(
            "recordInteractionAction $id ${action.type} ${action.targetComponentId} ${action.url} forcePush=$forcePush"
        )
    }

    override fun canPresentMessages(): Boolean = true

    override fun hasDataForSync(): Boolean = false

    override fun isActivityPresented(): Boolean = activityPresented

    override fun getDeviceId(): String = "device-id"

    override fun initializeIamView(interactionId: String) {
        record("initializeIamView $interactionId")
    }

    override fun saveDefaultNotificationChannel(channel: String) {
        record("saveDefaultNotificationChannel")
        storedChannel = channel
    }

    override fun getDefaultNotificationChannel(): String {
        storedChannelFailure?.let { throw it }
        return storedChannel
    }

    override fun notificationsEnabled(enabled: Boolean) {
        record("notificationsEnabled $enabled")
    }

    override fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String) {
        record("deeplinkClicked $linkWrapped $linkUnwrapped")
    }

    override fun getDefaultNotificationChannelConfig(): Procedure<NotificationChannelCompat.Builder>? = channelConfig

    override fun executeAfterInit(action: () -> Unit) {
        record("executeAfterInit")
        action()
    }

    override fun setSessionDuration(duration: Long) {
        record("setSessionDuration $duration")
    }

    override fun updatePushPermissionStatus() {
        record("updatePushPermissionStatus")
    }

    override suspend fun requestPermissionChecker(): AndroidPermissionChecker? = permissionChecker

    private fun record(call: String) {
        failure?.let { throw it }
        calls += call
    }
}

internal inline fun <T> withFakeFacade(
    facade: FakeRetenoFacade = FakeRetenoFacade(),
    block: (FakeRetenoFacade) -> T
): T {
    RetenoInternalEntryPoint.install(facade)
    try {
        return block(facade)
    } finally {
        RetenoInternalEntryPoint.install(null)
    }
}
