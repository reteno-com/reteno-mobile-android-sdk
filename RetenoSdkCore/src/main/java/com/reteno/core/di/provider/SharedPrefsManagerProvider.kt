package com.reteno.core.di.provider

import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

class SharedPrefsManagerProvider :
    ProviderWeakReference<SharedPrefsManager>() {

    override fun create(): SharedPrefsManager {
        return SharedPrefsManager()
    }
}