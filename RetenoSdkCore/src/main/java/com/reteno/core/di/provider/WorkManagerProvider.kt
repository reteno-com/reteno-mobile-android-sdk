package com.reteno.core.di.provider

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.di.base.ProviderWeakReference
import com.reteno.core.util.Logger

internal class WorkManagerProvider(
    private val context: Context
) : ProviderWeakReference<WorkManager>() {

    override fun create(): WorkManager {
        return try {
            /*@formatter:off*/ Logger.i(TAG, "create(): ", "try to get work-manager instance if it is not initialised, will try to initialise it")
            /*@formatter:on*/
            WorkManager.getInstance(context)
        } catch (e : Exception) {
            /*@formatter:off*/ Logger.i(TAG, "create(): ", "initialising work-manager ${e.message}")
            /*@formatter:on*/
            initialiseWorkManager(context)
            WorkManager.getInstance(context)
        }
    }

    private fun initialiseWorkManager(context : Context) {
        try {
            WorkManager.initialize(
                context ,
                Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.INFO)
                    .build())
            /*@formatter:off*/ Logger.i(TAG, "initialiseWorkManager(): ", "WorkManager initialised")
            /*@formatter:on*/
        } catch (e : Exception) {
            /*@formatter:off*/ Logger.i(TAG, "initialiseWorkManager(): ", "Failed to initialize WorkManager ${e.message}")
            /*@formatter:on*/
        }
    }

    companion object {
        private val TAG: String = WorkManagerProvider::class.java.simpleName
    }
}