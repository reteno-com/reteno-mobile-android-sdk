package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.internal.action.deeplink.TrackDeeplinkClickAction

internal class DeeplinkGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    val trackDeeplinkClick: TrackDeeplinkClickAction by lazy {
        TrackDeeplinkClickAction(infrastructure.db.wrappedLinksDb, graph.sync.dataSync, core.dispatchers, core.runner)
    }
}
