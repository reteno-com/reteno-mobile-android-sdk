package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.logevent.RetenoLogEventListRemote
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubRetenoLogEventDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PushLogEventsStepTest {

    private class SutFixture(scope: TestScope) {
        val logsDb = mockk<RetenoDatabaseManagerLogEvent>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushLogEventsStep(logsDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            every { logsDb.deleteLogEvents(any()) } returns true
            every { logsDb.getLogEventsCount() } returns 0L
        }
    }

    @Test
    fun `should continue without request when there are no log events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send log events in batches and delete each sent batch`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = listOf(stubRetenoLogEventDb(rowId = "1"))
        val second = listOf(stubRetenoLogEventDb(rowId = "2"))
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returnsMany listOf(
            first,
            second,
            emptyList()
        )

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf(
                SentRequest(
                    HttpMethod.POST,
                    ApiContract.LogEvent.Events,
                    RetenoLogEventListRemote(first.map { it.toRemote() }).toJson()
                ),
                SentRequest(
                    HttpMethod.POST,
                    ApiContract.LogEvent.Events,
                    RetenoLogEventListRemote(second.map { it.toRemote() }).toJson()
                )
            ),
            fixture.rest.requests
        )
        verify(exactly = 1) { fixture.logsDb.deleteLogEvents(first) }
        verify(exactly = 1) { fixture.logsDb.deleteLogEvents(second) }
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should delete oldest log events above backlog limit before sending`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val excess = listOf(stubRetenoLogEventDb(rowId = "old"))
        every { fixture.logsDb.getLogEventsCount() } returns PushLogEventsStep.MAX_BACKLOG + 1L
        every { fixture.logsDb.getLogEvents(1) } returns excess
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns emptyList()

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.logsDb.deleteLogEvents(excess) }
    }

    @Test
    fun `should delete large backlog in bounded chunks`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val chunk = PushLogEventsStep.DELETE_CHUNK
        every { fixture.logsDb.getLogEventsCount() } returns PushLogEventsStep.MAX_BACKLOG + 2L * chunk + 7L
        every { fixture.logsDb.getLogEvents(any()) } returns listOf(stubRetenoLogEventDb())
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns emptyList()

        whenn()
        fixture.sut()

        then()
        verify(exactly = 2) { fixture.logsDb.getLogEvents(chunk) }
        verify(exactly = 1) { fixture.logsDb.getLogEvents(7) }
    }

    @Test
    fun `should stop deleting backlog when a chunk cannot be deleted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val backlog = PushLogEventsStep.MAX_BACKLOG + 3L * PushLogEventsStep.DELETE_CHUNK
        every { fixture.logsDb.getLogEventsCount() } returns backlog
        every { fixture.logsDb.getLogEvents(any()) } returns listOf(stubRetenoLogEventDb())
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns emptyList()
        every { fixture.logsDb.deleteLogEvents(any()) } returns false

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.logsDb.getLogEvents(PushLogEventsStep.DELETE_CHUNK) }
    }

    @Test
    fun `should sanitize messages stored by older sdk versions before upload`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val legacy = stubRetenoLogEventDb(
            errorMessage = "setUserData(): user = [User(email=john@example.com)] at https://mobile-api.reteno.com/x"
        )
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returnsMany listOf(
            listOf(legacy),
            emptyList()
        )

        whenn()
        fixture.sut()

        then()
        val body = requireNotNull(fixture.rest.requests.single().body)
        assertTrue(body.contains("setUserData(): user"))
        assertFalse(body.contains("john@example.com") || body.contains("reteno.com"))
    }

    @Test
    fun `should keep backlog that fits the limit`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEventsCount() } returns PushLogEventsStep.MAX_BACKLOG.toLong()
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns emptyList()

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.logsDb.deleteLogEvents(any()) }
    }

    @Test
    fun `should abort and keep log events when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns listOf(stubRetenoLogEventDb())
        fixture.rest.failWith(503)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.logsDb.deleteLogEvents(any()) }
    }

    @Test
    fun `should drop rejected batch and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returnsMany listOf(
            listOf(stubRetenoLogEventDb()),
            emptyList()
        )
        fixture.rest.failWith(413)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.logsDb.deleteLogEvents(any()) }
    }

    @Test
    fun `should stop when sent batch cannot be deleted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returns listOf(stubRetenoLogEventDb())
        every { fixture.logsDb.deleteLogEvents(any()) } returns false

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should not report its own unexpected failure as a new log event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logsDb.getLogEventsCount() } throws IllegalStateException("database closed")

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should access local database on io dispatcher without tracing messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            listOf(stubRetenoLogEventDb(errorMessage = "secret failure details"))
        } andThenAnswer { emptyList() }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ send log events to server") })
        assertFalse(fixture.traced.sink.lines.any { it.contains("secret failure details") })
    }

    @Test
    fun `should upload log event without message unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val withoutMessage = stubRetenoLogEventDb(errorMessage = null)
        every { fixture.logsDb.getLogEvents(PushLogEventsStep.BATCH_SIZE) } returnsMany listOf(
            listOf(withoutMessage),
            emptyList()
        )

        whenn()
        fixture.sut()

        then()
        assertEquals(
            RetenoLogEventListRemote(listOf(withoutMessage.toRemote())).toJson(),
            fixture.rest.requests.single().body
        )
    }
}
