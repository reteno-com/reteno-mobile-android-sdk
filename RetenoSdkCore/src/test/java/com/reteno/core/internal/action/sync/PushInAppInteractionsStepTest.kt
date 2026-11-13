package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toRemote
import com.google.gson.JsonObject
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
        every { fixture.inAppDb.getPendingInteractions(any()) } returns emptyList()

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
        every { fixture.inAppDb.getPendingInteractions(any()) } returnsMany listOf(listOf(first), listOf(second), emptyList())

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
        every { fixture.inAppDb.getPendingInteractions(any()) } returns listOf(stubInAppInteractionDb())
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
        every { fixture.inAppDb.getPendingInteractions(any()) } returnsMany listOf(listOf(stubInAppInteractionDb()), emptyList())
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
        every { fixture.inAppDb.getPendingInteractions(any()) } returns listOf(stubInAppInteractionDb())
        every { fixture.inAppDb.deleteInteraction(any()) } returns false

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should keep ordinary click queued in its original account and retry its unchanged client iid payload`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val owner = fixture.rest.context
        val click = stubInAppInteractionDb().copy(
            status = "CLICKED",
            deliveryContext = owner,
            attempts = 3,
            action = """{"type":"OPEN_URL","targetComponentId":"cta","url":"https://example.com"}"""
        )
        var pending = true
        every { fixture.inAppDb.getPendingInteractions(any()) } answers {
            if (firstArg<String>() == owner && pending) listOf(click) else emptyList()
        }
        every { fixture.inAppDb.deleteInteraction(click) } answers { pending = false; true }
        fixture.rest.failOffline()
        fixture.sut().getOrThrow()
        val firstBody = fixture.rest.requests.single().body
        fixture.rest.context = "account-b"
        fixture.sut().getOrThrow()
        val requestsInB = fixture.rest.requests.size
        fixture.rest.context = owner

        whenn()
        fixture.sut().getOrThrow()

        then()
        assertEquals(1, requestsInB)
        assertEquals(2, fixture.rest.requests.size)
        assertEquals(firstBody, fixture.rest.requests.last().body)
        assertTrue(fixture.rest.requests.all { it.method == HttpMethod.PUT })
        assertTrue(fixture.rest.requests.all {
            it.contract.url == ApiContract.RetenoApi.InteractionStatus(click.interactionId).url
        })
        val payload = requireNotNull(firstBody).fromJson<JsonObject>()
        assertEquals("CLICKED", payload.get("status").asString)
        assertEquals(click.time, payload.get("time").asString)
        assertEquals(click.action?.fromJson<JsonObject>(), payload.getAsJsonObject("action"))
        assertFalse(payload.has("iid"))
        assertFalse(payload.has("messageInstanceId"))
        assertFalse(requireNotNull(firstBody).contains("\"serverAssignedIid\":true"))
        verify(exactly = 0) { fixture.inAppDb.recordAttempt(any()) }
        assertFalse(pending)
    }

    @Test
    fun `should reject a server click when its account changes after loading and replay for its owner`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val owner = fixture.rest.context
        val click = stubInAppInteractionDb().copy(
            status = "CLICKED",
            deliveryContext = owner,
            deliveryExpiresAt = java.time.Instant.now().plusSeconds(3600).toString(),
            action = """{"type":"OPEN_URL","targetComponentId":"cta","url":"https://example.com"}"""
        )
        var pending = true
        var switchAccount = true
        every { fixture.inAppDb.getPendingInteractions(any()) } answers {
            if (firstArg<String>() != owner || !pending) {
                emptyList()
            } else {
                if (switchAccount) fixture.rest.context = "account-b"
                listOf(click)
            }
        }
        every { fixture.inAppDb.recordAttempt(any()) } returns Unit
        every { fixture.inAppDb.deleteInteraction(click) } answers { pending = false; true }

        whenn()
        val rejected = fixture.sut().getOrThrow()
        val sentUnderWrongAccount = fixture.rest.requests.size
        val retainedAfterRejection = pending
        switchAccount = false
        fixture.rest.context = owner
        fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, rejected)
        assertEquals(0, sentUnderWrongAccount)
        assertTrue(retainedAfterRejection)
        assertEquals(HttpMethod.PUT, fixture.rest.requests.single().method)
        assertFalse(pending)
    }

    @Test
    fun `should keep server visibility and failure on the observation post`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val opened = stubInAppInteractionDb().copy(
            deliveryContext = fixture.rest.context,
            deliveryExpiresAt = java.time.Instant.now().plusSeconds(3600).toString()
        )
        val failed = opened.copy(rowId = "2", status = "FAILED", statusDescription = "Renderer failed")
        every { fixture.inAppDb.getPendingInteractions(any()) } returnsMany listOf(
            listOf(opened), listOf(failed), emptyList()
        )
        every { fixture.inAppDb.recordAttempt(any()) } returns Unit

        whenn()
        fixture.sut().getOrThrow()

        then()
        assertEquals(
            listOf(
                SentRequest(HttpMethod.POST, ApiContract.InAppMessages.RegisterInteraction, opened.toRemote().toJson()),
                SentRequest(HttpMethod.POST, ApiContract.InAppMessages.RegisterInteraction, failed.toRemote().toJson())
            ),
            fixture.rest.requests
        )
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(opened) }
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(failed) }
    }

    @Test
    fun `should send legacy unowned standalone click through the existing interaction endpoint`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val click = stubInAppInteractionDb().copy(status = "CLICKED")
        every { fixture.inAppDb.getPendingInteractions(any()) } returnsMany listOf(listOf(click), emptyList())

        whenn()
        fixture.sut().getOrThrow()

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.PUT, request.method)
        assertEquals(ApiContract.RetenoApi.InteractionStatus(click.interactionId).url, request.contract.url)
        assertEquals(click.time, requireNotNull(request.body).fromJson<JsonObject>().get("time").asString)
        verify(exactly = 1) { fixture.inAppDb.deleteInteraction(click) }
    }

    @Test
    fun `should access local database on io dispatcher and trace steps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.inAppDb.getPendingInteractions(any()) } answers {
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
