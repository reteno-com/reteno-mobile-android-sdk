package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.SyncRouter
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncAction
import com.reteno.core.internal.action.sync.ClearOldDataAction
import com.reteno.core.internal.action.sync.DataSync
import com.reteno.core.internal.action.sync.PushDeviceStep
import com.reteno.core.internal.action.sync.PushEventsStep
import com.reteno.core.internal.action.sync.PushInAppInteractionsStep
import com.reteno.core.internal.action.sync.PushInboxStatusStep
import com.reteno.core.internal.action.sync.PushInteractionsStep
import com.reteno.core.internal.action.sync.PushLogEventsStep
import com.reteno.core.internal.action.sync.PushRecomEventsStep
import com.reteno.core.internal.action.sync.PushUserStep
import com.reteno.core.internal.action.sync.PushWrappedLinksStep
import com.reteno.core.internal.action.sync.SyncCoordinator
import com.reteno.core.internal.action.sync.SyncDataAction
import kotlin.random.Random

internal class SyncGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    private val clearOldData: ClearOldDataAction by lazy {
        ClearOldDataAction(
            interactionsDb = infrastructure.db.interactionsDb,
            inAppInteractionsDb = infrastructure.db.inAppInteractionsDb,
            eventsDb = infrastructure.db.eventsDb,
            inboxDb = infrastructure.db.inboxDb,
            recomEventsDb = infrastructure.db.recomEventsDb,
            wrappedLinksDb = infrastructure.db.wrappedLinksDb,
            configuration = core.configuration,
            remoteLog = core.remoteLog,
            clock = core.clock,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    val dataSync: DataSync by lazy {
        DataSync(
            coordinator = SyncCoordinator(syncData, core.scope, core.initGate),
            clearOldDataAction = clearOldData,
            backgroundWork = core.backgroundWork,
            configuration = core.configuration,
            jitter = { Random.nextLong(DataSync.RANDOM_DELAY) },
            scope = core.scope,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    private val syncData: SyncDataAction by lazy {
        SyncDataAction(
            contactSteps = listOf(
                PushDeviceStep(
                    infrastructure.db.devicesDb,
                    core.api,
                    infrastructure.configRepository,
                    core.dispatchers,
                    core.runner
                ),
                PushUserStep(infrastructure.db.usersDb, core.api, core.dispatchers, core.runner)
            ),
            dataSteps = listOf(
                PushInteractionsStep(infrastructure.db.interactionsDb, core.api, core.dispatchers, core.runner),
                PushInAppInteractionsStep(
                    infrastructure.db.inAppInteractionsDb,
                    core.api,
                    core.dispatchers,
                    core.runner
                ),
                PushEventsStep(infrastructure.db.eventsDb, core.api, core.dispatchers, core.runner),
                PushInboxStatusStep(
                    infrastructure.db.inboxDb,
                    core.api,
                    graph.inbox.inboxCountPoller,
                    core.dispatchers,
                    core.runner
                ),
                PushRecomEventsStep(infrastructure.db.recomEventsDb, core.api, core.dispatchers, core.runner),
                PushWrappedLinksStep(infrastructure.db.wrappedLinksDb, core.api, core.dispatchers, core.runner),
                PushLogEventsStep(infrastructure.db.logEventsDb, core.api, core.dispatchers, core.runner)
            ),
            runner = core.runner
        )
    }

    val backgroundSync: BackgroundSync by lazy {
        BackgroundSyncAction(
            activityHelper = infrastructure.activityHelper,
            initGate = core.initGate,
            databaseManager = infrastructure.db.databaseManager,
            sync = dataSync,
            backgroundWork = core.backgroundWork,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    val router: SyncRouter by lazy {
        SyncRouter(this)
    }
}
