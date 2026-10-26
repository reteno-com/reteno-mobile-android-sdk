package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.internal.action.user.ContactWriter
import com.reteno.core.internal.action.user.SegmentationRefreshImpl
import com.reteno.core.internal.action.user.SetAnonymousUserAttributesAction
import com.reteno.core.internal.action.user.SetMultiAccountUserAttributesAction
import com.reteno.core.internal.action.user.SetUserAttributesAction

internal class UserGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    private val segmentationRefresh by lazy {
        SegmentationRefreshImpl(graph.inApp.checkInAppSegments, graph.inApp.inAppMessagesState, core.scope)
    }

    private val contactWriter by lazy {
        ContactWriter(
            configRepository = infrastructure.configRepository,
            usersDb = infrastructure.db.usersDb,
            currentDevice = graph.device.currentDevice,
            sync = graph.sync.dataSync,
            session = graph.device.contactSession,
            sdk = graph.init.sdk,
            sessionHandler = infrastructure.sessionHandler,
            remoteLog = core.remoteLog,
            dispatchers = core.dispatchers
        )
    }

    val setUserAttributes: SetUserAttributesAction by lazy {
        SetUserAttributesAction(contactWriter, graph.init.sdk, segmentationRefresh, core.runner)
    }

    val setMultiAccountUserAttributes: SetMultiAccountUserAttributesAction by lazy {
        SetMultiAccountUserAttributesAction(contactWriter, graph.init.sdk, segmentationRefresh, core.runner)
    }

    val setAnonymousUserAttributes: SetAnonymousUserAttributesAction by lazy {
        SetAnonymousUserAttributesAction(
            infrastructure.configRepository,
            infrastructure.db.usersDb,
            graph.sync.dataSync,
            core.remoteLog,
            core.dispatchers,
            core.runner
        )
    }
}
