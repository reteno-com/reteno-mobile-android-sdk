package com.reteno.core.di.provider

import android.content.Context
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.di.base.ProviderWeakReference

internal class SharedPrefsManagerProvider(private val context: Context) :
    ProviderWeakReference<SharedPrefsManager>() {

    override fun create(): SharedPrefsManager {
        return SharedPrefsManager(context)
    }
}