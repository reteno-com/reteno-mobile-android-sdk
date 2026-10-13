package com.reteno.core.internal.action.lifecycle

import com.reteno.core.lifecycle.RetenoSessionHandler
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
        private val sessionHandler = mockk<RetenoSessionHandler>().also { every { it.getForegroundTimeMillis() } returns 42_500L }
        val sut = AppBackgroundedAction(sessionHandler, env.state, env.lifecycleEvents, env.traced.runner)
    }

    @Test
    fun `should log backgrounded with open time and whole seconds in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.state.enterForeground(now = 7L)

        whenn()
        fixture.sut()

        then()
        val event = fixture.env.logged.single()
        assertEquals("ApplicationBackgrounded", event.eventTypeKey)
        assertTrue(event.params.orEmpty().any { it.name == "secondsInForeground" && it.value == "42" })
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
}
