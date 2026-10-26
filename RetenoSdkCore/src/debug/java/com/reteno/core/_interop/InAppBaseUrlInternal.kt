package com.reteno.core._interop

import android.content.Context
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

object InAppBaseUrlInternal {

    fun baseUrl(context: Context): String? = prefs(context).getIamBaseUrl()

    fun setBaseUrl(context: Context, url: String?) = prefs(context).saveIamBaseUrl(url)

    private fun prefs(context: Context) = SharedPrefsManager(context.applicationContext)
}
