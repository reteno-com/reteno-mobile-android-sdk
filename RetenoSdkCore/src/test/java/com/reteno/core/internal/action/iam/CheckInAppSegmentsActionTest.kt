package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRuleRetryParams
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubSegment
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CheckInAppSegmentsActionTest {

    private class SutFixture(scope: TestScope) {
        val rest = FakeRestClient()
        val inAppDb = mockk<RetenoDatabaseManagerInAppMessages>(relaxed = true)
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var savedOnIo = false
        val sut = CheckInAppSegmentsAction(
            api = rest.api(dispatchers),
            cache = InAppMessagesCache(inAppDb, dispatchers),
            clock = { NOW },
            lock = Mutex(),
            runner = traced.runner
        )

        init {
            every { inAppDb.insertInAppMessages(any()) } answers { savedOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should not ask server when no message has a segment`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage()), persist = true)

        then()
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should not ask server for message with async rules but no segment`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage(async = AsyncDisplayRules(segment = null))), persist = true)

        then()
        assertTrue(fixture.rest.requests.isEmpty())
    }

    @Test
    fun `should ask server once for each distinct segment`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(NO_CHECKS)
        val sameSegment = listOf(segmentMessage(id = 1, segmentId = 7), segmentMessage(id = 2, segmentId = 7))

        whenn()
        fixture.sut(sameSegment, persist = false)

        then()
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.InAppMessages.CheckUserInSegments, SEGMENT_7_REQUEST)),
            fixture.rest.requests
        )
    }

    @Test
    fun `should mark message in segment when server confirms it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":true}]}""")

        whenn()
        fixture.sut(listOf(message), persist = false)

        then()
        assertTrue(message.segment().isInSegment)
    }

    @Test
    fun `should mark message outside segment when server has no answer for it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7, inSegment = true)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":null}]}""")

        whenn()
        fixture.sut(listOf(message), persist = false)

        then()
        assertFalse(message.segment().isInSegment)
    }

    @Test
    fun `should remember when segment was checked`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":true}]}""")

        whenn()
        fixture.sut(listOf(message), persist = false)

        then()
        assertEquals(NOW, message.segment().lastCheckedTimestamp)
    }

    @Test
    fun `should keep retry advice from server`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7)
        fixture.rest.succeedWith(
            """{"checks":[{"segmentId":7,"error":{"status":429,"retryAfter":{"unit":"SECOND","amount":5}}}]}"""
        )

        whenn()
        fixture.sut(listOf(message), persist = false)

        then()
        assertEquals(AsyncRuleRetryParams(statusCode = 429, retryAfter = 5_000L), message.segment().retryParams)
    }

    @Test
    fun `should leave messages without a server answer untouched`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val unanswered = segmentMessage(id = 2, segmentId = 8, inSegment = true)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":false}]}""")

        whenn()
        fixture.sut(listOf(segmentMessage(id = 1, segmentId = 7), unanswered), persist = false)

        then()
        assertTrue(unanswered.segment().isInSegment)
        assertEquals(null, unanswered.segment().lastCheckedTimestamp)
    }

    @Test
    fun `should save checked messages to local database on io when asked to`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":true}]}""")

        whenn()
        fixture.sut(listOf(message), persist = true)

        then()
        verify(exactly = 1) { fixture.inAppDb.deleteInAppMessages(listOf(message.toDB())) }
        verify(exactly = 1) { fixture.inAppDb.insertInAppMessages(listOf(message.toDB())) }
        assertTrue(fixture.savedOnIo)
    }

    @Test
    fun `should not save checked messages when not asked to`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("""{"checks":[{"segmentId":7,"checkResult":true}]}""")

        whenn()
        fixture.sut(listOf(segmentMessage(segmentId = 7)), persist = false)

        then()
        verify(exactly = 0) { fixture.inAppDb.insertInAppMessages(any()) }
    }

    @Test
    fun `should not save anything when server answered for no message`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(NO_CHECKS)

        whenn()
        fixture.sut(listOf(segmentMessage(segmentId = 7)), persist = true)

        then()
        verify(exactly = 0) { fixture.inAppDb.insertInAppMessages(any()) }
    }

    @Test
    fun `should leave segments untouched when server fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7, inSegment = true)
        fixture.rest.failWith(503)

        whenn()
        val result = fixture.sut(listOf(message), persist = true)

        then()
        assertTrue(result.isSuccess)
        assertTrue(message.segment().isInSegment)
        verify(exactly = 0) { fixture.inAppDb.insertInAppMessages(any()) }
    }

    @Test
    fun `should leave segments untouched when server answer cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = segmentMessage(segmentId = 7, inSegment = true)
        fixture.rest.succeedWith("{not json")

        whenn()
        val result = fixture.sut(listOf(message), persist = true)

        then()
        assertTrue(result.isSuccess)
        assertTrue(message.segment().isInSegment)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    private fun segmentMessage(id: Long = 1, segmentId: Long, inSegment: Boolean = false) = stubInAppMessage(
        messageId = id,
        async = AsyncDisplayRules(segment = stubSegment(inSegment = inSegment, segmentId = segmentId))
    )

    private fun InAppMessage.segment() = requireNotNull(displayRules.async?.segment)

    private companion object {
        const val NOW = 5_000_000L
        const val NO_CHECKS = """{"checks":[]}"""
        const val SEGMENT_7_REQUEST = """{"checks":[{"type":"IS_IN_SEGMENT","params":{"segmentId":7}}]}"""
    }
}
