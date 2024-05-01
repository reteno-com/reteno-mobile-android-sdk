package com.reteno.core.data.repository

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext


internal class ConfigRepositoryImpl(
    private val sharedPrefsManager: SharedPrefsManager,
    private val restConfig: RestConfig
) : ConfigRepository {

    init {
        trackFirstLaunch()
    }

    override fun setExternalUserId(externalId: String?) {
        restConfig.setExternalUserId(externalId)
    }

    override fun getDeviceId(): DeviceId = restConfig.deviceId

    override suspend fun awaitForDeviceId(): DeviceId {
        while (coroutineContext.isActive && restConfig.deviceId.id == "") {
            delay(30L)
        }
        return restConfig.deviceId
    }

    override fun saveFcmToken(token: String) {
        sharedPrefsManager.saveFcmToken(token)
    }

    override fun getFcmToken(callback: (String) -> Unit) {
        sharedPrefsManager.getFcmToken()
            .takeIf { it.isNotEmpty() }
            ?.let { callback.invoke(it) }
            ?: run {
                getAndSaveFreshFcmToken { token ->
                    token?.takeIf { it.isNotEmpty() }?.let { callback.invoke(it) }
                }
            }
    }

    private fun getAndSaveFreshFcmToken(callback: (String?) -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "getAndSaveFreshFcmToken(): ", "")
        /*@formatter:on*/
        val firebaseMessaging = FirebaseMessaging.getInstance()
        if (firebaseMessaging.isAutoInitEnabled) {
            firebaseMessaging.token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    /*@formatter:off*/Logger.d(TAG, "Fetching FCM registration token failed", task.exception ?: Throwable(""))
                    /*@formatter:on*/
                    return@OnCompleteListener
                }

                val freshToken = task.result
                /*@formatter:off*/Logger.d(TAG, "getAndSaveFreshFcmToken()", "result: $freshToken")
                /*@formatter:on*/
                sharedPrefsManager.getFcmToken().takeIf { it.isNotEmpty() } ?: run {
                    saveFcmToken(freshToken)
                    callback.invoke(freshToken)
                }
            })
        } else {
            /*@formatter:off*/ Logger.d(TAG, "setting AutoInitEnabled = false. cannot initiate FirebaseMessaging")
            /*@formatter:on*/
            saveFcmToken("")
            callback.invoke(null)
        }
    }


    override fun saveDefaultNotificationChannel(channel: String) {
        sharedPrefsManager.saveDefaultNotificationChannel(channel)
    }

    override fun getDefaultNotificationChannel(): String =
        sharedPrefsManager.getDefaultNotificationChannel()

    override fun saveNotificationsEnabled(enabled: Boolean) {
        sharedPrefsManager.saveNotificationsEnabled(enabled)
    }

    override fun isNotificationsEnabled(): Boolean =
        sharedPrefsManager.isNotificationsEnabled()

    override fun saveDeviceRegistered(registered: Boolean) =
        sharedPrefsManager.saveDeviceRegistered(registered)

    override fun isDeviceRegistered(): Boolean =
        sharedPrefsManager.isDeviceRegistered()

    override fun isFirstLaunch(): Boolean = sharedPrefsManager.isFirstLaunch()

    override fun getAppVersion(): String = sharedPrefsManager.getAppVersion()

    override fun saveAppVersion(version: String) {
        sharedPrefsManager.saveAppVersion(version)
    }

    override fun getAppBuildNumber(): Long = sharedPrefsManager.getAppBuildNumber()

    override fun saveAppBuildNumber(number: Long) {
        sharedPrefsManager.saveAppBuildNumber(number)
    }

    private fun trackFirstLaunch() {
        sharedPrefsManager.setFirstLaunch(sharedPrefsManager.isDeviceRegistered())
    }

    companion object {
        private val TAG: String = ConfigRepositoryImpl::class.java.simpleName
    }
}

