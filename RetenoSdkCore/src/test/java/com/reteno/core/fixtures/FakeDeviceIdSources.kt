package com.reteno.core.fixtures

import com.reteno.core.internal.platform.DeviceIdSources

internal class FakeDeviceIdSources : DeviceIdSources {
    var androidId: () -> String? = { "android-id" }
    var appSetId: suspend () -> String = { "app-set-id" }

    override fun androidId(): String? = androidId.invoke()

    override suspend fun appSetId(): String = appSetId.invoke()
}
