package com.reteno.core.internal.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal interface FcmTokenSource {
    val isAutoInitEnabled: Boolean

    suspend fun fetchToken(): String?
}

internal class FirebaseFcmTokenSource : FcmTokenSource {

    override val isAutoInitEnabled: Boolean
        get() = FirebaseMessaging.getInstance().isAutoInitEnabled

    override suspend fun fetchToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
