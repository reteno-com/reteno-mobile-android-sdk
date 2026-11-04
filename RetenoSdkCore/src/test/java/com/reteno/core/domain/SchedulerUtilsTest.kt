package com.reteno.core.domain

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SchedulerUtilsTest {

    @Test
    fun `should keep user and device data for forty hours in release`() = runUnitTest {
        given()
        val debugView = false

        whenn()
        val keepFor = SchedulerUtils.outdatedDeviceAndUserTime(debugView = debugView, debugBuild = false)

        then()
        assertEquals(FORTY_HOURS, keepFor)
    }

    @Test
    fun `should keep user and device data for five minutes while debugging`() = runUnitTest {
        given()
        val modes = listOf(true to false, false to true)

        whenn()
        val keepFor = modes.map { (view, build) ->
            SchedulerUtils.outdatedDeviceAndUserTime(debugView = view, debugBuild = build)
        }

        then()
        assertEquals(listOf(FIVE_MINUTES, FIVE_MINUTES), keepFor)
    }

    @Test
    fun `should keep user and device data for five minutes in debug build`() = runUnitTest {
        given()
        val expected = FIVE_MINUTES

        whenn()
        val keepFor = SchedulerUtils.getOutdatedDeviceAndUserTime()

        then()
        assertEquals(expected, keepFor)
    }

    companion object {
        private const val FIVE_MINUTES = 5L * 60L * 1000L
        private const val FORTY_HOURS = 40L * 60L * 60L * 1000L
    }
}
