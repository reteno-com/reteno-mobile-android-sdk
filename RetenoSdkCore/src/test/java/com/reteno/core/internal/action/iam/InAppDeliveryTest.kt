package com.reteno.core.internal.action.iam

import com.google.gson.JsonObject
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubDeliveryMessage
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.fixtures.deliveryState
import com.reteno.core.internal.action.sync.PushInAppInteractionsStep
import com.reteno.core.internal.action.sync.StepOutcome
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime

internal class InAppDeliveryTest {

    private class SutFixture(scope: TestScope) {
        val now = System.currentTimeMillis()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val api = rest.api(dispatchers)
        val rows = mockk<RetenoDatabaseManagerInAppMessages>(relaxed = true)
        val observations = mockk<RetenoDatabaseManagerInAppInteraction>(relaxed = true)
        val prefs = mockk<SharedPrefsManager>(relaxed = true)
        val files = mockk<FileManager>()
        val users = mockk<RetenoDatabaseManagerUser>()
        val segments = mockk<CheckInAppSegments>()
        val traced = TestActionRunner(scope)
        val state = InAppMessagesStateImpl()
        val delivery = deliveryState(dispatchers, rest, observations) { now }
        val cache = InAppMessagesCache(rows, dispatchers, delivery)
        val fetch = FetchInAppMessagesAction(api, cache, prefs, segments, state, dispatchers, traced.runner, delivery)
        val load = LoadInAppHtmlAction(files, users, dispatchers, { "html-1" }, traced.runner, api, delivery)
        val push = PushInAppInteractionsStep(observations, api, dispatchers, traced.runner)

        init {
            every { observations.isDeliveryShown(any(), any()) } returns false
            every { rows.getInAppMessages(any()) } returns emptyList()
            every { observations.deleteInteraction(any()) } returns true
            justRun { observations.recordAttempt(any()) }
            coEvery { segments(any(), any()) } returns Result.success(Unit)
            coEvery { files.getBaseHtmlContent() } returns "<html>\${documentModel}|\${personalisation}</html>"
        }

        fun message(iid: String = FIRST_IID) = stubDeliveryMessage(iid, rest.context, now)

        fun discovery(): String = DISCOVERY.replace("{{expiry}}", Instant.ofEpochMilli(now).plusSeconds(3600).toString())
    }

    @Test
    fun `should preserve mixed discovery and authorize each execution by its original iid`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(fixture.discovery())
        fixture.rest.succeedWith("""{"contents":[]}""")

        whenn()
        val messages = fixture.fetch().getOrThrow()
        fixture.rest.succeedWith(CONTENTS)
        val first = fixture.load(messages.first { it.iid == FIRST_IID }).getOrThrow()
        fixture.rest.succeedWith(CONTENTS)
        val second = fixture.load(messages.first { it.iid == SECOND_IID }).getOrThrow()

