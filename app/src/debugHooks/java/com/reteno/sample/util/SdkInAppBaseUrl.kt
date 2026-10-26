package com.reteno.sample.util

import android.content.Context
import com.reteno.core._interop.InAppBaseUrlInternal

object SdkInAppBaseUrl {
    const val isAvailable = true

    fun baseUrl(context: Context): String? = InAppBaseUrlInternal.baseUrl(context)

    fun setBaseUrl(context: Context, url: String?) = InAppBaseUrlInternal.setBaseUrl(context, url)
}
