package com.reteno.core.internal.coroutine

import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.MarkingDispatcher
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class RetenoScopeTest {

    private class SutFixture(testScope: TestScope, enabled: Boolean = true) {
        private val dispatchers = testScope.testDispatchers()
        val markedDefault = MarkingDispatcher(dispatchers.default)
        val sink = FakeLogSink(isEnabled = enabled)
        val sut = retenoScope(
            object : RetenoDispatchers by dispatchers {
                override val default: CoroutineDispatcher = markedDefault
            },
            sink
        )
    }

    @Test
    fun `should log uncaught failure with redacted stack trace`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.launch { throw IOException("down https://mobile-api.reteno.com/x") }
        runCurrent()

        then()
        val entry = fixture.sink.entries.single()
        assertEquals(LogPriority.ERROR, entry.priority)
        assertEquals("Reteno", entry.tag)
        assertTrue(entry.message.startsWith("Uncaught: java.io.IOException: down <url>"))
    }

    @Test
    fun `should keep running other coroutines after one fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var secondRan = false

        whenn()
        fixture.sut.launch { throw IllegalStateException("first") }
        runCurrent()
        fixture.sut.launch { secondRan = true }
        runCurrent()

        then()
        assertTrue(secondRan)
    }

    @Test
    fun `should not log uncaught failure when logging disabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this, enabled = false)

        whenn()
        fixture.sut.launch { throw IllegalStateException("quiet") }
        runCurrent()

        then()
        assertTrue(fixture.sink.entries.isEmpty())
    }

    @Test
    fun `should run work on default dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var ranOnDefault = false

        whenn()
        fixture.sut.launch { ranOnDefault = fixture.markedDefault.isRunningHere }
        runCurrent()

        then()
        assertTrue(ranOnDefault)
    }

    @Test
    fun `should dispatch launched work instead of running it in place`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var ran = false

        whenn()
        fixture.sut.launch { ran = true }

        then()
        assertFalse(ran)
    }
}
