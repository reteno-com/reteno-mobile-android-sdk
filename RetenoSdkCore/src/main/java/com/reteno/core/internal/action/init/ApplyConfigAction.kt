package com.reteno.core.internal.action.init

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.controller.IamController
import com.reteno.core.internal.action.device.RegisterDevice
import com.reteno.core.internal.action.interaction.ProcessCachedInteractions
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.lifecycle.AppLifecycleTracking
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.withContext

internal interface ApplyConfig {
    suspend operator fun invoke(config: RetenoConfig, byUser: Boolean): Result<Unit>
}

internal class ApplyConfigAction(
    private val configuration: SdkConfiguration,
    private val restConfig: RestConfig,
    private val configRepository: ConfigRepository,
    private val prefs: SharedPrefsManager,
    private val sessionHandler: RetenoSessionHandler,
    private val registerDevice: RegisterDevice,
    private val initGate: InitGate,
    private val iamController: IamController,
    private val iamView: IamView,
    private val lifecycleTracking: AppLifecycleTracking,
    private val processCachedInteractions: ProcessCachedInteractions,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : ApplyConfig {

    override suspend operator fun invoke(config: RetenoConfig, byUser: Boolean): Result<Unit> = runner("ApplyConfig") {
        args(
            "source" to if (byUser) "user" else "cache",
            "debug" to config.isDebug,
            "customDeviceId" to (config.userIdProvider != null),
            "sessionDurationMillis" to config.sessionDuration
        )
        apply(config, byUser)
    }

    private suspend fun ActionTrace.apply(config: RetenoConfig, byUser: Boolean) {
        if (byUser) {
            step("use user configuration") {
                configuration.useUserConfig(config)
            }
        } else {
            val used = step("use cached configuration") {
                configuration.useCachedConfig(config)
            }
            if (!used) note { "user configuration already in use" }
        }
        runner("ApplyConfigOptions") {
            applyOptions(config, byUser)
        }
        if (byUser) registerDevice()
        step("open init gate") {
            initGate.markInitialized(byUser)
        }
        step(
            "apply in-app pause options",
            detail = { "inApps=${config.isPausedInAppMessages}  pushInApps=${config.isPausedPushInAppMessages}" }
        ) {
            withContext(dispatchers.main) {
                iamController.pauseInAppMessages(config.isPausedInAppMessages)
                iamView.pauseIncomingPushInApps(config.isPausedPushInAppMessages)
            }
        }
        if (byUser) {
            step("start app lifecycle tracking") {
                withContext(dispatchers.main) { lifecycleTracking.start() }
            }
        }
        processCachedInteractions()
    }

    private suspend fun ActionTrace.applyOptions(config: RetenoConfig, byUser: Boolean) {
        if (byUser) {
            val mode = if (config.userIdProvider != null) DeviceIdMode.CLIENT_UUID else DeviceIdMode.ANDROID_ID
            step("init device id", detail = { "mode=$mode" }) {
                withContext(dispatchers.io) { restConfig.initDeviceId(mode) }
            }
        }
        val deviceId = step("wait for device id") {
            withContext(dispatchers.io) { configRepository.awaitForDeviceId() }
        }
        step("cache configuration in local settings") {
            withContext(dispatchers.io) { prefs.cacheConfiguration(deviceId.id, config) }
        }
        step("apply session duration") {
            sessionHandler.setSessionResetDuration(config.sessionDuration)
        }
    }
}
