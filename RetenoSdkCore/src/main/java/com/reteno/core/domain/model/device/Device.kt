package com.reteno.core.domain.model.device

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.pm.PackageInfoCompat
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import java.util.*


data class Device(
    val deviceId: String,
    val externalUserId: String?,
    val pushToken: String?,
    val pushSubscribed: Boolean?,
    val category: DeviceCategory,
    val osType: DeviceOS = DeviceOS.ANDROID,
    val osVersion: String?,
    val deviceModel: String?,
    val appVersion: String?,
    val languageCode: String?,
    val timeZone: String?,
    val advertisingId: String?
) {
    companion object {
        private val TAG: String = Device::class.java.simpleName

        @JvmStatic
        fun createDevice(
            deviceId: String,
            externalUserId: String? = null,
            pushToken: String? = null,
            pushSubscribed: Boolean? = null,
            advertisingId: String? = null
        ): Device {
            val device = Device(
                deviceId = deviceId,
                externalUserId = externalUserId,
                pushToken = pushToken,
                pushSubscribed = pushSubscribed,
                category = fetchDeviceCategory(),
                osType = DeviceOS.ANDROID,
                osVersion = fetchOsVersion(),
                deviceModel = fetchDeviceModel(),
                appVersion = fetchAppVersion(),
                languageCode = fetchLanguageCode(),
                timeZone = fetchTimeZone(),
                advertisingId = advertisingId
            )
            /*@formatter:off*/ Logger.i(TAG, "createDevice(): ", "deviceId = [" , deviceId , "], externalUserId = [" , externalUserId , "], pushToken = [" , pushToken , "], advertisingId = [" , advertisingId , "]")
            /*@formatter:on*/
            return device
        }


        internal fun fetchDeviceCategory(): DeviceCategory {
            val context = RetenoImpl.application
            val telephonyManager =
                context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            val noTelephony = telephonyManager?.phoneType == TelephonyManager.PHONE_TYPE_NONE
            val deviceCategory = if (noTelephony) {
                DeviceCategory.TABLET
            } else {
                DeviceCategory.MOBILE
            }
            /*@formatter:off*/ Logger.i(TAG, "fetchDeviceCategory(): ", deviceCategory)
            /*@formatter:on*/
            return deviceCategory
        }

        internal fun fetchOsVersion(): String {
            val osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            /*@formatter:off*/ Logger.i(TAG, "fetchOsVersion(): ", osVersion)
            /*@formatter:on*/
            return osVersion
        }


        internal fun fetchDeviceModel(): String {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            val deviceModel = if (model.lowercase().startsWith(manufacturer.lowercase())) {
                model.uppercase()
            } else {
                "${manufacturer.uppercase()} $model"
            }

            /*@formatter:off*/ Logger.i(TAG, "fetchDeviceModel(): ", deviceModel)
            /*@formatter:on*/
            return deviceModel
        }

        internal fun fetchAppVersion(): String? =
            try {
                val context = RetenoImpl.application

                val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageInfoFlags.of(0)
                    )
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val versionName = pInfo.versionName
                val versionCode = PackageInfoCompat.getLongVersionCode(pInfo)


                val appVersion = "$versionName ($versionCode)"
                /*@formatter:off*/ Logger.i(TAG, "fetchAppVersion(): ", appVersion)
                /*@formatter:on*/
                appVersion
            } catch (e: PackageManager.NameNotFoundException) {
                Logger.e(TAG, "fetchAppVersion(): ", e)
                null
            }

        internal fun fetchLanguageCode(): String {
            val languageCode = Locale.getDefault().toLanguageTag()
            /*@formatter:off*/ Logger.i(TAG, "fetchLanguageCode(): ", languageCode)
            /*@formatter:on*/
            return languageCode
        }

        internal fun fetchTimeZone(): String {
            val timeZone = TimeZone.getDefault().toZoneId().id
            /*@formatter:off*/ Logger.i(TAG, "fetchTimeZone(): ", timeZone)
            /*@formatter:on*/
            return timeZone
        }
    }
}