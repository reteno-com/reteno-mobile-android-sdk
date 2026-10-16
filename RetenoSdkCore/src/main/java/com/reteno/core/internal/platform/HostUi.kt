package com.reteno.core.internal.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.view.iam.AndroidInAppScreen

internal interface HostUi {
    fun trackActivities()

    fun watchAppLifecycle(onForeground: () -> Unit, onBackground: () -> Unit, onActivityPaused: (Activity) -> Unit)

    fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit)

    fun packageOnTop(activity: Activity): String?

    fun inAppScreen(): InAppScreen
}

internal class AndroidHostUi(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val activityHelper: RetenoActivityHelper,
    private val broadcaster: AppBroadcaster
) : HostUi {

    override fun trackActivities() {
        activityHelper.enableLifecycleCallbacks(context.applicationContext as Application)
    }

    override fun watchAppLifecycle(
        onForeground: () -> Unit,
        onBackground: () -> Unit,
        onActivityPaused: (Activity) -> Unit
    ) {
        AppLifecycleWatcher(
            lifecycle = lifecycleOwner.lifecycle,
            activityHelper = activityHelper,
            onForeground = onForeground,
            onBackground = onBackground,
            onActivityPaused = onActivityPaused
        ).register()
    }

    override fun watchFragmentScreens(onScreen: (String, ScreenTrackingTrigger) -> Unit) {
        FragmentScreenWatcher(activityHelper, onScreen).register()
    }

    override fun packageOnTop(activity: Activity): String? = TopActivity.packageOf(activity)

    override fun inAppScreen(): InAppScreen = AndroidInAppScreen(context, activityHelper, broadcaster)
}
