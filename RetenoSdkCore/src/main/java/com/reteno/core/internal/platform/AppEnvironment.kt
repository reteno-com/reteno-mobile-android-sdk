package com.reteno.core.internal.platform

import android.content.Context
import android.content.res.Resources
import android.icu.util.ULocale
import android.telephony.TelephonyManager
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.R
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.util.DeviceInfo
import com.reteno.core.util.Util
import java.util.Locale
import java.util.TimeZone

internal interface AppEnvironment {
    fun installedVersion(): AppVersion

    fun createDevice(deviceId: DeviceId, pushToken: String, pushSubscribed: Boolean, languageCode: String): Device

    fun systemLocale(): Locale

    fun appLocale(): Locale

    fun defaultLocale(): Locale

    fun likelyRegion(language: String): String

    fun logEventOrigin(): LogEventOrigin

    fun bundledWidget(): String?

    fun notificationsEnabled(channelId: String): Boolean
}

internal class AndroidAppEnvironment(private val context: Context) : AppEnvironment {

    private val appInfo = AppInfo(context)

    override fun installedVersion(): AppVersion = appInfo.installedVersion()

    override fun createDevice(
        deviceId: DeviceId,
        pushToken: String,
        pushSubscribed: Boolean,
        languageCode: String
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
        languageCode = languageCode,
        timeZone = TimeZone.getDefault().toZoneId().id,
        advertisingId = null,
        email = deviceId.email,
        phone = deviceId.phone
    )

    override fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]

    override fun appLocale(): Locale = context.resources.configuration.locales[0]

    override fun defaultLocale(): Locale = Locale.getDefault()

    override fun likelyRegion(language: String): String = ULocale.addLikelySubtags(ULocale(language)).country

    override fun logEventOrigin(): LogEventOrigin = LogEventOrigin(
        packageName = context.packageName,
        osVersion = DeviceInfo.fetchOsVersion(),
        appVersion = appInfo.installedVersionName(),
        deviceModel = DeviceInfo.fetchDeviceModel()
    )

    override fun bundledWidget(): String? = Util.readFromRaw(context, R.raw.widget)

    override fun notificationsEnabled(channelId: String): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannelCompat(channelId) ?: return true
        return channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    private fun deviceCategory(): DeviceCategory {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return DeviceCategory.MOBILE
        if (telephony.phoneType == TelephonyManager.PHONE_TYPE_NONE) return DeviceCategory.TABLET
        return DeviceCategory.MOBILE
    }
}
