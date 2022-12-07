package com.reteno.core.di.provider

import android.content.Context
import androidx.work.WorkManager
import com.reteno.core.di.base.ProviderWeakReference

internal class WorkManagerProvider(
    private val context: Context
) : ProviderWeakReference<WorkManager>() {

    override fun create(): WorkManager {
        return WorkManager.getInstance(context)
    }
}