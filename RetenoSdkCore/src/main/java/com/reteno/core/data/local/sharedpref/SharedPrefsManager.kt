package com.reteno.core.data.local.sharedpref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reteno.core.R
import com.reteno.core.RetenoConfig
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.util.UUID


internal class SharedPrefsManager(
    private val context: Context
) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            SHARED_PREF_NAME,
            Context.MODE_PRIVATE
        )
    }

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

    fun getIamBaseUrl(): String? {
        val url = sharedPreferences.getString(PREF_KEY_IAM_BASE_URL, null)
        /*@formatter:off*/ Logger.i(TAG, "getIamBaseUrl(): ", "url = ", url)
        /*@formatter:on*/
        return url
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
        sharedPreferences.edit().putString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, defaultChannel)
            .apply()
    }

    fun getDefaultNotificationChannel(): String {
        val defaultChannel =
            sharedPreferences.getString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, "") ?: ""
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
            ?: Util.readFromRaw(context, R.raw.base_html)
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

    fun saveLastInteractionTime(appTimestamp: Long) {
        /*@formatter:off*/ Logger.i(TAG, "saveLastInteractionTime(): ", "appTimestamp = [", appTimestamp, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putLong(PREF_KEY_APP_INTERACTION_TIMESTAMP, appTimestamp)
            ?.apply()
    }

    fun getLastInteractionTime(): Long {
        val result = sharedPreferences.getLong(PREF_KEY_APP_INTERACTION_TIMESTAMP, 0L)
        /*@formatter:off*/ Logger.i(TAG, "getLastInteractionTime(): ", "result = ", result)
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

    fun saveSessionId(sessionId: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveSessionId(): ", "sessionId = [", sessionId, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_SESSION_ID, sessionId)
            ?.apply()
    }

    fun getSessionId(): String? {
        val result = sharedPreferences.getString(PREF_KEY_SESSION_ID, null)
        /*@formatter:off*/ Logger.i(TAG, "getSessionId(): ", "result = ", result)
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

    fun isFirstLaunch(): Boolean {
        val result = sharedPreferences.getBoolean(PREF_KEY_FIRST_LAUNCH, true)
        /*@formatter:off*/ Logger.i(TAG, "isFirstLaunch(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun setFirstLaunch(isFirstLaunch: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "setFirstLaunch(): ", "isFirstLaunch = [", isFirstLaunch, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putBoolean(PREF_KEY_FIRST_LAUNCH, isFirstLaunch)
            ?.apply()
    }

    fun getAppVersion(): String {
        val result = sharedPreferences.getString(PREF_KEY_APP_VERSION, "")
        /*@formatter:off*/ Logger.i(TAG, "getAppVersion(): ", "result = ", result)
        /*@formatter:on*/
        return result.orEmpty()
    }

    fun saveAppVersion(version: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveAppVersion(): ", "version = [", version, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_APP_VERSION, version)
            ?.apply()
    }

    fun getAppBuildNumber(): Long {
        val result = sharedPreferences.getLong(PREF_KEY_APP_BUILD_NUMBER, 0L)
        /*@formatter:off*/ Logger.i(TAG, "getAppBuildNumber(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveAppBuildNumber(number: Long) {
        /*@formatter:off*/ Logger.i(TAG, "saveAppBuildNumber(): ", "version = [", number, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putLong(PREF_KEY_APP_BUILD_NUMBER, number)
            ?.apply()
    }

    fun getOpenCount(): Int {
        val result = sharedPreferences.getInt(PREF_KEY_APP_OPEN_COUNT, 0)
        /*@formatter:off*/ Logger.i(TAG, "getOpenCount(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveOpenCount(count: Int) {
        /*@formatter:off*/ Logger.i(TAG, "saveOpenCount(): ", "count = [", count, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putInt(PREF_KEY_APP_OPEN_COUNT, count)
            ?.apply()
    }

    fun getBackgroundCount(): Int {
        val result = sharedPreferences.getInt(PREF_KEY_APP_BG_COUNT, 0)
        /*@formatter:off*/ Logger.i(TAG, "getBackgroundCount(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveBackgroundCount(count: Int) {
        /*@formatter:off*/ Logger.i(TAG, "saveBackgroundCount(): ", "count = [", count, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putInt(PREF_KEY_APP_BG_COUNT, count)
            ?.apply()
    }

    fun saveExternalUserId(id: String?) {
        /*@formatter:off*/ Logger.i(TAG, "saveExternalDeviceId(): ", "id = [", id, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_EXTERNAL_DEVICE_ID, id)
            ?.apply()
    }

    fun saveDeviceIdSuffix(suffix: String?) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceIdSuffix(): ", "suffix = [", suffix, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_DEVICE_SUFFIX, suffix)
            ?.apply()
    }

    fun getDeviceIdSuffix(): String? {
        val result = sharedPreferences.getString(PREF_KEY_DEVICE_SUFFIX, null)
        /*@formatter:off*/ Logger.i(TAG, "getDeviceIdSuffix(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun getExternalUserId(): String? {
        val result = sharedPreferences.getString(PREF_KEY_EXTERNAL_DEVICE_ID, null)
        /*@formatter:off*/ Logger.i(TAG, "getExternalDeviceId(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveDevicePhone(phone: String?) {
        /*@formatter:off*/ Logger.i(TAG, "saveDevicePhone(): ", "saveDevicePhone = [", phone, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_DEVICE_PHONE, phone)
            ?.apply()
    }

    fun getPhone(): String? {
        val result = sharedPreferences.getString(PREF_KEY_DEVICE_PHONE, null)
        /*@formatter:off*/ Logger.i(TAG, "getPhone(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun saveDeviceEmail(email: String?) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceEmail(): ", "saveDevicePhone = [", email, "]")
        /*@formatter:on*/
        sharedPreferences.edit()
            ?.putString(PREF_KEY_DEVICE_EMAIL, email)
            ?.apply()
    }

    fun getEmail(): String? {
        val result = sharedPreferences.getString(PREF_KEY_DEVICE_EMAIL, null)
        /*@formatter:off*/ Logger.i(TAG, "getEmail(): ", "result = ", result)
        /*@formatter:on*/
        return result
    }

    fun cacheConfiguration(deviceId: String, config: RetenoConfig) {
        sharedPreferences.edit {
            putString(PREF_KEY_CONFIG_LAST_STORED_ID, deviceId)
            putBoolean(PREF_KEY_CONFIG_IN_APP, config.isPausedInAppMessages)
            putBoolean(PREF_KEY_CONFIG_LC_TRACK, config.lifecycleTrackingOptions.appLifecycleEnabled)
            putBoolean(PREF_KEY_CONFIG_SESS_TRACK, config.lifecycleTrackingOptions.sessionEventsEnabled)
            putBoolean(PREF_KEY_CONFIG_PUSH_TRACK, config.lifecycleTrackingOptions.pushSubscriptionEnabled)
            putString(PREF_KEY_CONFIG_ACCESS, config.accessKey)
            putBoolean(PREF_KEY_CONFIG_PUSH_IN_APP, config.isPausedPushInAppMessages)
            putString(PREF_KEY_CONFIG_PLATFORM, config.platform)
            putBoolean(PREF_KEY_CONFIG_DEBUG, config.isDebug)
            putBoolean(PREF_KEY_CONFIG_HAS_CUSTOM_ID, config.userIdProvider != null)
        }
    }

    fun getLastStoredId(): String {
        return sharedPreferences.getString(PREF_KEY_CONFIG_LAST_STORED_ID, "").orEmpty()
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
        private const val PREF_KEY_IAM_BASE_URL = "iam_base_url"
        private const val PREF_KEY_APP_STOPPED_TIMESTAMP = "app_stopped_timestamp"
        private const val PREF_KEY_APP_INTERACTION_TIMESTAMP = "app_interaction_timestamp"
        private const val PREF_KEY_SESSION_START_TIMESTAMP = "session_start_timestamp"
        private const val PREF_KEY_SESSION_ID = "session_id"
        private const val PREF_KEY_APP_SESSION_TIME = "session_time"
        private const val PREF_KEY_FIRST_LAUNCH = "first_launch"
        private const val PREF_KEY_APP_VERSION = "app_version"
        private const val PREF_KEY_APP_BUILD_NUMBER = "app_build_number"
        private const val PREF_KEY_APP_OPEN_COUNT = "open_count"
        private const val PREF_KEY_APP_BG_COUNT = "background_count"
        private const val PREF_KEY_EXTERNAL_DEVICE_ID = "external_user_id"
        private const val PREF_KEY_DEVICE_SUFFIX = "device_suffix"
        private const val PREF_KEY_DEVICE_PHONE = "device_phone"
        private const val PREF_KEY_DEVICE_EMAIL = "device_email"
        private const val PREF_KEY_CONFIG_IN_APP = "config_paused_in_app"
        private const val PREF_KEY_CONFIG_LC_TRACK = "config_lifecycle_tracking"
        private const val PREF_KEY_CONFIG_SESS_TRACK = "config_session_tracking"
        private const val PREF_KEY_CONFIG_PUSH_TRACK = "config_push_tracking"
        private const val PREF_KEY_CONFIG_ACCESS = "config_access_key"
        private const val PREF_KEY_CONFIG_PUSH_IN_APP = "config_paused_push_in_app"
        private const val PREF_KEY_CONFIG_PLATFORM = "config_platform"
        private const val PREF_KEY_CONFIG_DEBUG = "config_debug"
        private const val PREF_KEY_CONFIG_LAST_STORED_ID = "config_last_id"
        private const val PREF_KEY_CONFIG_HAS_CUSTOM_ID = "config_has_custom_id"
    }
}