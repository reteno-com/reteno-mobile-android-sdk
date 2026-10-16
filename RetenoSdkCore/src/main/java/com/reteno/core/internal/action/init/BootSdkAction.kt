package com.reteno.core.internal.action.init

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.DeviceIdentity
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.AppPlatform
import kotlinx.coroutines.withContext

internal class BootSdkAction(
    private val platform: AppPlatform,
    private val prefs: SharedPrefsManager,
    private val restConfig: DeviceIdentity,
    private val applyConfig: ApplyConfig,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(): Result<Unit> = runner("BootSdk") {
        boot()
    }

    private suspend fun ActionTrace.boot() {
        runner("WarmUpStorage") {
            step("init WorkManager off the main thread") {
                withContext(dispatchers.io) { platform.initWorkManager() }
            }
            step("load local settings off the main thread") {
                withContext(dispatchers.io) { prefs.getEmail() }
            }
        }
        val cached = step(
            "read cached configuration from local settings",
            detail = { if (it == null) "none" else "found" }
        ) {
            withContext(dispatchers.io) { prefs.getCachedConfiguration() }
        }
        if (cached == null) {
            note { "no cached configuration, waiting for user configuration" }
            return
        }
        step("restore device id from local settings") {
            withContext(dispatchers.io) { restConfig.initDeviceId(DeviceIdMode.CACHE) }
        }
        applyConfig(cached, byUser = false)
    }
}
