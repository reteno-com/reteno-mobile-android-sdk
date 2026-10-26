package com.reteno.core.fixtures

import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.internal.action.push.PushActions

internal class FakePushActions(private val log: FakeCallLog = FakeCallLog()) : PushActions {

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) {
        log.record("submitPushInteraction $interactionId $status")
    }

    override fun submitDeeplinkClick(wrappedLink: String) {
        log.record("submitDeeplinkClick $wrappedLink")
    }

    override fun submitIamFromPush(interactionId: String) {
        log.record("submitIamFromPush $interactionId")
    }

    override fun submitDefaultNotificationChannel(channel: String) {
        log.record("submitDefaultNotificationChannel $channel")
    }
}
