package com.reteno.core.di.route

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.di.graph.InAppGraph
import com.reteno.core.di.graph.InteractionGraph
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class InteractionRouter(
    private val interactions: InteractionGraph,
    private val inApp: InAppGraph,
    private val scope: CoroutineScope
) : InteractionSubmitter {

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) {
        scope.launch { interactions.recordInteractionClick(interactionId, action, forcePush) }
    }

    override fun submitInAppInteraction(interaction: InAppInteraction) {
        scope.launch { interactions.recordInAppInteraction(interaction) }
    }

    override fun submitInAppShown(message: InAppMessage) {
        scope.launch { inApp.recordInAppShown(message) }
    }
}
