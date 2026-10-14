package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubInteractionDb
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

internal class PushInteractionsStepTest {

    private class SutFixture(scope: TestScope) {
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushInteractionsStep(interactionsDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            every { interactionsDb.deleteInteraction(any()) } returns true
        }
    }

    @Test
    fun `should continue without request when there are no interactions`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send interaction status with put to interaction url`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val interaction = stubInteractionDb(interactionId = "abc")
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(listOf(interaction), emptyList())

        whenn()
        fixture.sut()

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.PUT, request.method)
        assertEquals(ApiContract.RetenoApi.InteractionStatus("abc").url, request.contract.url)
        assertEquals(interaction.toRemote().toJson(), request.body)
        verify(exactly = 1) { fixture.interactionsDb.deleteInteraction(interaction) }
    }

    @Test
    fun `should send interactions one by one until none are left`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInteractionDb(rowId = "1")),
            listOf(stubInteractionDb(rowId = "2")),
            emptyList()
        )

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(2, fixture.rest.requests.size)
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should abort and keep interaction when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returns listOf(stubInteractionDb())
        fixture.rest.failWith(503)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.interactionsDb.deleteInteraction(any()) }
    }

    @Test
    fun `should drop rejected interaction and continue with next one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInteractionDb(rowId = "1")),
            listOf(stubInteractionDb(rowId = "2")),
            emptyList()
        )
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(2, fixture.rest.requests.size)
        verify(exactly = 2) { fixture.interactionsDb.deleteInteraction(any()) }
    }

    @Test
    fun `should stop when sent interaction cannot be deleted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returns listOf(stubInteractionDb())
        every { fixture.interactionsDb.deleteInteraction(any()) } returns false

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should stop after rejected interaction cannot be deleted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInteractionDb(rowId = "1")),
            listOf(stubInteractionDb(rowId = "2")),
            emptyList()
        )
        every { fixture.interactionsDb.deleteInteraction(any()) } returns false
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should access local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.interactionsDb.getInteractions(1) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
    }

    @Test
    fun `should trace steps without push token or interaction id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInteractionDb(interactionId = "secret-id")),
            emptyList()
        )

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ send interaction status to server") })
        assertTrue(lines.any { it.contains("✓ delete sent interaction from local database") })
        assertFalse(lines.any { it.contains("secret-id") || it.contains("token") })
    }
}
