package com.reteno.core.di

import com.reteno.core.data.local.config.DeviceIdentity
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.permission.PermissionActivityDelegate

internal interface SdkInfrastructure {
    val prefs: SharedPrefsManager
    val restConfig: DeviceIdentity
    val restClient: RestClient
    val configRepository: ConfigRepository
    val fileManager: FileManager
    val activityHelper: RetenoActivityHelper
    val sessionHandler: RetenoSessionHandler
    val db: DatabaseManagers
}

internal interface FacadeInfrastructure {
    val configuration: SdkConfiguration
    val permissions: PermissionActivityDelegate
    val actions: SdkActions
}
