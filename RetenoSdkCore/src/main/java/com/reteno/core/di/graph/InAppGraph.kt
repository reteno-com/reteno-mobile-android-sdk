package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.InAppRouter
import com.reteno.core.internal.action.iam.CheckInAppSegments
import com.reteno.core.internal.action.iam.CheckInAppSegmentsAction
import com.reteno.core.internal.action.iam.FetchInAppMessagesAction
import com.reteno.core.internal.action.iam.InAppHtmlPreload
import com.reteno.core.internal.action.iam.InAppMessagesCache
import com.reteno.core.internal.action.iam.InAppTriggers
import com.reteno.core.internal.action.iam.LoadInAppHtmlAction
import com.reteno.core.internal.action.iam.LoadPushInAppHtmlAction
import com.reteno.core.internal.action.iam.PauseInAppMessagesAction
import com.reteno.core.internal.action.iam.PreloadIamHtmlAction
import com.reteno.core.internal.action.iam.ProcessInAppMessagesAction
import com.reteno.core.internal.action.iam.RecordInAppShownAction
import com.reteno.core.internal.action.iam.ReportWidgetFailureAction
import com.reteno.core.internal.action.iam.SetInAppPauseBehaviourAction
import com.reteno.core.internal.action.iam.ShowInAppAction
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.iam.DisplayRulesEvaluator
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.core.internal.iam.InAppPresenter
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.sync.Mutex

internal class InAppGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    private val inAppTriggerLock = Mutex()

    val inAppQueue by lazy { SerialQueue(core.scope, "InApp", core.logSink) }

    private val inAppCache by lazy {
        InAppMessagesCache(infrastructure.db.inAppMessagesDb, core.dispatchers)
    }

    val inAppDisplayRules: InAppDisplayRules by lazy {
        DisplayRulesEvaluator(core.clock, core.logSink)
    }

    val inAppMessagesState: InAppMessagesState by lazy {
        InAppMessagesStateImpl()
    }

    val checkInAppSegments: CheckInAppSegments by lazy {
        CheckInAppSegmentsAction(core.api, inAppCache, core.clock, inAppTriggerLock, core.runner)
    }

    private val showInApp: ShowInAppAction by lazy {
        ShowInAppAction(
            rules = inAppDisplayRules,
            state = inAppMessagesState,
            checkSegments = checkInAppSegments,
            sessionHandler = infrastructure.sessionHandler,
            display = inAppPresenter,
            lock = inAppTriggerLock,
            runner = core.runner
        )
    }

    val inAppTriggers: InAppTriggers by lazy {
        InAppTriggers(
            eventBus = core.eventBus,
            state = inAppMessagesState,
            rules = inAppDisplayRules,
            showInApp = lazy { showInApp },
            sink = core.logSink,
            scope = core.scope
        )
    }

    val processInAppMessages: ProcessInAppMessagesAction by lazy {
        ProcessInAppMessagesAction(
            inAppMessagesState,
            infrastructure.sessionHandler,
            inAppTriggers,
            showInApp,
            core.runner
        )
    }

    val pauseInApps: PauseInAppMessagesAction by lazy {
        PauseInAppMessagesAction(inAppMessagesState, inAppPresenter, core.runner)
    }

    val setInAppPauseBehaviour: SetInAppPauseBehaviourAction by lazy {
        SetInAppPauseBehaviourAction(inAppMessagesState, core.runner)
    }

    val recordInAppShown: RecordInAppShownAction by lazy {
        RecordInAppShownAction(inAppCache, core.clock, inAppTriggerLock, core.runner)
    }

    val fetchInAppMessages: FetchInAppMessagesAction by lazy {
        FetchInAppMessagesAction(
            api = core.api,
            cache = inAppCache,
            prefs = infrastructure.prefs,
            checkSegments = checkInAppSegments,
            state = inAppMessagesState,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    val inAppHtmlPreload: InAppHtmlPreload by lazy {
        InAppHtmlPreload(
            preload = PreloadIamHtmlAction(
                core.api,
                infrastructure.prefs,
                infrastructure.fileManager,
                core.dispatchers,
                core.runner
            ),
            scope = core.scope
        )
    }

    val inAppPresenter: InAppPresenter by lazy {
        InAppPresenter(
            screen = core.hostUi.inAppScreen(),
            loadHtml = loadInAppHtml,
            loadPushHtml = loadPushInAppHtml,
            reportFailure = reportWidgetFailure,
            interactions = graph.interactions.router,
            remoteLog = core.remoteLog,
            scope = core.scope,
            dispatchers = core.dispatchers,
            newId = core.newId
        )
    }

    val iamView: IamView
        get() = inAppPresenter

    private val loadInAppHtml: LoadInAppHtmlAction by lazy {
        LoadInAppHtmlAction(
            files = infrastructure.fileManager,
            usersDb = infrastructure.db.usersDb,
            dispatchers = core.dispatchers,
            newId = core.newId,
            runner = core.runner
        )
    }

    private val loadPushInAppHtml: LoadPushInAppHtmlAction by lazy {
        LoadPushInAppHtmlAction(
            api = core.api,
            files = infrastructure.fileManager,
            environment = core.environment,
            initGate = core.initGate,
            dispatchers = core.dispatchers,
            newId = core.newId,
            runner = core.runner
        )
    }

    private val reportWidgetFailure: ReportWidgetFailureAction by lazy {
        ReportWidgetFailureAction(core.api, core.initGate, core.runner)
    }

    val router: InAppRouter by lazy {
        InAppRouter(this)
    }
}
