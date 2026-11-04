package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.PushRouter
import com.reteno.core.internal.action.push.InitializeIamFromPushAction
import com.reteno.core.internal.action.push.SaveDefaultNotificationChannelAction

internal class PushGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    val initializeIamFromPush: InitializeIamFromPushAction by lazy {
        InitializeIamFromPushAction(graph.inApp.inAppPresenter, core.runner)
    }

    val saveDefaultNotificationChannel: SaveDefaultNotificationChannelAction by lazy {
        SaveDefaultNotificationChannelAction(infrastructure.configRepository, core.dispatchers, core.runner)
    }

    val router: PushRouter by lazy {
        PushRouter(this, graph.interactions, graph.deeplink, core.scope)
    }
}
