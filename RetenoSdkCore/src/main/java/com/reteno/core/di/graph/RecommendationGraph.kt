package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.internal.action.recommendation.FetchRecommendationAction
import com.reteno.core.internal.action.recommendation.FetchRecommendationJsonAction
import com.reteno.core.internal.action.recommendation.LogRecommendationEventsAction

internal class RecommendationGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure
) {

    val fetchRecommendation: FetchRecommendationAction by lazy {
        FetchRecommendationAction(core.api, core.initGate, core.dispatchers, core.runner)
    }

    val fetchRecommendationJson: FetchRecommendationJsonAction by lazy {
        FetchRecommendationJsonAction(core.api, core.initGate, core.dispatchers, core.runner)
    }

    val logRecommendationEvents: LogRecommendationEventsAction by lazy {
        LogRecommendationEventsAction(infrastructure.db.recomEventsDb, core.dispatchers, core.runner)
    }
}
