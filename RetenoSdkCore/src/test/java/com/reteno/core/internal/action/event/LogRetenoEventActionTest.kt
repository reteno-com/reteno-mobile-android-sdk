package com.reteno.core.internal.action.event

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class LogRetenoEventActionTest {

    private class SutFixture(scope: TestScope) {
        val logEventsDb = mockk<RetenoDatabaseManagerLogEvent>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = LogRetenoEventAction(logEventsDb, dispatchers, traced.runner)
    }

    @Test
    fun `should insert log event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubRetenoLogEvent()
        justRun { fixture.logEventsDb.insertLogEvent(any()) }

        whenn()
        val result = fixture.sut(event)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.logEventsDb.insertLogEvent(event.toDb()) }
    }

    @Test
    fun `should insert log event on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var insertedOnIo = false
        every { fixture.logEventsDb.insertLogEvent(any()) } answers {
            insertedOnIo = fixture.dispatchers.markedIo.isRunningHere
        }

        whenn()
        fixture.sut(stubRetenoLogEvent())

        then()
        assertTrue(insertedOnIo)
    }

    @Test
    fun `should not report its own failure so that failing inserts cannot recurse`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logEventsDb.insertLogEvent(any()) } throws IOException("database locked")

        whenn()
        val result = fixture.sut(stubRetenoLogEvent())

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(fixture.traced.reports.isEmpty())
        assertEquals(LogPriority.ERROR, fixture.traced.sink.entries.last().priority)
    }

    @Test
    fun `should trace log level without message content`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.logEventsDb.insertLogEvent(any()) }

        whenn()
        fixture.sut(stubRetenoLogEvent(logLevel = LogLevel.ERROR, errorMessage = "secret details"))

        then()
        assertEquals(
            fixture.traced.successfulTrace("LogRetenoEvent", "level=ERROR", "save log event to local database"),
            fixture.traced.sink.lines
        )
        assertFalse(fixture.traced.sink.lines.any { it.contains("secret details") })
    }
}
