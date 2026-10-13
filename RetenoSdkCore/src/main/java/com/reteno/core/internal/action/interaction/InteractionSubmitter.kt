package com.reteno.core.internal.action.interaction

import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction

internal interface InteractionSubmitter {
    fun submitInAppInteraction(interaction: InAppInteraction)

    fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean)
}
