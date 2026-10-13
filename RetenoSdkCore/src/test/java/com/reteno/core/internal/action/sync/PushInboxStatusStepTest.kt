package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.appinbox.AppInboxMessagesStatusRemote
import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubAppInboxMessageDb
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

internal class PushInboxStatusStepTest {

    private class SutFixture(scope: TestScope) {
        val inboxDb = mockk<RetenoDatabaseManagerAppInbox>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var countRefreshes = 0
        private val inboxRepository = mockk<AppInboxRepository>().also { every { it.refreshMessagesCount() } answers { countRefreshes++ } }
        val sut = PushInboxStatusStep(inboxDb, rest.api(dispatchers), inboxRepository, dispatchers, traced.runner)

        init {
            justRun { inboxDb.deleteAppInboxMessages(any()) }
        }
    }

    @Test
    fun `should continue without request when no message was opened`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inboxDb.getAppInboxMessages(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertEquals(0, fixture.countRefreshes)
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send opened message ids then delete them and refresh unread count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val messages = listOf(stubAppInboxMessageDb("m1"), stubAppInboxMessageDb("m2"))
        every { fixture.inboxDb.getAppInboxMessages(any()) } returns messages

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.AppInbox.MessagesStatus, AppInboxMessagesStatusRemote(ids = listOf("m1", "m2")).toJson())),
            fixture.rest.requests
        )
        verify(exactly = 1) { fixture.inboxDb.deleteAppInboxMessages(messages) }
        assertEquals(1, fixture.countRefreshes)
    }

    @Test
    fun `should abort and keep statuses when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inboxDb.getAppInboxMessages(any()) } returns listOf(stubAppInboxMessageDb())
        fixture.rest.failWith(502)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.inboxDb.deleteAppInboxMessages(any()) }
    }

    @Test
    fun `should drop rejected statuses without refreshing count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inboxDb.getAppInboxMessages(any()) } returns listOf(stubAppInboxMessageDb())
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.inboxDb.deleteAppInboxMessages(any()) }
        assertEquals(0, fixture.countRefreshes)
    }

    @Test
    fun `should access local database on io dispatcher and trace status count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.inboxDb.getAppInboxMessages(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            listOf(stubAppInboxMessageDb("secret-message"))
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ load opened inbox messages from local database") && it.contains("count=1") })
        assertFalse(fixture.traced.sink.lines.any { it.contains("secret-message") })
    }
}
