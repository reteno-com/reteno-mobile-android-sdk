package com.reteno.core.internal.iam

import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubInAppWithEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InAppMessagesStateTest {

    private class SutFixture {
        val sut = InAppMessagesStateImpl()
        val first = stubInAppMessage(messageId = 1)
        val second = stubInAppMessage(messageId = 2)

        fun pauseAndPostponeBoth() {
            sut.setPaused(true)
            sut.postponeIfPaused(first)
            sut.postponeIfPaused(second)
        }
    }

    @Test
    fun `should start not paused`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val paused = fixture.sut.isPaused

        then()
        assertFalse(paused)
    }

    @Test
    fun `should not postpone message while not paused`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val postponed = fixture.sut.postponeIfPaused(fixture.first)

        then()
        assertFalse(postponed)
    }

    @Test
    fun `should not keep message offered while not paused for a later resume`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.postponeIfPaused(fixture.first)
        fixture.sut.setPaused(true)

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertNull(toShow)
    }

    @Test
    fun `should postpone message while paused`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.setPaused(true)

        whenn()
        val postponed = fixture.sut.postponeIfPaused(fixture.first)

        then()
        assertTrue(postponed)
    }

    @Test
    fun `should hand back first postponed message when resumed`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.pauseAndPostponeBoth()

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertEquals(fixture.first, toShow)
    }

    @Test
    fun `should forget postponed messages after handing one back`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.pauseAndPostponeBoth()
        fixture.sut.setPaused(false)
        fixture.sut.setPaused(true)

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertNull(toShow)
    }

    @Test
    fun `should drop postponed messages when resumed with skip behaviour`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.pauseAndPostponeBoth()

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertNull(toShow)
    }

    @Test
    fun `should forget skipped messages for later resumes`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.pauseAndPostponeBoth()
        fixture.sut.setPaused(false)
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.POSTPONE_IN_APPS)
        fixture.sut.setPaused(true)

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertNull(toShow)
    }

    @Test
    fun `should keep postponed messages while pause is set again`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.pauseAndPostponeBoth()
        fixture.sut.setPaused(true)

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertEquals(fixture.first, toShow)
    }

    @Test
    fun `should hand back nothing when resumed without pause`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val toShow = fixture.sut.setPaused(false)

        then()
        assertNull(toShow)
    }

    @Test
    fun `should report paused after pausing`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.setPaused(true)

        then()
        assertTrue(fixture.sut.isPaused)
    }

    @Test
    fun `should start without messages waiting for events`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val waiting = fixture.sut.waitingForEvent()

        then()
        assertTrue(waiting.isEmpty())
    }

    @Test
    fun `should replace messages waiting for events`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val replacement = listOf(stubInAppWithEvent(messageId = 3))
        fixture.sut.replaceWaitingForEvent(listOf(stubInAppWithEvent(messageId = 1), stubInAppWithEvent(messageId = 2)))

        whenn()
        fixture.sut.replaceWaitingForEvent(replacement)

        then()
        assertEquals(replacement, fixture.sut.waitingForEvent())
    }

    @Test
    fun `should clear messages waiting for events when replaced with none`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.replaceWaitingForEvent(listOf(stubInAppWithEvent()))

        whenn()
        fixture.sut.replaceWaitingForEvent(emptyList())

        then()
        assertTrue(fixture.sut.waitingForEvent().isEmpty())
    }
}
