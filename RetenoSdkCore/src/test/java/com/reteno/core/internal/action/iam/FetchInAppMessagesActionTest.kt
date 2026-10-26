package com.reteno.core.internal.action.iam

import com.google.gson.JsonParser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.testDispatchers
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
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FetchInAppMessagesActionTest {

    private class SutFixture(scope: TestScope, cached: List<InAppMessageDb> = emptyList()) {
        val rest = FakeRestClient()
        val inAppDb = mockk<RetenoDatabaseManagerInAppMessages>(relaxed = true)
        val prefs = mockk<SharedPrefsManager>()
        val checkSegments = mockk<CheckInAppSegments>()
        val state = InAppMessagesStateImpl()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val segmentChecks = mutableListOf<Pair<List<Long>, Boolean>>()
        var cacheReadOnIo = false
        var savedOnIo = false
        val sut = FetchInAppMessagesAction(
            api = rest.api(dispatchers),
            cache = InAppMessagesCache(inAppDb, dispatchers),
            prefs = prefs,
            checkSegments = checkSegments,
            state = state,
            dispatchers = dispatchers,
            runner = traced.runner
        )

        init {
            every { inAppDb.getInAppMessages(any()) } answers {
                cacheReadOnIo = dispatchers.markedIo.isRunningHere
                cached
            }
            every { inAppDb.insertInAppMessages(any()) } answers { savedOnIo = dispatchers.markedIo.isRunningHere }
            every { prefs.getIamEtag() } returns STORED_VERSION
            justRun { prefs.saveIamEtag(any()) }
            coEvery { checkSegments(any(), any()) } answers {
                segmentChecks += firstArg<List<InAppMessage>>().map { it.messageId } to secondArg<Boolean>()
                Result.success(Unit)
            }
        }
    }

    @Test
    fun `should load cached in-app messages from local database on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.cacheReadOnIo)
    }

    @Test
    fun `should ask server for full list when nothing is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            SentRequest(HttpMethod.GET, ApiContract.InAppMessages.GetInAppMessages, null),
            fixture.rest.requests.first()
        )
    }

    @Test
    fun `should send stored list version when messages are cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)

        whenn()
        fixture.sut()

        then()
        assertEquals(mapOf("If-None-Match" to STORED_VERSION), fixture.rest.requests.first().headers)
    }

    @Test
    fun `should return in-app messages from server`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(1L, 2L), messages.map { it.messageId })
    }

    @Test
    fun `should keep show history of cached message with same id`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        val first = messages.first { it.messageId == 1L }
        assertEquals(SHOWN_AT, first.lastShowTime)
        assertEquals(2L, first.showCount)
    }

    @Test
    fun `should load contents of messages without content with retries`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)
        fixture.rest.succeedWith(CONTENTS_JSON)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            SentRequest(
                method = HttpMethod.POST,
                contract = ApiContract.InAppMessages.GetInAppMessagesContent,
                body = """{"messageInstanceIds":[11,12]}""",
                retryCount = 3
            ),
            fixture.rest.requests[1]
        )
    }

    @Test
    fun `should attach loaded content to its message`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)
        fixture.rest.succeedWith(CONTENTS_JSON)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(JsonParser.parseString("""{"text":"hi"}"""), messages.first { it.messageId == 1L }.content?.model)
        assertNull(messages.first { it.messageId == 2L }.content)
    }

    @Test
    fun `should keep messages without content when contents cannot be loaded`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)
        fixture.rest.failWith(500)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(null, null), messages.map { it.content })
    }

    @Test
    fun `should keep messages without content when contents cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)
        fixture.rest.succeedWith("{not json")

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(null, null), messages.map { it.content })
    }

    @Test
    fun `should not ask for contents when every message has one`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = listOf(stubInAppMessage(messageId = 1).toDB()))
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should check segments of server messages without saving them separately`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(listOf(1L, 2L) to false), fixture.segmentChecks)
    }

    @Test
    fun `should replace cached messages with server messages on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        fixture.sut()

        then()
        verifyOrder {
            fixture.inAppDb.deleteAllInAppMessages()
            fixture.inAppDb.insertInAppMessages(match { rows -> rows.map { it.messageId } == listOf(1L, 2L) })
        }
        assertTrue(fixture.savedOnIo)
    }

    @Test
    fun `should save list version sent by server`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON, headers = mapOf("ETag" to listOf("v2")))

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.prefs.saveIamEtag("v2") }
    }

    @Test
    fun `should use cached messages when server reports no changes`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(304)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(1L, 3L), messages.map { it.messageId })
    }

    @Test
    fun `should check segments of cached messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(listOf(1L, 3L) to false), fixture.segmentChecks)
    }

    @Test
    fun `should save refreshed cached messages so loaded contents are kept`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = listOf(stubInAppMessage(messageId = 3, content = null).toDB()))
        fixture.rest.failWith(304)
        fixture.rest.succeedWith(CONTENT_FOR_3_JSON)

        whenn()
        fixture.sut()

        then()
        val saved = mutableListOf<List<InAppMessageDb>>()
        verify { fixture.inAppDb.insertInAppMessages(capture(saved)) }
        assertEquals("""{"text":"hi"}""", saved.single().single().model)
    }

    @Test
    fun `should keep list version when using cached messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.prefs.saveIamEtag(any()) }
    }

    @Test
    fun `should load contents of cached messages without content`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = listOf(stubInAppMessage(messageId = 3, content = null).toDB()))
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        assertEquals(ApiContract.InAppMessages.GetInAppMessagesContent, fixture.rest.requests[1].contract)
    }

    @Test
    fun `should use cached messages when server fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(503)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(1L, 3L), messages.map { it.messageId })
    }

    @Test
    fun `should use cached messages when device is offline`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failOffline()

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(1L, 3L), messages.map { it.messageId })
    }

    @Test
    fun `should use cached messages when server list cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.succeedWith("{not json")

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(listOf(1L, 3L), messages.map { it.messageId })
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should return no messages when server fails and nothing is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(503)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `should keep fetched messages as current messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(LIST_JSON)

        whenn()
        val messages = fixture.sut().getOrThrow()

        then()
        assertEquals(messages, fixture.state.messages())
    }

    @Test
    fun `should note when server reports no changes`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ in-app messages unchanged on server") })
    }

    @Test
    fun `should trace unchanged in-app list as a successful server answer`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(304)

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ load in-app messages from server") && it.contains("304 not modified") })
        assertTrue(lines.none { it.contains("✗") })
    }

    @Test
    fun `should note when server list is unavailable`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.failWith(503)

        whenn()
        fixture.sut()

        then()
        val note = "→ in-app messages unavailable on server, using cached ones"
        assertTrue(fixture.traced.sink.lines.any { it.contains(note) })
    }

    @Test
    fun `should not trace list version or server addresses`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = CACHED)
        fixture.rest.succeedWith(LIST_JSON, headers = mapOf("ETag" to listOf("v2")))

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.none { it.contains(STORED_VERSION) || it.contains("v2") || it.contains("http") })
    }

    private companion object {
        const val STORED_VERSION = "stored-version"
        const val SHOWN_AT = 1_700L
        const val LIST_JSON = """{"messages":[""" +
            """{"messageId":1,"messageInstanceId":11,"displayRules":{}},""" +
            """{"messageId":2,"messageInstanceId":12,"displayRules":{}}]}"""
        const val CONTENT_FOR_3_JSON =
            """{"contents":[{"messageInstanceId":3,"layoutType":"FULL","model":{"text":"hi"}}]}"""
        const val CONTENTS_JSON =
            """{"contents":[{"messageInstanceId":11,"layoutType":"FULL","model":{"text":"hi"}}]}"""
        val CACHED = listOf(
            stubInAppMessage(messageId = 1, lastShowTime = SHOWN_AT, showCount = 2).toDB(),
            stubInAppMessage(messageId = 3).toDB()
        )
    }
}
