package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.device.DeviceOsRemote
import com.reteno.core.fixtures.stubRetenoLogEventDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class LogEventMapperTest {

    @Test
    fun `should send every stored log event field with lower case level`() = runUnitTest {
        given()
        val event = stubRetenoLogEventDb()

        whenn()
        val remote = event.toRemote()

        then()
        val expected = listOf(
            DeviceOsRemote.ANDROID, "14", "1.0 (1)", "Pixel", "2.0.0", "device-body", "com.example.app", "error",
            "Action Sync failed"
        )
        val actual = listOf(
            remote.platformName, remote.osVersion, remote.version, remote.device, remote.sdkVersion,
            remote.deviceId, remote.bundleId, remote.logLevel, remote.errorMessage
        )
        assertEquals(expected, actual)
    }
}
