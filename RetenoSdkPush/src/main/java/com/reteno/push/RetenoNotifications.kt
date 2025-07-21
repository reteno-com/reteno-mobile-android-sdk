package com.reteno.push

import android.Manifest
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.RetenoInternalImpl
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.permission.NotificationPermissionChecker
import com.reteno.push.permission.NotificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture


object RetenoNotifications {

    private val notificationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmStatic
    fun updateDefaultNotificationChannel(name: String? = null, description: String? = null) {
        if (name == null && description == null) return
        RetenoInternalImpl.instance.executeAfterInit {
            val channel = NotificationChannelCompat.Builder(
                RetenoNotificationChannel.DEFAULT_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                name?.let { setName(it) }
                description?.let { setDescription(it) }
            }.build()

            NotificationManagerCompat.from(RetenoInternalImpl.instance.application)
                .createNotificationChannel(channel)
        }
    }

    @JvmStatic
    suspend fun requestNotificationPermission(): Boolean {
        val checker = RetenoInternalImpl.instance.requestPermissionChecker() ?: return false
        val notificationChecker = NotificationPermissionChecker(checker)
        return withContext(Dispatchers.Main) {
            notificationChecker.requestPermission()
        }
    }

    @JvmStatic
    fun requestNotificationPermissionFuture(): CompletableFuture<Boolean> {
        return notificationScope.future {
            requestNotificationPermission()
        }
    }

    suspend fun getNotificationPermissionStatus(): NotificationStatus {
        val checker = RetenoInternalImpl.instance.requestPermissionChecker()
            ?: return NotificationStatus.DENIED
        val context = checker.awaitActivity()
        val manager = NotificationManagerCompat.from(context)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = requestNotificationPermission()
            if (hasPermission) {
                NotificationStatus.ALLOWED
            } else {
                if (context.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    NotificationStatus.DENIED
                } else {
                    NotificationStatus.PERMANENTLY_DENIED
                }
            }
        } else {
            val isNotificationsAllowed = manager.areNotificationsEnabled()
            if (isNotificationsAllowed) {
                NotificationStatus.ALLOWED
            } else {
                NotificationStatus.PERMANENTLY_DENIED
            }
        }
    }

    @JvmStatic
    fun getNotificationPermissionStatusFuture(): CompletableFuture<NotificationStatus> {
        return notificationScope.future {
            getNotificationPermissionStatus()
        }
    }
}