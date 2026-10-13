package com.reteno.core.internal.lifecycle

import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.recordingLogEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LifecycleEventsTest {

    private class SutFixture(scope: TestScope, options: LifecycleTrackingOptions) {
        val logged = mutableListOf<Event>()
        val traced = TestActionRunner(scope)
        val sut = LifecycleEvents(LifecycleEventFilter(options), recordingLogEvent(logged))
    }

    @Test
    fun `should log enabled lifecycle event as a named step`() = runUnitTest {
        given()
        val fixture = SutFixture(this, LifecycleTrackingOptions.ALL)
        val event = Event.applicationOpen(fromBackground = false)

        whenn()
        fixture.traced.runner("Test") { fixture.sut.log(this, event) }

        then()
        assertEquals(listOf(event.event), fixture.logged)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ log ApplicationOpened event") })
    }

    @Test
    fun `should skip disabled lifecycle event and note why`() = runUnitTest {
        given()
        val fixture = SutFixture(this, LifecycleTrackingOptions.NONE)

        whenn()
        fixture.traced.runner("Test") { fixture.sut.log(this, Event.applicationOpen(fromBackground = true)) }

        then()
        assertTrue(fixture.logged.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ ApplicationOpened event disabled by lifecycle tracking options") })
    }
}
