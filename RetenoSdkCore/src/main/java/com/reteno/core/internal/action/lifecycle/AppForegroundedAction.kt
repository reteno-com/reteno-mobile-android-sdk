package com.reteno.core.internal.action.lifecycle

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.lifecycle.AppForegroundState
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.internal.platform.AppVersion
import com.reteno.core.util.Constants

internal interface AppForegrounded {
    suspend operator fun invoke(): Result<Unit>
}

internal class AppForegroundedAction(
    private val platform: AppPlatform,
    private val dataSync: SyncScheduler,
    private val configRepository: ConfigRepository,
    private val iamController: IamController,
    private val state: AppForegroundState,
    private val lifecycleEvents: LifecycleEvents,
    private val clock: () -> Long,
    private val runner: ActionRunner
) : AppForegrounded {

    override suspend operator fun invoke(): Result<Unit> = runner("AppForegrounded") {
        step("notify app resume receivers", detail = { "receivers=$it" }) {
            platform.sendBroadcast(Constants.BROADCAST_ACTION_RETENO_APP_RESUME)
        }
        step("schedule outdated data cleanup in local database") {
            dataSync.clearOldData()
        }
        trackAppVersion()
        step("preload in-app message html") {
            iamController.preloadHtml()
        }
        trackAppOpened()
    }

    private suspend fun ActionTrace.trackAppVersion() {
        val installed = step("read installed app version", detail = { it.toString() }) {
            platform.installedVersion()
        }
        val saved = step("read saved app version from local settings", detail = { it.toString() }) {
            AppVersion(configRepository.getAppVersion(), configRepository.getAppBuildNumber())
        }
        if (saved.name.isNotEmpty() && saved == installed) {
            note { "app version unchanged" }
            return
        }
        val event = if (saved.name.isEmpty()) {
            Event.applicationInstall(installed.name, installed.code)
        } else {
            Event.applicationUpdate(
                version = installed.name,
                build = installed.code,
                prevVersion = saved.name,
                prevBuild = saved.code
            )
        }
        lifecycleEvents.log(this, event)
        step("save app version to local settings") {
            configRepository.saveAppVersion(installed.name)
            configRepository.saveAppBuildNumber(installed.code)
        }
    }

    private suspend fun ActionTrace.trackAppOpened() {
        val wasBackgrounded = state.enterForeground(clock())
        if (wasBackgrounded == null) {
            note { "app already in foreground" }
            return
        }
        lifecycleEvents.log(this, Event.applicationOpen(fromBackground = wasBackgrounded))
    }
}
