package com.reteno.core.di.route

import com.reteno.core.di.graph.DeeplinkGraph
import com.reteno.core.di.graph.InteractionGraph
import com.reteno.core.di.graph.PushGraph
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.internal.action.push.PushActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PushRouter(
    private val push: PushGraph,
    private val interactions: InteractionGraph,
    private val deeplink: DeeplinkGraph,
    private val scope: CoroutineScope
) : PushActions {

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) {
        scope.launch { interactions.recordPushInteraction(interactionId, status) }
    }

    override fun submitDeeplinkClick(wrappedLink: String) {
        scope.launch { deeplink.trackDeeplinkClick(wrappedLink) }
    }

    override fun submitIamFromPush(interactionId: String) {
        scope.launch { push.initializeIamFromPush(interactionId) }
    }

    override fun submitDefaultNotificationChannel(channel: String) {
        scope.launch { push.saveDefaultNotificationChannel(channel) }
    }
}
