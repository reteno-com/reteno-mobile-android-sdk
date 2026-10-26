package com.reteno.core.internal.action.iam

import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.RecordingInAppDisplay
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PauseInAppMessagesActionTest {

    private class SutFixture(scope: TestScope) {
        val state = InAppMessagesStateImpl()
        val display = RecordingInAppDisplay()
        val shown = display.shown
        val traced = TestActionRunner(scope)
        val sut = PauseInAppMessagesAction(state, display, traced.runner)
        val setBehaviour = SetInAppPauseBehaviourAction(state, traced.runner)
    }

    @Test
    fun `should pause in-app messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(paused = true)

        then()
        assertTrue(fixture.state.isPaused)
    }

    @Test
    fun `should show postponed in-app message when resumed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.state.setPaused(true)
        fixture.state.postponeIfPaused(stubInAppMessage(messageId = 4))

        whenn()
        fixture.sut(paused = false)

        then()
        assertEquals(listOf(4L), fixture.shown)
    }

    @Test
    fun `should show nothing when resumed without postponed messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.state.setPaused(true)

        whenn()
        fixture.sut(paused = false)

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should apply pause behaviour to postponed messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.setBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.state.setPaused(true)
        fixture.state.postponeIfPaused(stubInAppMessage(messageId = 4))

        whenn()
        fixture.sut(paused = false)

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should trace which postponed message is shown`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.state.setPaused(true)
        fixture.state.postponeIfPaused(stubInAppMessage(messageId = 4))

        whenn()
        fixture.sut(paused = false)

        then()
        val pauseLine = fixture.traced.sink.lines.first { it.contains("✓ apply pause to in-app state") }
        assertTrue(pauseLine.contains("show postponed 4"))
    }

    @Test
    fun `should trace that display skipped the postponed message because another one is on screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.display.offer = DisplayOffer.SKIPPED
        fixture.state.setPaused(true)
        fixture.state.postponeIfPaused(stubInAppMessage(messageId = 4))

        whenn()
        fixture.sut(paused = false)

        then()
        val displayLine = fixture.traced.sink.lines.first { it.contains("✓ pass postponed in-app message to display") }
        assertTrue(displayLine.contains("skipped, another in-app message is on screen"))
    }

    @Test
    fun `should trace that nothing was postponed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(paused = true)

        then()
        assertTrue(fixture.traced.sink.lines.any { it.contains("nothing postponed to show") })
    }
}
