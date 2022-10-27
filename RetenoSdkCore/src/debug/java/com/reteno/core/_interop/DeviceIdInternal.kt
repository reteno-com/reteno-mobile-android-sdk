package com.reteno.core._interop

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode

object DeviceIdInternal {

    fun DeviceId.getIdInternal(): String = id

    fun DeviceId.getExternalIdInternal(): String? = externalId

    fun DeviceId.getModeInternal(): DeviceIdMode = mode
}