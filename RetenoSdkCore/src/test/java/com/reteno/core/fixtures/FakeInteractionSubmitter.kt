package com.reteno.core.fixtures

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.internal.action.interaction.InteractionSubmitter

internal class FakeInteractionSubmitter(private val log: FakeCallLog = FakeCallLog()) : InteractionSubmitter {

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) {
        log.record("submitInteractionClick $interactionId ${action.type} $forcePush")
    }

    override fun submitInAppInteraction(interaction: InAppInteraction) {
        log.record("submitInAppInteraction ${interaction.interactionId}")
    }

    override fun submitInAppShown(message: InAppMessage) {
        log.record("submitInAppShown ${message.messageId}")
    }
}
