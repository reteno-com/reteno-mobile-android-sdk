package com.reteno.push.fixtures

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.reteno.push.Constants
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

internal const val STUB_INTERACTION_ID = "interaction-1"

internal fun stubPushBundle(
    interactionId: String? = STUB_INTERACTION_ID,
    vararg extras: Pair<String, String>
): Bundle = Bundle().apply {
    interactionId?.let { putString(Constants.KEY_ES_INTERACTION_ID, it) }
    putString(Constants.KEY_ES_TITLE, "Title")
    extras.forEach { (key, value) -> putString(key, value) }
}

internal fun testApplication(): Application = ApplicationProvider.getApplicationContext()

internal fun Context.notificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

internal const val LAUNCH_ACTIVITY = "com.example.MainActivity"

internal fun Context.registerLaunchActivity() {
    val launchIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(packageName)
    val resolveInfo = ResolveInfo().apply {
        activityInfo = ActivityInfo().also {
            it.packageName = packageName
            it.name = LAUNCH_ACTIVITY
        }
    }
    shadowOf(packageManager).addResolveInfoForIntent(launchIntent, resolveInfo)
    shadowOf(packageManager).addActivityIfNotPresent(ComponentName(packageName, LAUNCH_ACTIVITY))
}

internal inline fun <T> onAndroidSdk(sdk: Int, block: () -> T): T {
    val actual = Build.VERSION.SDK_INT
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    try {
        return block()
    } finally {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", actual)
    }
}

internal fun Context.applicationMetaData(): Bundle =
    shadowOf(packageManager).getInternalMutablePackageInfo(packageName).applicationInfo.metaData
