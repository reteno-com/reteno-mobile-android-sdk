package com.reteno.core.di.provider

import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.di.base.ProviderWeakReference

class RetenoActivityHelperProvider() :
    ProviderWeakReference<RetenoActivityHelper>() {

    override fun create(): RetenoActivityHelper {
        return RetenoActivityHelper()
    }
}