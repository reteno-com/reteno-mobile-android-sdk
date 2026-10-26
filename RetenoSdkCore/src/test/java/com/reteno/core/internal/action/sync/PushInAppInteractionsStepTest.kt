package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubInAppInteractionDb
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

internal class PushInAppInteractionsStepTest {

    private class SutFixture(scope: TestScope) {
        val inAppDb = mockk<RetenoDatabaseManagerInAppInteraction>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushInAppInteractionsStep(inAppDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            every { inAppDb.deleteInteraction(any()) } returns true
        }
    }

    @Test
    fun `should report nothing to send without request when there are no in app interactions`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inAppDb.getInteractions(1) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send every in app interaction and delete it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = stubInAppInteractionDb(rowId = "1")
        val second = stubInAppInteractionDb(rowId = "2", status = "CLOSE")
        every { fixture.inAppDb.getInteractions(1) } returnsMany listOf(listOf(first), listOf(second), emptyList())

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf(
                SentRequest(HttpMethod.POST, ApiContract.InAppMessages.RegisterInteraction, first.toRemote().toJson()),
                SentRequest(HttpMethod.POST, ApiContract.InAppMessages.RegisterInteraction, second.toRemote().toJson())
            ),
            fixture.rest.requests
        )
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(first) }
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(second) }
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should abort and keep in app interaction when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inAppDb.getInteractions(1) } returns listOf(stubInAppInteractionDb())
        fixture.rest.failOffline()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.inAppDb.deleteInteraction(any()) }
    }

    @Test
    fun `should drop rejected in app interaction and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inAppDb.getInteractions(1) } returnsMany listOf(listOf(stubInAppInteractionDb()), emptyList())
        fixture.rest.failWith(404)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(any()) }
    }

    @Test
    fun `should stop when sent in app interaction cannot be deleted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inAppDb.getInteractions(1) } returns listOf(stubInAppInteractionDb())
        every { fixture.inAppDb.deleteInteraction(any()) } returns false

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should access local database on io dispatcher and trace steps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.inAppDb.getInteractions(1) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ load in-app interaction from local database") })
        assertFalse(fixture.traced.sink.lines.any { it.contains("in-app-1") })
    }
}
