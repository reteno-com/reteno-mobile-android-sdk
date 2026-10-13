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
}
