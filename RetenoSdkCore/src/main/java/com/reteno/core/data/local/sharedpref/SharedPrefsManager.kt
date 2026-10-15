package com.reteno.core.data.local.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.lifecycle.SessionTracker
import java.util.UUID

internal class SharedPrefsManager(
    private val context: Context
) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getDeviceIdUuid(): String {
        val stored = string(PREF_KEY_DEVICE_ID)
        if (!stored.isNullOrBlank()) return stored
        val randomId = UUID.randomUUID().toString()
        save { putString(PREF_KEY_DEVICE_ID, randomId) }
        return randomId
    }

    fun getIamBaseUrl(): String? = string(PREF_KEY_IAM_BASE_URL)

    fun saveFcmToken(token: String) = save { putString(PREF_KEY_FCM_TOKEN, token) }

    fun getFcmToken(): String = string(PREF_KEY_FCM_TOKEN).orEmpty()

    fun saveDefaultNotificationChannel(defaultChannel: String) =
        save { putString(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT, defaultChannel) }

    fun getDefaultNotificationChannel(): String = string(PREF_KEY_NOTIFICATION_CHANNEL_DEFAULT).orEmpty()

    fun saveNotificationsEnabled(enabled: Boolean) = save { putBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, enabled) }

    fun isNotificationsEnabled(): Boolean = sharedPreferences.getBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, false)

    fun saveDeviceRegistered(registered: Boolean) = save { putBoolean(PREF_KEY_DEVICE_REGISTERED, registered) }

    fun isDeviceRegistered(): Boolean = sharedPreferences.getBoolean(PREF_KEY_DEVICE_REGISTERED, false)

    fun saveIamBaseHtmlVersion(version: String) = save { putString(PREF_KEY_IAM_BASE_HTML_VERSION, version) }

    fun getIamBaseHtmlVersion(): String? = string(PREF_KEY_IAM_BASE_HTML_VERSION)

    fun clearBaseHtmlContent() = save { remove(PREF_KEY_IAM_BASE_HTML_CONTENT) }

    fun getIamBaseHtmlContent(): String = string(PREF_KEY_IAM_BASE_HTML_CONTENT).orEmpty()

    fun saveIamEtag(etag: String?) = save { putString(PREF_KEY_IAM_ETAG, etag) }

    fun getIamEtag(): String? = string(PREF_KEY_IAM_ETAG)

    fun saveAppStoppedTimestamp(appStoppedTimestamp: Long) =
        save { putLong(PREF_KEY_APP_STOPPED_TIMESTAMP, appStoppedTimestamp) }

    fun getLastInteractionTime(): Long = sharedPreferences.getLong(PREF_KEY_APP_INTERACTION_TIMESTAMP, 0L)

    fun saveSessionStartTimestamp(sessionStartTimestamp: Long) =
        save { putLong(PREF_KEY_SESSION_START_TIMESTAMP, sessionStartTimestamp) }

    fun getSessionStartTimestamp(): Long = sharedPreferences.getLong(PREF_KEY_SESSION_START_TIMESTAMP, 0L)

    fun saveSessionId(sessionId: String) = save { putString(PREF_KEY_SESSION_ID, sessionId) }

    fun getSessionId(): String? = string(PREF_KEY_SESSION_ID)

    fun batchSaveSession(sessionTime: Long, lastInteractionTime: Long) = save {
        putLong(PREF_KEY_APP_SESSION_TIME, sessionTime)
        putLong(PREF_KEY_APP_INTERACTION_TIMESTAMP, lastInteractionTime)
    }

    fun saveAppSessionTime(appSessionTime: Long) = save { putLong(PREF_KEY_APP_SESSION_TIME, appSessionTime) }

    fun getAppSessionTime(): Long = sharedPreferences.getLong(PREF_KEY_APP_SESSION_TIME, 0L)

    fun getAppVersion(): String = string(PREF_KEY_APP_VERSION).orEmpty()

    fun saveAppVersion(version: String) = save { putString(PREF_KEY_APP_VERSION, version) }

    fun getAppBuildNumber(): Long = sharedPreferences.getLong(PREF_KEY_APP_BUILD_NUMBER, 0L)

    fun saveAppBuildNumber(number: Long) = save { putLong(PREF_KEY_APP_BUILD_NUMBER, number) }

    fun getOpenCount(): Int = sharedPreferences.getInt(PREF_KEY_APP_OPEN_COUNT, 0)

    fun batchSave(openCount: Int, backgroundCount: Int, lastInteractionTime: Long) = save {
        putInt(PREF_KEY_APP_OPEN_COUNT, openCount)
        putInt(PREF_KEY_APP_BG_COUNT, backgroundCount)
        putLong(PREF_KEY_APP_INTERACTION_TIMESTAMP, lastInteractionTime)
    }

    fun saveOpenCount(count: Int) = save { putInt(PREF_KEY_APP_OPEN_COUNT, count) }

    fun getBackgroundCount(): Int = sharedPreferences.getInt(PREF_KEY_APP_BG_COUNT, 0)

    fun saveBackgroundCount(count: Int) = save { putInt(PREF_KEY_APP_BG_COUNT, count) }

    fun saveExternalUserId(id: String?) = save { putString(PREF_KEY_EXTERNAL_DEVICE_ID, id) }

    fun getExternalUserId(): String? = string(PREF_KEY_EXTERNAL_DEVICE_ID)

    fun saveDeviceIdSuffix(suffix: String?) = save { putString(PREF_KEY_DEVICE_SUFFIX, suffix) }

    fun getDeviceIdSuffix(): String? = string(PREF_KEY_DEVICE_SUFFIX)

    fun saveDevicePhone(phone: String?) = save { putString(PREF_KEY_DEVICE_PHONE, phone) }

    fun getPhone(): String? = string(PREF_KEY_DEVICE_PHONE)

    fun saveDeviceEmail(email: String?) = save { putString(PREF_KEY_DEVICE_EMAIL, email) }

    fun getEmail(): String? = string(PREF_KEY_DEVICE_EMAIL)

    fun cacheConfiguration(deviceId: String, config: RetenoConfig) = save {
        val tracking = config.lifecycleTrackingOptions
        putString(PREF_KEY_CONFIG_LAST_STORED_ID, deviceId)
        putBoolean(PREF_KEY_CONFIG_IN_APP, config.isPausedInAppMessages)
        putBoolean(PREF_KEY_CONFIG_LC_TRACK, tracking.appLifecycleEnabled)
        putBoolean(PREF_KEY_CONFIG_FG_LC_TRACK, tracking.foregroundLifecycleEnabled)
        putBoolean(PREF_KEY_CONFIG_SESS_TRACK, tracking.sessionStartEventsEnabled)
        putBoolean(PREF_KEY_CONFIG_SESS_END_TRACK, tracking.sessionEndEventsEnabled)
        putBoolean(PREF_KEY_CONFIG_PUSH_TRACK, tracking.pushSubscriptionEnabled)
        putLong(PREF_KEY_CONFIG_SESS_DURATION, config.sessionDuration)
        putString(PREF_KEY_CONFIG_ACCESS, config.accessKey)
        putBoolean(PREF_KEY_CONFIG_PUSH_IN_APP, config.isPausedPushInAppMessages)
        putString(PREF_KEY_CONFIG_PLATFORM, config.platform)
        putBoolean(PREF_KEY_CONFIG_DEBUG, config.isDebug)
        putBoolean(PREF_KEY_CONFIG_HAS_CUSTOM_ID, config.userIdProvider != null)
    }

    fun getCachedConfiguration(): RetenoConfig? {
        if (string(PREF_KEY_CONFIG_LAST_STORED_ID) == null) return null
        val tracking = LifecycleTrackingOptions(
            appLifecycleEnabled = sharedPreferences.getBoolean(PREF_KEY_CONFIG_LC_TRACK, true),
            foregroundLifecycleEnabled = sharedPreferences.getBoolean(PREF_KEY_CONFIG_FG_LC_TRACK, false),
            sessionStartEventsEnabled = sharedPreferences.getBoolean(PREF_KEY_CONFIG_SESS_TRACK, true),
            sessionEndEventsEnabled = sharedPreferences.getBoolean(PREF_KEY_CONFIG_SESS_END_TRACK, false),
            pushSubscriptionEnabled = sharedPreferences.getBoolean(PREF_KEY_CONFIG_PUSH_TRACK, true)
        )
        val builder = RetenoConfig.Builder()
            .accessKey(string(PREF_KEY_CONFIG_ACCESS).orEmpty())
            .lifecycleTrackingOptions(tracking)
            .pauseInAppMessages(sharedPreferences.getBoolean(PREF_KEY_CONFIG_IN_APP, false))
            .pausePushInAppMessages(sharedPreferences.getBoolean(PREF_KEY_CONFIG_PUSH_IN_APP, false))
            .setPlatform(string(PREF_KEY_CONFIG_PLATFORM).orEmpty())
            .setDebug(sharedPreferences.getBoolean(PREF_KEY_CONFIG_DEBUG, false))
            .sessionDuration(
                sharedPreferences.getLong(PREF_KEY_CONFIG_SESS_DURATION, SessionTracker.DEFAULT_SESSION_RESET_TIME)
            )
        if (sharedPreferences.getBoolean(PREF_KEY_CONFIG_HAS_CUSTOM_ID, false)) {
            builder.customDeviceIdProvider { DeviceId.getIdBodyFrom(getLastStoredId()) }
        }
        return builder.build()
    }

    fun getLastStoredId(): String = string(PREF_KEY_CONFIG_LAST_STORED_ID).orEmpty()

    private fun string(key: String): String? = sharedPreferences.getString(key, null)

    private fun save(change: SharedPreferences.Editor.() -> Unit) {
        val editor = sharedPreferences.edit()
        editor.change()
        editor.apply()
    }

    companion object {
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
        private const val PREF_KEY_CONFIG_FG_LC_TRACK = "config_foreground_lifecycle_tracking"
        private const val PREF_KEY_CONFIG_SESS_TRACK = "config_session_tracking"
        private const val PREF_KEY_CONFIG_SESS_END_TRACK = "config_session_end_tracking"
        private const val PREF_KEY_CONFIG_SESS_DURATION = "config_session_duration"
        private const val PREF_KEY_CONFIG_PUSH_TRACK = "config_push_tracking"
        private const val PREF_KEY_CONFIG_ACCESS = "config_access_key"
        private const val PREF_KEY_CONFIG_PUSH_IN_APP = "config_paused_push_in_app"
        private const val PREF_KEY_CONFIG_PLATFORM = "config_platform"
        private const val PREF_KEY_CONFIG_DEBUG = "config_debug"
        private const val PREF_KEY_CONFIG_LAST_STORED_ID = "config_last_id"
        private const val PREF_KEY_CONFIG_HAS_CUSTOM_ID = "config_has_custom_id"
    }
}
