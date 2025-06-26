package com.reteno.push.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.reteno.core.Reteno
import com.reteno.core.permission.AndroidPermissionChecker

internal class NotificationPermissionChecker(
    private val coreChecker: AndroidPermissionChecker
) {

    suspend fun requestPermission(): Boolean {
        if (hasPermission()) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            coreChecker.check(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }.also { granted ->
            if (granted) {
                Reteno.instance.updatePushPermissionStatus()
            }
        }
    }

    suspend fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                coreChecker.awaitContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}