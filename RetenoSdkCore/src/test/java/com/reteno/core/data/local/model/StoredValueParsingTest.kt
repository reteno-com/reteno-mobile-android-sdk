package com.reteno.core.data.local.model

import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StoredValueParsingTest {

    @Test
    fun `should read stored flags and nothing for unknown values`() = runUnitTest {
        given()
        val stored = listOf("TRUE", "FALSE", "maybe", null)

        whenn()
        val read = stored.map { BooleanDb.fromString(it) }

        then()
        assertEquals(listOf(BooleanDb.TRUE, BooleanDb.FALSE, null, null), read)
    }

    @Test
    fun `should read stored log levels and debug for unknown values`() = runUnitTest {
        given()
        val stored = listOf("DEBUG", "INFO", "WARNING", "ERROR", "FATAL", "TRACE", null)

        whenn()
        val read = stored.map { LogLevelDb.fromString(it) }

        then()
        val expected = listOf(
            LogLevelDb.DEBUG,
            LogLevelDb.INFO,
            LogLevelDb.WARNING,
            LogLevelDb.ERROR,
            LogLevelDb.FATAL,
            LogLevelDb.DEBUG,
            LogLevelDb.DEBUG
        )
        assertEquals(expected, read)
    }

    @Test
    fun `should read stored interaction statuses and delivered for unknown values`() = runUnitTest {
        given()
        val stored = listOf("DELIVERED", "CLICKED", "OPENED", "SEEN", null)

        whenn()
        val read = stored.map { InteractionStatusDb.fromString(it) }

        then()
        val expected = listOf(
            InteractionStatusDb.DELIVERED,
            InteractionStatusDb.CLICKED,
            InteractionStatusDb.OPENED,
            InteractionStatusDb.DELIVERED,
            InteractionStatusDb.DELIVERED
        )
        assertEquals(expected, read)
    }

    @Test
    fun `should read stored recommendation event types and nothing for unknown values`() = runUnitTest {
        given()
        val stored = listOf("IMPRESSIONS", "CLICKS", "VIEWS", null)

        whenn()
        val read = stored.map { RecomEventTypeDb.fromString(it) }

        then()
        assertEquals(listOf(RecomEventTypeDb.IMPRESSIONS, RecomEventTypeDb.CLICKS, null, null), read)
    }

    @Test
    fun `should read stored os and android for unknown values`() = runUnitTest {
        given()
        val stored = listOf("ANDROID", "IOS", "WEB", null)

        whenn()
        val read = stored.map { DeviceOsDb.fromString(it) }

        then()
        assertEquals(listOf(DeviceOsDb.ANDROID, DeviceOsDb.IOS, DeviceOsDb.ANDROID, DeviceOsDb.ANDROID), read)
    }

    @Test
    fun `should read stored device category and mobile for unknown or missing values`() = runUnitTest {
        given()
        val stored = listOf("MOBILE", "TABLET", "TV", null)

        whenn()
        val read = stored.map { DeviceCategoryDb.fromString(it) }

        then()
        val expected = listOf(
            DeviceCategoryDb.MOBILE,
            DeviceCategoryDb.TABLET,
            DeviceCategoryDb.MOBILE,
            DeviceCategoryDb.MOBILE
        )
        assertEquals(expected, read)
    }
}
