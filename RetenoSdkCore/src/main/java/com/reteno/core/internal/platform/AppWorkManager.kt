package com.reteno.core.internal.platform

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

internal fun lazyAppWorkManager(context: Context): Lazy<WorkManager> = lazy { appWorkManager(context) }

internal fun appWorkManager(context: Context): WorkManager =
    runCatching { WorkManager.getInstance(context) }.getOrElse {
        runCatching {
            WorkManager.initialize(context, Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build())
        }
        WorkManager.getInstance(context)
    }
