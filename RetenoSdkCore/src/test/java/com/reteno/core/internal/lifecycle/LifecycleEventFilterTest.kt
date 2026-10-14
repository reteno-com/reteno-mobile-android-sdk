package com.reteno.core.internal.lifecycle

import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class LifecycleEventFilterTest {

    @Test
    fun `should allow only event types enabled in options`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.DEFAULT)

        whenn()
        val enabled = LifecycleEventType.values().associateWith { sut.isEnabled(it) }

        then()
        assertEquals(
            mapOf(
                LifecycleEventType.APP_LIFECYCLE to true,
                LifecycleEventType.FOREGROUND_LIFECYCLE to false,
                LifecycleEventType.PUSH to true,
                LifecycleEventType.SESSION_START to true,
                LifecycleEventType.SESSION_END to false
            ),
            enabled
        )
    }

    @Test
    fun `should apply updated options`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.NONE)

        whenn()
        sut.update(LifecycleTrackingOptions.ALL)

        then()
        assertEquals(true, LifecycleEventType.values().all { sut.isEnabled(it) })
    }

    @Test
    fun `should disable only app lifecycle events when app lifecycle tracking is off`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.ALL.copy(appLifecycleEnabled = false))

        whenn()
        val disabled = LifecycleEventType.values().filterNot { sut.isEnabled(it) }

        then()
        assertEquals(listOf(LifecycleEventType.APP_LIFECYCLE), disabled)
    }

    @Test
    fun `should disable only foreground lifecycle events when foreground lifecycle tracking is off`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.ALL.copy(foregroundLifecycleEnabled = false))

        whenn()
        val disabled = LifecycleEventType.values().filterNot { sut.isEnabled(it) }

        then()
        assertEquals(listOf(LifecycleEventType.FOREGROUND_LIFECYCLE), disabled)
    }

    @Test
    fun `should disable only push events when push subscription tracking is off`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.ALL.copy(pushSubscriptionEnabled = false))

        whenn()
        val disabled = LifecycleEventType.values().filterNot { sut.isEnabled(it) }

        then()
        assertEquals(listOf(LifecycleEventType.PUSH), disabled)
    }

    @Test
    fun `should disable only session start events when session start tracking is off`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.ALL.copy(sessionStartEventsEnabled = false))

        whenn()
        val disabled = LifecycleEventType.values().filterNot { sut.isEnabled(it) }

        then()
        assertEquals(listOf(LifecycleEventType.SESSION_START), disabled)
    }

    @Test
    fun `should disable only session end events when session end tracking is off`() = runUnitTest {
        given()
        val sut = LifecycleEventFilter(LifecycleTrackingOptions.ALL.copy(sessionEndEventsEnabled = false))

        whenn()
        val disabled = LifecycleEventType.values().filterNot { sut.isEnabled(it) }

        then()
        assertEquals(listOf(LifecycleEventType.SESSION_END), disabled)
    }
}
