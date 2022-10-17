package com.reteno.di.provider

import android.content.Context
import com.reteno.di.base.ProviderWeakReference
import com.reteno.util.SharedPrefsManager

class SharedPrefsManagerProvider(private val context: Context) :
    ProviderWeakReference<SharedPrefsManager>() {

    override fun create(): SharedPrefsManager {
        return SharedPrefsManager(context)
    }
}