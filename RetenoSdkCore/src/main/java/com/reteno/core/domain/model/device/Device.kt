package com.reteno.core.domain.model.device

import android.content.Context
import android.telephony.TelephonyManager
import com.reteno.core.Reteno
import com.reteno.core.RetenoImpl
import com.reteno.core.util.DeviceInfo
import com.reteno.core.util.Logger
import java.util.Locale
import java.util.TimeZone


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
    val advertisingId: String?,
    val email: String?,
    val phone: String?
) {
    companion object {
        private val TAG: String = Device::class.java.simpleName

        @JvmStatic
        fun createDevice(
            deviceId: String,
            externalUserId: String? = null,
            pushToken: String? = null,
            pushSubscribed: Boolean? = null,
            advertisingId: String? = null,
            email: String? = null,
            phone: String? = null
        ): Device {
            val device = Device(
                deviceId = deviceId,
                externalUserId = externalUserId,
                pushToken = pushToken,
                pushSubscribed = pushSubscribed,
                category = fetchDeviceCategory(),
                osType = DeviceOS.ANDROID,
                osVersion = DeviceInfo.fetchOsVersion(),
                deviceModel = DeviceInfo.fetchDeviceModel(),
                appVersion = DeviceInfo.fetchAppVersion(),
                languageCode = fetchLanguageCode(),
                timeZone = fetchTimeZone(),
                advertisingId = advertisingId,
                email = email,
                phone = phone
            )
            /*@formatter:off*/ Logger.i(TAG, "createDevice(): ", "deviceId = [" , deviceId , "], externalUserId = [" , externalUserId , "], pushToken = [" , pushToken , "], advertisingId = [" , advertisingId , "]")
            /*@formatter:on*/
            return device
        }


        internal fun fetchDeviceCategory(): DeviceCategory {
            val context = RetenoImpl.instance.application
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