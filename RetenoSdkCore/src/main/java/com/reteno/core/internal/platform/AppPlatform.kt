package com.reteno.core.internal.platform

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.di.base.Provider
import com.reteno.core.domain.model.device.Device
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingTrigger

internal interface AppPlatform {
    fun sendBroadcast(action: String): Int

    fun installedVersion(): AppVersion

    fun packageOnTop(activity: Activity): String?

    fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit)

    fun watchAppLifecycle(onForeground: () -> Unit, onBackground: () -> Unit, onActivityPaused: (Activity) -> Unit)

    fun initWorkManager()

    fun scheduleBackgroundSync()

    fun cancelBackgroundSync()

    fun createDevice(deviceId: DeviceId, pushToken: String, pushSubscribed: Boolean): Device
}

internal class AndroidAppPlatform(
    private val context: Context,
    private val workManager: Provider<WorkManager>,
    private val lifecycleOwner: LifecycleOwner,
    private val activityHelper: RetenoActivityHelper
) : AppPlatform {

    private val broadcaster = AppBroadcaster(context)
    private val appInfo = AppInfo(context)

    override fun sendBroadcast(action: String): Int = broadcaster.send(action)

    override fun installedVersion(): AppVersion = appInfo.installedVersion()

    override fun packageOnTop(activity: Activity): String? = TopActivity.packageOf(activity)

    override fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit) {
        FragmentScreenWatcher(activityHelper, onScreen).register()
    }

    override fun watchAppLifecycle(
        onForeground: () -> Unit,
        onBackground: () -> Unit,
        onActivityPaused: (Activity) -> Unit
    ) {
        AppLifecycleWatcher(
            lifecycle = lifecycleOwner.lifecycle,
            activityHelper = activityHelper,
            onForeground = onForeground,
            onBackground = onBackground,
            onActivityPaused = onActivityPaused
        ).register()
    }

    override fun initWorkManager() {
        if (context !is Configuration.Provider) workManager.get()
    }

    override fun scheduleBackgroundSync() {
        PushDataWorker.enqueuePeriodicWork(workManager.get())
    }

    override fun cancelBackgroundSync() {
        workManager.get().cancelUniqueWork(PushDataWorker.PUSH_DATA_WORK_NAME)
    }

    override fun createDevice(
        deviceId: DeviceId,
        pushToken: String,
        pushSubscribed: Boolean
    ): Device = Device.createDevice(
        deviceId = deviceId.id,
        deviceIdSuffix = deviceId.idSuffix,
        externalUserId = deviceId.externalId,
        pushToken = pushToken,
        pushSubscribed = pushSubscribed,
        email = deviceId.email,
        phone = deviceId.phone
    )
}
