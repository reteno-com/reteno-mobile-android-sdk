package com.reteno.push.interceptor.click

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.reteno.core.util.getResolveInfoList
import com.reteno.push.Constants

internal object IntentHandler {

    internal fun getDeepLinkIntent(bundle: Bundle): Intent? {
        val esLink = bundle.getString(Constants.KEY_ES_LINK_UNWRAPPED)
            ?: bundle.getString(Constants.KEY_ES_LINK_WRAPPED) ?: return null

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(esLink))
        deepLinkIntent.putExtras(bundle)
        deepLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return deepLinkIntent
    }

    object AppLaunchIntent {
        internal fun getAppLaunchIntent(context: Context) =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
    }
}