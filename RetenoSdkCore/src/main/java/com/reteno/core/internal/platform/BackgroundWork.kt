package com.reteno.core.internal.platform

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.data.workmanager.PushDataWorker

internal interface BackgroundWork {
    fun initWorkManager()

    fun scheduleBackgroundSync()

    fun cancelBackgroundSync()
}

internal class AndroidBackgroundWork(
    private val context: Context,
    private val workManager: Lazy<WorkManager>
) : BackgroundWork {

    override fun initWorkManager() {
        if (context !is Configuration.Provider) workManager.value
    }

    override fun scheduleBackgroundSync() {
        PushDataWorker.enqueuePeriodicWork(workManager.value)
    }

    override fun cancelBackgroundSync() {
        workManager.value.cancelUniqueWork(PushDataWorker.PUSH_DATA_WORK_NAME)
    }
}
