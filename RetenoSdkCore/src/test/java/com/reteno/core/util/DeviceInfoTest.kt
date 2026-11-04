package com.reteno.core.util

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DeviceInfoTest {

    @Test
    fun `should name device by model when model starts with manufacturer`() = runUnitTest {
        given()
        val manufacturer = "Google"

        whenn()
        val model = DeviceInfo.deviceModel(manufacturer, "google Pixel 8")

        then()
        assertEquals("GOOGLE PIXEL 8", model)
    }

    @Test
    fun `should name device by manufacturer and model`() = runUnitTest {
        given()
        val manufacturer = "samsung"

        whenn()
        val model = DeviceInfo.deviceModel(manufacturer, "SM-S918B")

        then()
        assertEquals("SAMSUNG SM-S918B", model)
    }
}
