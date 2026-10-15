package com.reteno.core.internal.platform

import android.app.Activity
import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.R
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.DeviceInfo
import com.reteno.core.util.Util
import com.reteno.core.view.iam.AndroidInAppScreen
import java.util.Locale
import java.util.TimeZone

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

    fun bundledWidget(): String?

    fun inAppScreen(): InAppScreen

    fun logEventOrigin(): LogEventOrigin
}

internal class AndroidAppPlatform(
    private val context: Context,
    private val workManager: Lazy<WorkManager>,
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
        if (context !is Configuration.Provider) workManager.value
    }

    override fun scheduleBackgroundSync() {
        PushDataWorker.enqueuePeriodicWork(workManager.value)
    }

    override fun cancelBackgroundSync() {
        workManager.value.cancelUniqueWork(PushDataWorker.PUSH_DATA_WORK_NAME)
    }

    override fun bundledWidget(): String? = Util.readFromRaw(context, R.raw.widget)

    override fun inAppScreen(): InAppScreen = AndroidInAppScreen(context, activityHelper, broadcaster)

    override fun logEventOrigin(): LogEventOrigin = LogEventOrigin(
        packageName = context.packageName,
        osVersion = DeviceInfo.fetchOsVersion(),
        appVersion = appInfo.installedVersionName(),
        deviceModel = DeviceInfo.fetchDeviceModel()
    )

    override fun createDevice(
        deviceId: DeviceId,
        pushToken: String,
        pushSubscribed: Boolean
    ): Device = Device(
        deviceId = deviceId.id,
        deviceIdSuffix = deviceId.idSuffix,
        externalUserId = deviceId.externalId,
        pushToken = pushToken,
        pushSubscribed = pushSubscribed,
        category = deviceCategory(),
        osType = DeviceOS.ANDROID,
        osVersion = DeviceInfo.fetchOsVersion(),
        deviceModel = DeviceInfo.fetchDeviceModel(),
        appVersion = appInfo.installedVersionName(),
        languageCode = Locale.getDefault().toLanguageTag(),
        timeZone = TimeZone.getDefault().toZoneId().id,
        advertisingId = null,
        email = deviceId.email,
        phone = deviceId.phone
    )

    private fun deviceCategory(): DeviceCategory {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return DeviceCategory.MOBILE
        if (telephony.phoneType == TelephonyManager.PHONE_TYPE_NONE) return DeviceCategory.TABLET
        return DeviceCategory.MOBILE
    }
}
