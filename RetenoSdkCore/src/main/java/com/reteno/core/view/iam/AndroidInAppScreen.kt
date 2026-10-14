package com.reteno.core.view.iam

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.core.net.toUri
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.internal.iam.InAppScreenEvents
import com.reteno.core.internal.platform.AppBroadcaster
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallBacksAdapter
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.container.IamContainer

internal class AndroidInAppScreen(
    private val context: Context,
    private val activityHelper: RetenoActivityHelper,
    private val broadcaster: AppBroadcaster
) : InAppScreen {

    private var events: InAppScreenEvents? = null
    private var container: IamContainer? = null

    override fun attach(events: InAppScreenEvents) {
        this.events = events
        activityHelper.registerActivityLifecycleCallbacks(
            LIFECYCLE_KEY,
            RetenoLifecycleCallBacksAdapter(
                onStart = { events.onActivityStarted() },
                onStop = { activity ->
                    if (activityHelper.currentActivity == activity) {
                        events.onActivityStopped()
                    }
                }
            )
        )
    }

    override fun create(html: IamFetchResult): Boolean = runCatching {
        container = IamContainer.create(context, html, WidgetBridge(html.id)) { events?.onDismissed(html.id) }
    }.onFailure {
        Logger.e(TAG, "create(): ", it)
    }.isSuccess

    override fun canPresent(): Boolean = activityHelper.canPresentMessages() && activityHelper.isActivityFullyReady()

    override fun present(): Boolean {
        val activity = activityHelper.currentActivity ?: return false
        val shown = container ?: return false
        return runCatching {
            shown.show(activity)
        }.onFailure {
            Logger.e(TAG, "present(): ", it)
        }.isSuccess
    }

    override fun resize(heightDp: Int) {
        container?.onHeightDefined(dpToPx(heightDp))
    }

    override fun hide() {
        container?.dismiss()
    }

    override fun destroy() {
        container?.destroy()
        container = null
    }

    override fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            (activityHelper.currentActivity ?: context).startActivity(intent)
        }.onFailure {
            Logger.e(TAG, "openLink(): ", it)
        }
    }

    override fun sendCustomData(data: Map<String, String>): Boolean =
        broadcaster.sendToApp(Constants.BROADCAST_ACTION_CUSTOM_INAPP_DATA, data) > 0

    private inner class WidgetBridge(private val htmlId: String) : RetenoAndroidHandler() {
        override fun onMessagePosted(event: String?) {
            events?.onWidgetMessage(htmlId, event)
        }
    }

    internal companion object {
        const val JS_INTERFACE_NAME = "RetenoAndroidHandler"
        private const val TAG = "AndroidInAppScreen"
        private const val LIFECYCLE_KEY = "IamView"

        fun dpToPx(dp: Int): Int = (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}
