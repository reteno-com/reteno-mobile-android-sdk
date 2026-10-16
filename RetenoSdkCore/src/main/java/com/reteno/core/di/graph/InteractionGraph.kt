package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.internal.action.interaction.ProcessCachedInteractionsAction
import com.reteno.core.internal.action.interaction.RecordInAppInteractionAction
import com.reteno.core.internal.action.interaction.RecordInteractionAction
import com.reteno.core.internal.action.interaction.RecordInteractionClickAction
import com.reteno.core.internal.action.interaction.RecordPushInteractionAction
import com.reteno.core.util.Util

internal class InteractionGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    private val recordInteraction: RecordInteractionAction by lazy {
        RecordInteractionAction(
            infrastructure.db.interactionsDb,
            infrastructure.configRepository,
            core.dispatchers,
            Util::getCurrentTimeStamp,
            core.runner
        )
    }

    val recordPushInteraction: RecordPushInteractionAction by lazy {
        RecordPushInteractionAction(
            infrastructure.db.interactionsDb,
            core.initGate,
            processCachedInteractions,
            core.dispatchers,
            core.runner
        )
    }

    val recordInteractionClick: RecordInteractionClickAction by lazy {
        RecordInteractionClickAction(
            infrastructure.db.interactionsDb,
            graph.sync.dataSync,
            core.dispatchers,
            Util::getCurrentTimeStamp,
            core.runner
        )
    }

    val recordInAppInteraction: RecordInAppInteractionAction by lazy {
        RecordInAppInteractionAction(
            infrastructure.db.inAppInteractionsDb,
            graph.sync.dataSync,
            core.dispatchers,
            core.runner
        )
    }

    val processCachedInteractions: ProcessCachedInteractionsAction by lazy {
        ProcessCachedInteractionsAction(
            infrastructure.db.interactionsDb,
            recordInteraction,
            graph.sync.dataSync,
            core.dispatchers,
            core.runner
        )
    }
}
