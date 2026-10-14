package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.util.Util.asZonedDateTime
import com.reteno.core.util.Util.formatToRemote
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AppBackgroundedActionTest {

    private class SutFixture(scope: TestScope) {
        val env = LifecycleActionFixture(scope)
        private val sessionHandler = mockk<RetenoSessionHandler>().also {
            every { it.getForegroundTimeMillis() } returns 42_500L
        }
        val sut = AppBackgroundedAction(sessionHandler, env.state, env.lifecycleEvents, env.traced.runner)
    }

    @Test
    fun `should log backgrounded with open time and whole seconds in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.state.enterForeground(now = OPENED_AT)

        whenn()
        fixture.sut()

        then()
        val expected = listOf(
            Parameter("applicationOpenedTime", OPENED_AT.asZonedDateTime().formatToRemote()),
            Parameter("secondsInForeground", "42")
        )
        val event = fixture.env.logged.single()
        assertEquals("ApplicationBackgrounded", event.eventTypeKey)
        assertEquals(expected, event.params)
    }

    @Test
    fun `should do nothing when app was not in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.env.logged.isEmpty())
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ app was not in foreground") })
    }

    companion object {
        private const val OPENED_AT = 1_700_000_123_000L
    }
}
