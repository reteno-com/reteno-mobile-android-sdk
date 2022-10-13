package com.reteno.push.interceptor.click

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.reteno.push.Constants.KEY_ES_LINK
import com.reteno.push.Util
import com.reteno.util.Logger
import com.reteno.util.getResolveInfoList
import com.reteno.util.toStringVerbose

class RetenoNotificationClickedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "notification clicked, intent.extras = [" , intent.extras.toStringVerbose() , "]")
        /*@formatter:on*/

        try {
            intent.extras?.let { bundle ->
                Util.tryToSendToCustomReceiverNotificationClicked(bundle)

                getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                    launchDeeplink(deeplinkIntent)
                    return
                }

                launchApp()
                return
            }

            launchApp()
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onCreate(): ", t)
            /*@formatter:on*/
        }

        finish()
    }

    private fun launchDeeplink(deeplinkIntent: Intent) {
        resolveIntentActivity(this, deeplinkIntent)
        startActivity(deeplinkIntent)
        finish()
    }

    private fun launchApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        intent.component = launchIntent?.component
        startActivity(intent)
        finish()
    }

    private fun getDeepLinkIntent(bundle: Bundle): Intent? {
        val esLink = bundle.getString(KEY_ES_LINK) ?: return null

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(esLink))
        deepLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return deepLinkIntent
    }

    /**
     * Checks if url can be handled by current app to skip chooser dialog.
     */

    // TODO: Needs to be tested
    private fun resolveIntentActivity(context: Context, deepLinkIntent: Intent) {
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

    companion object {
        val TAG: String = RetenoNotificationClickedActivity::class.java.simpleName
    }
}