package com.reteno.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.*


internal object SharedPrefsManager {

    private const val SHARED_PREF_NAME = "reteno_shared_prefs"
    private const val PREF_KEY_DEVICE_ID = "device_id"

    private var masterKey: MasterKey? = null
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            SHARED_PREF_NAME,
            masterKey!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getDeviceIdUuid(): String {
        val currentDeviceId = sharedPreferences?.getString(PREF_KEY_DEVICE_ID, "")
        val deviceId = if (currentDeviceId.isNullOrBlank()) {
            val randomId = UUID.randomUUID().toString()
            sharedPreferences?.edit()?.putString(PREF_KEY_DEVICE_ID, randomId)?.apply()
            randomId
        } else {
            currentDeviceId
        }
        Logger.d(TAG, "getDeviceId(): ", "deviceId = [", deviceId, "]")
        return deviceId
    }

    val TAG: String = SharedPrefsManager::class.java.simpleName
}