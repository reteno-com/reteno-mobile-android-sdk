package com.reteno.di.provider

import com.reteno.lifecycle.RetenoActivityHelper
import com.reteno.di.base.ProviderWeakReference

class RetenoActivityHelperProvider() :
    ProviderWeakReference<RetenoActivityHelper>() {

    override fun create(): RetenoActivityHelper {
        return RetenoActivityHelper()
    }
}