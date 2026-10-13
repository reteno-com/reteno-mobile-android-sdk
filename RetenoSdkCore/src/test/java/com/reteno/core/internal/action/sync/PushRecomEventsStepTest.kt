package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.stubRecomEventsDb
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
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

internal class PushRecomEventsStepTest {

    private class SutFixture(scope: TestScope) {
        val recomDb = mockk<RetenoDatabaseManagerRecomEvents>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushRecomEventsStep(recomDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            justRun { recomDb.deleteRecomEvents(any()) }
        }
    }

    @Test
    fun `should continue without request when there are no recommendation events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.recomDb.getRecomEvents(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should delete groups without events instead of sending them`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val empty = listOf(stubRecomEventsDb(recomEvents = null), stubRecomEventsDb(recomVariantId = "v2", recomEvents = emptyList()))
        every { fixture.recomDb.getRecomEvents(any()) } returns empty

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.recomDb.deleteRecomEvents(empty) }
    }

    @Test
    fun `should send all recommendation events and delete them`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val events = listOf(stubRecomEventsDb())
        every { fixture.recomDb.getRecomEvents(any()) } returns events

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(listOf(SentRequest(HttpMethod.POST, ApiContract.Recommendation.PostRecoms, events.toRemote().toJson())), fixture.rest.requests)
        verify(exactly = 1) { fixture.recomDb.deleteRecomEvents(events) }
    }

    @Test
    fun `should abort and keep events when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.recomDb.getRecomEvents(any()) } returns listOf(stubRecomEventsDb())
        fixture.rest.failOffline()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.recomDb.deleteRecomEvents(any()) }
    }

    @Test
    fun `should drop rejected events and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.recomDb.getRecomEvents(any()) } returns listOf(stubRecomEventsDb())
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.recomDb.deleteRecomEvents(any()) }
    }

    @Test
    fun `should access local database on io dispatcher and trace steps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.recomDb.getRecomEvents(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ load recommendation events from local database") })
    }
}
