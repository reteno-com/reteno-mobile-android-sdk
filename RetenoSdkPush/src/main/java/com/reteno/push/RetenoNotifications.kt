package com.reteno.push

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.events.EventListener
import com.reteno.push.events.InAppCustomData
import com.reteno.push.events.NotificationClick
import com.reteno.push.events.NotificationCustom
import com.reteno.push.events.NotificationDelete
import com.reteno.push.events.NotificationInAppCustomDataReceived
import com.reteno.push.events.NotificationReceived
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
    val click: EventListener<Bundle> = NotificationClick

    @JvmStatic
    val close: EventListener<Bundle> = NotificationDelete

    @JvmStatic
    val received: EventListener<Bundle> = NotificationReceived

    @JvmStatic
    val inAppCustomDataReceived: EventListener<InAppCustomData> =
        NotificationInAppCustomDataReceived

    @JvmStatic
    val custom: EventListener<Bundle> = NotificationCustom

    internal var groupingRule: ((Map<String, String?>) -> String?)? = null

    @JvmStatic
    fun updateDefaultNotificationChannel(name: String? = null, description: String? = null) {
        if (name == null && description == null) return
        val facade = RetenoInternalEntryPoint.facade
        facade.executeAfterInit {
            val manager = NotificationManagerCompat.from(facade.application)
            val channelId = RetenoNotificationChannel.DEFAULT_CHANNEL_ID
            val existing = manager.getNotificationChannelCompat(channelId)
            val builder = if (existing != null) {
                existing.toBuilder()
            } else {
                NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            }
            val channel = builder.apply {
                name?.let(::setName)
                description?.let(::setDescription)
            }.build()
            manager.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    suspend fun requestNotificationPermission(): Boolean {
        val checker = RetenoInternalEntryPoint.facade.requestPermissionChecker() ?: return false
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
        val checker = RetenoInternalEntryPoint.facade.requestPermissionChecker()
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

    @JvmStatic
    @MainThread
    fun setGroupingRule(transform: ((Map<String, String?>) -> String?)?) {
        this.groupingRule = transform
    }
}