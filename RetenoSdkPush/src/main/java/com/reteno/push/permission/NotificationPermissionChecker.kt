package com.reteno.push.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.reteno.core.InternalRetenoApi
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.permission.AndroidPermissionChecker

internal class NotificationPermissionChecker(
    private val coreChecker: AndroidPermissionChecker
) {

    @OptIn(InternalRetenoApi::class)
    suspend fun requestPermission(): Boolean {
        if (hasPermission()) return true
        val granted = coreChecker.check(Manifest.permission.POST_NOTIFICATIONS)
        if (granted) RetenoInternalEntryPoint.facade.updatePushPermissionStatus()
        return granted
    }

    suspend fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            coreChecker.awaitContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
