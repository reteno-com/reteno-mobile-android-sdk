package com.reteno.core.di.provider

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.di.base.ProviderWeakReference

internal class SharedPrefsManagerProvider :
    ProviderWeakReference<SharedPrefsManager>() {

    override fun create(): SharedPrefsManager {
        return SharedPrefsManager()
    }
}