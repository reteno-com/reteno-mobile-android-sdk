package com.reteno.push.interceptor.click

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RetenoRestClient
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.getResolveInfoList
import com.reteno.push.Constants

internal object IntentHandler {

    val TAG: String = IntentHandler::class.java.simpleName

    internal fun getDeepLinkIntent(bundle: Bundle): Intent? {
        val esLink = bundle.getString(Constants.KEY_ES_LINK_UNWRAPPED)
            ?: bundle.getString(Constants.KEY_ES_LINK_WRAPPED) ?: return null

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(esLink))
        deepLinkIntent.putExtras(bundle)
        deepLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return deepLinkIntent
    }

    internal fun resolveIntentActivity(context: Context, deepLinkIntent: Intent) {
        val resolveInfoList = context.getResolveInfoList(deepLinkIntent)
        if (resolveInfoList.isNotEmpty()) {
            for (resolveInfo in resolveInfoList) {
                if (resolveInfo?.activityInfo != null && resolveInfo.activityInfo.name != null) {
                    if (resolveInfo.activityInfo.name.contains(context.packageName)) {
                        deepLinkIntent.setPackage(resolveInfo.activityInfo.packageName)
                    }
                    return
                }
            }
        }
    }

    object AppLaunchIntent {
        internal fun getAppLaunchIntent(context: Context) =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
    }
}