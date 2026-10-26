package com.reteno.core.fixtures

import android.os.Build
import org.robolectric.util.ReflectionHelpers

internal inline fun <T> onAndroidSdk(sdk: Int, block: () -> T): T {
    val actual = Build.VERSION.SDK_INT
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    try {
        return block()
    } finally {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", actual)
    }
}
