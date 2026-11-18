package com.reteno.push.interceptor.click

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen

internal fun appLaunchOptions(): Bundle? = appLaunchOptions(Build.VERSION.SDK_INT)

@SuppressLint("NewApi")
internal fun appLaunchOptions(sdkInt: Int): Bundle? {
    if (sdkInt < Build.VERSION_CODES.TIRAMISU) return null
    return ActivityOptions.makeBasic()
        .setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON)
        .toBundle()
}
