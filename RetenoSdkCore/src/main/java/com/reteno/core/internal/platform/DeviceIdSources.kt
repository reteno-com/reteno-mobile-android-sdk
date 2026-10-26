package com.reteno.core.internal.platform

import android.content.Context
import android.provider.Settings
import com.google.android.gms.appset.AppSet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface DeviceIdSources {
    fun androidId(): String?

    suspend fun appSetId(): String
}

internal class AndroidDeviceIdSources(private val context: Context) : DeviceIdSources {

    override fun androidId(): String? = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    override suspend fun appSetId(): String = suspendCancellableCoroutine { continuation ->
        AppSet.getClient(context).appSetIdInfo
            .addOnSuccessListener { continuation.resume(it.id) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
}
