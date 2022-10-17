package com.reteno.util

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.reteno.RetenoImpl
import java.util.*


class SharedPrefsManager {

    private val context = RetenoImpl.application

    private val masterKey: MasterKey =
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SHARED_PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDeviceIdUuid(): String {
        val currentDeviceId = sharedPreferences.getString(PREF_KEY_DEVICE_ID, "")
        val deviceId = if (currentDeviceId.isNullOrBlank()) {
            val randomId = UUID.randomUUID().toString()
            sharedPreferences.edit()?.putString(PREF_KEY_DEVICE_ID, randomId)?.apply()
            randomId
        } else {
            currentDeviceId
        }
        /*@formatter:off*/ Logger.i(TAG, "getDeviceId(): ", "deviceId = [", deviceId, "]")
        /*@formatter:on*/
        return deviceId
    }

    fun saveFcmToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveFcmToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        sharedPreferences.edit()?.putString(PREF_KEY_FCM_TOKEN, token)?.apply()
    }

    fun getFcmToken(): String {
        val token = sharedPreferences.getString(PREF_KEY_FCM_TOKEN, "") ?: ""
        /*@formatter:off*/ Logger.i(TAG, "getFcmToken(): ", "token = ", token)
        /*@formatter:on*/
        return token
    }

    fun saveDefaultNotificationChannel(defaultChannel: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveDefaultNotificationChannel(): ", "defaultChannel = [" , defaultChannel , "]")
        /*@formatter:on*/
        sharedPreferences.edit()?.putString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, defaultChannel)?.apply()
    }

    fun getDefaultNotificationChannel(): String {
        val defaultChannel = sharedPreferences.getString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, "") ?: ""
        /*@formatter:off*/ Logger.i(TAG, "getDefaultNotificationChannel(): ", "defaultChannel = ", defaultChannel)
        /*@formatter:on*/
        return defaultChannel
    }

    companion object {
        val TAG: String = SharedPrefsManager::class.java.simpleName

        private const val SHARED_PREF_NAME = "reteno_shared_prefs"
        private const val PREF_KEY_DEVICE_ID = "device_id"
        private const val PREF_KEY_FCM_TOKEN = "fcm_token"
        private const val PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT = "notification_channel_default"
    }
}