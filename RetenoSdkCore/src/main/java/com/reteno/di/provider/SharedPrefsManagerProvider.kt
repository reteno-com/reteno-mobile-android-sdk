package com.reteno.di.provider

import com.reteno.di.base.ProviderWeakReference
import com.reteno.util.SharedPrefsManager

class SharedPrefsManagerProvider :
    ProviderWeakReference<SharedPrefsManager>() {

    override fun create(): SharedPrefsManager {
        return SharedPrefsManager()
    }
}