package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.InboxRouter
import com.reteno.core.internal.action.inbox.GetInboxMessagesAction
import com.reteno.core.internal.action.inbox.GetInboxMessagesCountAction
import com.reteno.core.internal.action.inbox.InboxCountPoller
import com.reteno.core.internal.action.inbox.MarkAllInboxMessagesOpenedAction
import com.reteno.core.internal.action.inbox.MarkInboxMessageOpenedAction
import com.reteno.core.util.Util

internal class InboxGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    val getInboxMessages: GetInboxMessagesAction by lazy {
        GetInboxMessagesAction(core.api, core.initGate, core.dispatchers, core.runner)
    }

    val getInboxMessagesCount: GetInboxMessagesCountAction by lazy {
        GetInboxMessagesCountAction(core.api, core.initGate, core.dispatchers, core.runner)
    }

    val inboxCountPoller: InboxCountPoller by lazy {
        InboxCountPoller(getInboxMessagesCount, core.scope, core.dispatchers)
    }

    val markInboxMessageOpened: MarkInboxMessageOpenedAction by lazy {
        MarkInboxMessageOpenedAction(
            inboxDb = infrastructure.db.inboxDb,
            configRepository = infrastructure.configRepository,
            sync = graph.sync.dataSync,
            initGate = core.initGate,
            dispatchers = core.dispatchers,
            timestamp = Util::getCurrentTimeStamp,
            runner = core.runner
        )
    }

    val markAllInboxMessagesOpened: MarkAllInboxMessagesOpenedAction by lazy {
        MarkAllInboxMessagesOpenedAction(
            core.api,
            infrastructure.db.inboxDb,
            inboxCountPoller,
            core.initGate,
            core.dispatchers,
            core.runner
        )
    }

    val router: InboxRouter by lazy {
        InboxRouter(this, core.scope)
    }
}
