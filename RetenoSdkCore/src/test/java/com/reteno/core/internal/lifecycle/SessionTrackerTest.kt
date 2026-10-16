package com.reteno.core.internal.lifecycle

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.recordingLogEvent
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SessionTrackerTest {

    private class SutFixture(
        private val scope: TestScope,
        options: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL,
        previousSessionId: String? = null,
        previousSessionStart: Long = 0L
    ) {
        val prefs = mockk<SharedPrefsManager>(relaxed = true)
        val events = mutableListOf<Event>()
        val filter = LifecycleEventFilter(options)
        val sut = SessionTracker(
            prefs = prefs,
            filter = filter,
            logEvent = recordingLogEvent(events),
            scope = scope.workScope(),
            clock = { NOW + scope.testScheduler.currentTime },
            newSessionId = { "new-session" }
        )

        init {
            every { prefs.getSessionId() } returns previousSessionId
            every { prefs.getSessionStartTimestamp() } returns previousSessionStart
            every { prefs.getAppSessionTime() } returns 0L
            every { prefs.getOpenCount() } returns 2
            every { prefs.getBackgroundCount() } returns 1
        }

        val eventKeys: List<String>
            get() = events.map { it.eventTypeKey }

        companion object {
            const val NOW = 1_800_000_000_000L
        }
    }

    @Test
    fun `should start first session without ending a previous one`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = null)

        whenn()
        fixture.sut.start()
        runCurrent()

        then()
        assertEquals(listOf("SessionStarted"), fixture.eventKeys)
        assertEquals("new-session", fixture.sut.getSessionId())
        verify(exactly = 1) { fixture.prefs.saveSessionId("new-session") }
    }

    @Test
    fun `should end expired session before starting a new one`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "old-session", previousSessionStart = 1L)

        whenn()
        fixture.sut.start()
        runCurrent()

        then()
        assertEquals(listOf("SessionEnded", "SessionStarted"), fixture.eventKeys)
        assertTrue(fixture.events.first().params.orEmpty().any { it.value == "old-session" })
        verify(exactly = 1) { fixture.prefs.saveOpenCount(0) }
        verify(exactly = 1) { fixture.prefs.saveBackgroundCount(0) }
    }

    @Test
    fun `should keep running session that has not expired`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW - 1_000L)

        whenn()
        fixture.sut.start()
        runCurrent()

        then()
        assertTrue(fixture.events.isEmpty())
        assertEquals("current", fixture.sut.getSessionId())
        assertEquals(SutFixture.NOW - 1_000L, fixture.sut.getSessionStartTimestamp())
    }

    @Test
    fun `should not log session events disabled in options`() = runUnitTest {
        given()
        val fixture = SutFixture(this, options = LifecycleTrackingOptions.NONE, previousSessionId = "old-session")

        whenn()
        fixture.sut.start()
        runCurrent()

        then()
        assertTrue(fixture.events.isEmpty())
        assertEquals("new-session", fixture.sut.getSessionId())
    }

    @Test
    fun `should expire session after configured duration`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        fixture.sut.setSessionResetDuration(5_000L)

        whenn()
        fixture.sut.start()
        advanceTimeBy(6_000L)
        runCurrent()

        then()
        assertEquals(listOf("SessionEnded", "SessionStarted"), fixture.eventKeys)
    }

    @Test
    fun `should count foreground time every second and persist it`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)

        whenn()
        fixture.sut.start()
        advanceTimeBy(3_000L)
        runCurrent()

        then()
        assertEquals(3_000L, fixture.sut.getForegroundTimeMillis())
        verify(exactly = 4) { fixture.prefs.batchSaveSession(any(), any()) }
    }

    @Test
    fun `should run a single loop when started twice`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)

        whenn()
        fixture.sut.start()
        fixture.sut.start()
        advanceTimeBy(2_000L)
        runCurrent()

        then()
        verify(exactly = 3) { fixture.prefs.batchSaveSession(any(), any()) }
        verify(exactly = 1) { fixture.prefs.saveOpenCount(3) }
    }

    @Test
    fun `should stop counting and persist session state when stopped`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        fixture.sut.start()
        advanceTimeBy(2_000L)
        runCurrent()

        whenn()
        fixture.sut.stop()
        advanceTimeBy(5_000L)
        runCurrent()

        then()
        assertEquals(2_000L, fixture.sut.getForegroundTimeMillis())
        verify(exactly = 1) { fixture.prefs.saveBackgroundCount(2) }
        verify(exactly = 1) { fixture.prefs.saveAppSessionTime(2_000L) }
        verify(exactly = 1) { fixture.prefs.saveAppStoppedTimestamp(SutFixture.NOW + 2_000L) }
    }

    @Test
    fun `should persist stopped state when stopped before start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.stop()

        then()
        verify(exactly = 1) { fixture.prefs.saveBackgroundCount(2) }
        verify(exactly = 1) { fixture.prefs.saveAppStoppedTimestamp(SutFixture.NOW) }
    }

    @Test
    fun `should show timed in-app once foreground time is reached`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        val inApp = stubInAppMessage()
        val shown = mutableListOf<List<InAppMessage>>()
        fixture.sut.scheduleInAppMessages(mutableListOf(InAppWithTime(inApp, 2_000L))) { shown += it }

        whenn()
        fixture.sut.start()
        advanceTimeBy(5_000L)
        runCurrent()

        then()
        assertEquals(listOf(listOf(inApp)), shown)
    }

    @Test
    fun `should drop timed in-app that is already too late`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        every { fixture.prefs.getAppSessionTime() } returns 10_000L
        val shown = mutableListOf<List<InAppMessage>>()
        fixture.sut.scheduleInAppMessages(mutableListOf(InAppWithTime(mockk(), 2_000L))) { shown += it }

        whenn()
        fixture.sut.start()
        advanceTimeBy(3_000L)
        runCurrent()

        then()
        assertTrue(shown.isEmpty())
    }

    @Test
    fun `should ignore empty in-app schedule`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        var called = false

        whenn()
        fixture.sut.scheduleInAppMessages(mutableListOf()) { called = true }
        fixture.sut.start()
        advanceTimeBy(2_000L)
        runCurrent()

        then()
        assertEquals(false, called)
    }

    @Test
    fun `should reset counters when session is cleared`() = runUnitTest {
        given()
        val fixture = SutFixture(this, previousSessionId = "current", previousSessionStart = SutFixture.NOW)
        fixture.sut.start()
        advanceTimeBy(2_000L)
        runCurrent()

        whenn()
        fixture.sut.clearSessionForced()

        then()
        verify(exactly = 1) { fixture.prefs.batchSave(openCount = 0, backgroundCount = 0, lastInteractionTime = 0) }
        assertEquals(0L, fixture.sut.getForegroundTimeMillis())
    }

    @Test
    fun `should use shared filter updated after creation`() = runUnitTest {
        given()
        val fixture = SutFixture(this, options = LifecycleTrackingOptions.ALL, previousSessionId = "old-session")

        whenn()
        fixture.filter.update(LifecycleTrackingOptions.NONE)
        fixture.sut.setLifecycleEventConfig(LifecycleTrackingOptions.NONE)
        fixture.sut.start()
        runCurrent()

        then()
        assertTrue(fixture.events.isEmpty())
    }
}