        then()
        assertEquals(3, messages.size)
        assertEquals(2, messages.count { it.serverAssignedIid })
        assertNull(messages.first { !it.serverAssignedIid }.iid)
        assertNull(fixture.rest.requests.first().queryParams?.get("includeWorkflow"))
        val bodies = fixture.rest.requests.drop(1).map { requireNotNull(it.body).fromJson<JsonObject>() }
        assertEquals(setOf("messageInstanceIds"), bodies.first().keySet())
        assertEquals(FIRST_IID, bodies[1].getAsJsonArray("messages")[0].asJsonObject.get("iid").asString)
        assertEquals(SECOND_IID, bodies[2].getAsJsonArray("messages")[0].asJsonObject.get("iid").asString)
        assertTrue(first.fullHtml.contains("order-1"))
        assertTrue(second.fullHtml.contains("order-2"))
        verify(exactly = 0) { fixture.users.getUsers(any()) }
    }

    @Test
    fun `should retain backend priority across a not modified response without changing stored columns`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val expiry = Instant.ofEpochMilli(fixture.now).plusSeconds(3600).toString()
        fixture.rest.succeedWith("""{"messages":[{
            "messageId":7,"messageInstanceId":8001,"priority":20,"iid":"$FIRST_IID",
            "expiresAt":"$expiry","displayRules":{}}]}""")
        val first = fixture.fetch().getOrThrow()
        every { fixture.rows.getInAppMessages(any()) } returns first.map { it.toDB() }
        every { fixture.prefs.getIamEtag() } returns "delivery-version"
        fixture.rest.failWith(304)

        whenn()
        val restored = fixture.fetch().getOrThrow()

        then()
        assertEquals(20, restored.single().priority)
        assertEquals(FIRST_IID, restored.single().iid)
        assertEquals(mapOf("If-None-Match" to "delivery-version"), fixture.rest.requests.last().headers)
    }

    @Test
    fun `should defer expired delivery content without requesting it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val expired = fixture.message().copy(expiresAt = Instant.ofEpochMilli(fixture.now).minusSeconds(1).toString())

        whenn()
        val result = fixture.load(expired)

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should defer old account content without requesting it under the new account`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val old = fixture.message()
        fixture.rest.context = "another-account"

        whenn()
        val result = fixture.load(old)

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should suppress a delivery execution restored from a persisted opened marker`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = fixture.message()
        fixture.delivery.remember(listOf(message))
        every { fixture.observations.isDeliveryShown(any(), FIRST_IID) } returns true

        whenn()
        val result = fixture.load(message)

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should invalidate content when current non frequency rules change`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val evaluated = fixture.message()
        val changed = evaluated.copy(displayRulesJson = JsonObject().apply { addProperty("version", "new") })
        fixture.delivery.remember(listOf(changed))

        whenn()
        val result = fixture.load(evaluated)

        then()
        assertTrue(result.isFailure)
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should keep independent executions after the first is shown`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = fixture.message()
        val second = fixture.message(SECOND_IID)
        fixture.delivery.remember(listOf(first, second))

        whenn()
        fixture.delivery.markShown(first)

        then()
        assertFalse(fixture.delivery.isEligible(first))
        assertTrue(fixture.delivery.isEligible(second))
    }

    @Test
    fun `should keep delivery content unavailable when authorization fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = fixture.message()
        fixture.delivery.remember(listOf(message))
        fixture.rest.failWith(404)

        whenn()
        val result = fixture.load(message)

        then()
        assertTrue(result.isFailure)
        assertEquals(1, fixture.rest.requests.size)
        assertFalse(fixture.delivery.isEligible(message))
        val retried = fixture.load(message)
        assertTrue(retried.isFailure)
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should retry the same clicked observation with its original time iid and complete action`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val interaction = InAppInteraction(
            interactionId = FIRST_IID,
            time = ZonedDateTime.now().withNano(0),
            messageInstanceId = 8001,
            status = InAppInteractionStatus.CLICKED,
            deliveryContext = fixture.rest.context,
            deliveryExpiresAt = fixture.message().expiresAt,
            action = InteractionAction("OPEN_URL", "order-details", "https://example.com/orders/123")
        ).toDb()
        every { fixture.observations.getPendingInteractions(any()) } returnsMany listOf(
            listOf(interaction), listOf(interaction.copy(attempts = 1)), emptyList()
        )
        fixture.rest.failWith(503)

        whenn()
        val failed = fixture.push().getOrThrow()
        fixture.rest.succeed()
        fixture.push().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, failed)
        assertEquals(2, fixture.rest.requests.size)
        assertEquals(fixture.rest.requests[0].body, fixture.rest.requests[1].body)
        val payload = requireNotNull(fixture.rest.requests[1].body).fromJson<JsonObject>()
        assertEquals(FIRST_IID, payload.get("iid").asString)
        assertEquals(interaction.time, payload.get("time").asString)
        assertTrue(payload.get("serverAssignedIid").asBoolean)
        assertEquals("order-details", payload.getAsJsonObject("action").get("targetComponentId").asString)
        assertEquals("https://example.com/orders/123", payload.getAsJsonObject("action").get("url").asString)
        verify(exactly = 2) { fixture.observations.recordAttempt(any()) }
    }

    @Test
    fun `should retain shown marker without another request after retry budget is exhausted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val observation = InAppInteraction.createOpened(FIRST_IID, 8001)
            .copy(deliveryContext = fixture.rest.context, deliveryExpiresAt = fixture.message().expiresAt)
            .toDb().copy(attempts = 3)
        every { fixture.observations.getPendingInteractions(any()) } returnsMany listOf(listOf(observation), emptyList())

        whenn()
        fixture.push().getOrThrow()

        then()
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.observations.deleteInteraction(observation) }
    }

    @Test
    fun `should preserve the original delivery expiry across repeated discovery`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val original = fixture.message().copy(expiresAt = Instant.ofEpochMilli(fixture.now).plusSeconds(60).toString())
        every { fixture.rows.getInAppMessages(any()) } returns listOf(original.toDB())
        fixture.rest.succeedWith(fixture.discovery())
        fixture.rest.succeedWith("""{"contents":[]}""")

        whenn()
        val messages = fixture.fetch().getOrThrow()

        then()
        assertEquals(original.expiresAt, messages.first { it.iid == FIRST_IID }.expiresAt)
    }

    @Test
    fun `should defer cold start cached deliveries after failed discovery while keeping ordinary offline messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val delivery = fixture.message()
        val ordinary = stubInAppMessage(messageId = 99)
        val cached = listOf(delivery.toDB(), ordinary.toDB())
        every { fixture.rows.getInAppMessages(any()) } returns cached
        fixture.rest.failWith(503)

        whenn()
        val messages = fixture.fetch().getOrThrow()

        then()
        assertEquals(listOf(99L), messages.map { it.messageId })
        assertEquals(listOf(99L), fixture.state.messages().map { it.messageId })
        assertFalse(fixture.delivery.isEligible(delivery))
        verify { fixture.rows.insertInAppMessages(match { rows -> rows.any { it.iid == FIRST_IID } }) }
    }

    @Test
    fun `should restore server priority and preserve earlier cached expiry after malformed cold start discovery`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val earlier = fixture.message().copy(expiresAt = Instant.ofEpochMilli(fixture.now).plusSeconds(60).toString())
        every { fixture.rows.getInAppMessages(any()) } returns listOf(earlier.toDB())
        fixture.rest.succeedWith("malformed")
        val deferred = fixture.fetch().getOrThrow()
        fixture.rest.succeedWith(fixture.discovery())
        fixture.rest.succeedWith("""{"contents":[]}""")

        whenn()
        val recovered = fixture.fetch().getOrThrow().first { it.iid == FIRST_IID }

        then()
        assertTrue(deferred.isEmpty())
        assertEquals(20, recovered.priority)
        assertEquals(earlier.expiresAt, recovered.expiresAt)
        assertTrue(fixture.delivery.isEligible(recovered))
        assertTrue(fixture.rest.requests.take(2).all { it.headers == null })
    }

    @Test
    fun `should reject delivery removed by the current successful discovery`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val old = fixture.message()
        fixture.delivery.remember(listOf(old))
        fixture.rest.succeedWith("""{"messages":[]}""")

        whenn()
        fixture.fetch().getOrThrow()

        then()
        assertFalse(fixture.delivery.isEligible(old))
    }

    private companion object {
        const val FIRST_IID = "11111111-1111-4111-8111-111111111111"
        const val SECOND_IID = "22222222-2222-4222-8222-222222222222"
        const val DISCOVERY = """{
            "messages":[
                {"messageId":7,"messageInstanceId":8,"displayRules":{}},
                {"messageId":7,"messageInstanceId":8001,"priority":20,
                 "iid":"11111111-1111-4111-8111-111111111111","expiresAt":"{{expiry}}","displayRules":{}},
                {"messageId":7,"messageInstanceId":8001,"priority":20,
                 "iid":"22222222-2222-4222-8222-222222222222","expiresAt":"{{expiry}}","displayRules":{}}
            ]
        }"""
        const val CONTENTS = """{
            "contents":[
                {"iid":"22222222-2222-4222-8222-222222222222","messageInstanceId":8001,
                 "layoutType":"FULL","model":{"text":"order"},"personalisation":{"order":"order-2"}},
                {"iid":"11111111-1111-4111-8111-111111111111","messageInstanceId":8001,
                 "layoutType":"FULL","model":{"text":"order"},"personalisation":{"order":"order-1"}}
            ]
        }"""
    }
}
