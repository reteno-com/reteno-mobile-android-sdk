package com.reteno.core.di.graph

import com.reteno.core.di.ActionCore
import com.reteno.core.di.ActionGraph
import com.reteno.core.di.SdkInfrastructure
import com.reteno.core.di.route.ContactRouter
import com.reteno.core.internal.action.device.ContactSession
import com.reteno.core.internal.action.device.CurrentDevice
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSessionAction
import com.reteno.core.internal.action.device.RegisterDeviceAction
import com.reteno.core.internal.action.device.UpdateFcmTokenAction
import com.reteno.core.internal.action.device.UpdateNotificationsEnabledAction
import com.reteno.core.internal.coroutine.SerialQueue

internal class DeviceGraph(
    private val core: ActionCore,
    private val infrastructure: SdkInfrastructure,
    private val graph: ActionGraph
) {

    val contactSession = ContactSession()

    val contactQueue by lazy { SerialQueue(core.scope, "Contact", core.logSink) }

    val currentDevice by lazy {
        CurrentDevice(
            infrastructure.configRepository,
            infrastructure.db.devicesDb,
            core.environment,
            core.dispatchers
        )
    }

    val registerDevice: RegisterDeviceAction by lazy {
        RegisterDeviceAction(
            infrastructure.configRepository,
            currentDevice,
            graph.sync.dataSync,
            contactSession,
            core.runner
        )
    }

    val ensureDeviceRegisteredThisSession: EnsureDeviceRegisteredThisSessionAction by lazy {
        EnsureDeviceRegisteredThisSessionAction(
            configRepository = infrastructure.configRepository,
            devicesDb = infrastructure.db.devicesDb,
            currentDevice = currentDevice,
            sync = graph.sync.dataSync,
            session = contactSession,
            dispatchers = core.dispatchers,
            runner = core.runner
        )
    }

    val updateFcmToken: UpdateFcmTokenAction by lazy {
        UpdateFcmTokenAction(
            infrastructure.configRepository,
            currentDevice,
            graph.sync.dataSync,
            contactSession,
            core.runner
        )
    }

    val updateNotificationsEnabled: UpdateNotificationsEnabledAction by lazy {
        UpdateNotificationsEnabledAction(
            configRepository = infrastructure.configRepository,
            currentDevice = currentDevice,
            sync = graph.sync.dataSync,
            session = contactSession,
            lifecycleEvents = graph.lifecycle.lifecycleEvents,
            runner = core.runner
        )
    }

    val router: ContactRouter by lazy {
        ContactRouter(graph.user, this)
    }
}
