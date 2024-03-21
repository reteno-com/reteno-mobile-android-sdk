package com.reteno.core.data.local.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.reteno.core.R
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.util.UUID


internal class SharedPrefsManager {

    private val context = RetenoImpl.application

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        SHARED_PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun getDeviceIdUuid(): String {
        val currentDeviceId = sharedPreferences.getString(PREF_KEY_DEVICE_ID, "")
        val deviceId = if (currentDeviceId.isNullOrBlank()) {
            val randomId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(PREF_KEY_DEVICE_ID, randomId).apply()
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
        sharedPreferences.edit().putString(PREF_KEY_FCM_TOKEN, token).apply()
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
        sharedPreferences.edit().putString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, defaultChannel).apply()
    }

    fun getDefaultNotificationChannel(): String {
        val defaultChannel = sharedPreferences.getString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, "") ?: ""
        /*@formatter:off*/ Logger.i(TAG, "getDefaultNotificationChannel(): ", "defaultChannel = ", defaultChannel)
        /*@formatter:on*/
        return defaultChannel
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "saveNotificationsEnabled(): ", "boolean = [" , enabled , "]")
        /*@formatter:on*/
        sharedPreferences.edit().putBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        val result = sharedPreferences.getBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, false)
        /*@formatter:off*/ Logger.i(TAG, "isNotificationsEnabled(): ", result)
        /*@formatter:on*/
        return result
    }

    fun saveDeviceRegistered(registered: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceRegistered(): ", "registered = [", registered, "]")
        /*@formatter:on*/
        sharedPreferences.edit().putBoolean(PREF_KEY_DEVICE_REGISTERED, registered).apply()
    }

    fun isDeviceRegistered(): Boolean {
        val result = sharedPreferences.getBoolean(PREF_KEY_DEVICE_REGISTERED, false)
        /*@formatter:off*/ Logger.i(TAG, "isDeviceRegistered(): ", result)
        /*@formatter:on*/
        return result
    }

    fun saveIamBaseHtmlVersion(version: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveIamBaseHtmlVersion(): ", "version = [", version, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_IAM_BASE_HTML_VERSION, version)
            ?.apply()
    }

    fun getIamBaseHtmlVersion(): String? {
        val version = sharedPreferences.getString(PREF_KEY_IAM_BASE_HTML_VERSION, null)
        /*@formatter:off*/ Logger.i(TAG, "getIamBaseHtmlVersion(): ", "version = [", version, "]")
        /*@formatter:on*/
        return version
    }

    fun saveIamBaseHtmlContent(baseHtml: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveIamBaseHtmlContent(): ", "baseHtml = [", baseHtml, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_IAM_BASE_HTML_CONTENT, baseHtml)
            ?.apply()
    }

    fun getIamBaseHtmlContent(): String {
        val result = sharedPreferences.getString(PREF_KEY_IAM_BASE_HTML_CONTENT, null)
            ?: Util.readFromRaw(R.raw.base_html)
            ?: ""
        /*@formatter:off*/ Logger.i(TAG, "getIamBaseHtmlContent(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveIamEtag(etag: String?) {
        /*@formatter:off*/ Logger.i(TAG, "saveIamEtag(): ", "etag = [", etag, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_IAM_ETAG, etag)
            ?.apply()
    }

    fun getIamEtag(): String? {
        val result = sharedPreferences.getString(PREF_KEY_IAM_ETAG, null)
        /*@formatter:off*/ Logger.i(TAG, "getIamEtag(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveAppStoppedTimestamp(appStoppedTimestamp: Long) {
        /*@formatter:off*/ Logger.i(TAG, "saveAppStoppedTimestamp(): ", "appStoppedTimestamp = [", appStoppedTimestamp, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putLong(PREF_KEY_APP_STOPPED_TIMESTAMP, appStoppedTimestamp)
            ?.apply()
    }

    fun getAppStoppedTimestamp(): Long {
        val result = sharedPreferences.getLong(PREF_KEY_APP_STOPPED_TIMESTAMP, 0L)
        /*@formatter:off*/ Logger.i(TAG, "getAppStoppedTimestamp(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveSessionStartTimestamp(sessionStartTimestamp: Long) {
        /*@formatter:off*/ Logger.i(TAG, "saveSessionStartTimestamp(): ", "sessionStartTimestamp = [", sessionStartTimestamp, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putLong(PREF_KEY_SESSION_START_TIMESTAMP, sessionStartTimestamp)
            ?.apply()
    }

    fun getSessionStartTimestamp(): Long {
        val result = sharedPreferences.getLong(PREF_KEY_SESSION_START_TIMESTAMP, 0L)
        /*@formatter:off*/ Logger.i(TAG, "getAppStoppedTimestamp(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveAppSessionTime(appSessionTime: Long) {
        /*@formatter:off*/ Logger.i(TAG, "saveAppSessionTime(): ", "appSessionTime = [", appSessionTime, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putLong(PREF_KEY_APP_SESSION_TIME, appSessionTime)
            ?.apply()
    }

    fun getAppSessionTime(): Long {
        val result = sharedPreferences.getLong(PREF_KEY_APP_SESSION_TIME, 0L)
        /*@formatter:off*/ Logger.i(TAG, "getAppSessionTime(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    companion object {
        private val TAG: String = SharedPrefsManager::class.java.simpleName

        private const val SHARED_PREF_NAME = "reteno_shared_prefs"
        private const val PREF_KEY_DEVICE_ID = "device_id"
        private const val PREF_KEY_FCM_TOKEN = "fcm_token"
        private const val PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT = "notification_channel_default"
        private const val PREF_KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val PREF_KEY_DEVICE_REGISTERED = "device_registered"
        private const val PREF_KEY_IAM_BASE_HTML_VERSION = "in_app_messages_base_html_version"
        private const val PREF_KEY_IAM_BASE_HTML_CONTENT = "in_app_messages_base_html_content"
        private const val PREF_KEY_IAM_ETAG = "in_app_e_tag"
        private const val PREF_KEY_APP_STOPPED_TIMESTAMP = "app_stopped_timestamp"
        private const val PREF_KEY_SESSION_START_TIMESTAMP = "session_start_timestamp"
        private const val PREF_KEY_APP_SESSION_TIME = "session_time"
    }
}