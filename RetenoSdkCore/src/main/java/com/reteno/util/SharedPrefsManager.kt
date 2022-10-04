package com.reteno.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


internal object SharedPrefsManager {

    private const val SHARED_PREF_NAME = "reteno_shared_prefs"
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
}