package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.data.remote.model.logevent.RetenoLogEventRemote


internal fun RetenoLogEventDb.toRemote() = RetenoLogEventRemote(
    platformName = platformName.toRemote(),
    osVersion = osVersion,
    version = version,
    device = device,
    sdkVersion = sdkVersion,
    deviceId = deviceId,
    bundleId = bundleId,
    logLevel = logLevel.name.lowercase(),
    errorMessage = errorMessage
)