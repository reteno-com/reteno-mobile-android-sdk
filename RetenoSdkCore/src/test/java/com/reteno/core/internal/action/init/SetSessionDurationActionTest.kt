package com.reteno.core.internal.action.init

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SetSessionDurationActionTest {

    @Test
    fun `should apply session duration to session tracking`() = runUnitTest {
        given()
        val applied = mutableListOf<Long>()
        val sessionHandler = mockk<RetenoSessionHandler>().also {
            every { it.setSessionResetDuration(any()) } answers { applied += firstArg<Long>() }
        }
        val traced = TestActionRunner(this)
        val sut = SetSessionDurationAction(sessionHandler, traced.runner)

        whenn()
        sut(60_000L)

        then()
        assertEquals(listOf(60_000L), applied)
        assertTrue(traced.sink.lines.first().contains("SetSessionDuration  durationMillis=60000"))
        assertTrue(traced.sink.lines.any { it.contains("✓ apply session duration to session tracking") })
    }
}
