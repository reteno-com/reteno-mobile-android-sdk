package com.reteno.core.di

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.core.internal.action.inbox.InboxCountPoller
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorInboxWiringTest {

    private fun LocatorFixture.countRequests() =
        rest.requests.count { it.contract == ApiContract.AppInbox.MessagesCount }

    @Test
    fun `should load submitted inbox page`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.rest.succeedWith("""{"list":[],"totalPages":1}""")
        val callback = RecordingResultCallback<AppInboxMessages>()

        whenn()
        fixture.sut.submitInboxMessagesRequest(1, 10, AppInboxStatus.OPENED, callback)
        runCurrent()

        then()
        assertEquals(listOf(AppInboxMessages(emptyList(), 1)), callback.results)
    }

    @Test
    fun `should load submitted unread count`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.rest.succeedWith("""{"unreadCount":4}""")
        val callback = RecordingResultCallback<Int>()

        whenn()
        fixture.sut.submitInboxCountRequest(callback)
        runCurrent()

        then()
        assertEquals(listOf(4), callback.results)
    }

    @Test
    fun `should poll unread count for subscribers once sdk is started`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.rest.succeedWith(ApiContract.AppInbox.MessagesCount, """{"unreadCount":2}""")
        val callback = RecordingResultCallback<Int>()
        fixture.sut.subscribeInboxCount(callback)
        runCurrent()
        val requestsBeforeStart = fixture.countRequests()

        whenn()
        fixture.sut.requestStart()
        runCurrent()

        then()
        assertEquals(0, requestsBeforeStart)
        assertEquals(listOf(2), callback.results)
    }

    @Test
    fun `should stop delivering unread count to unsubscribed callback while others keep polling`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val removed = RecordingResultCallback<Int>()
        val kept = RecordingResultCallback<Int>()
        fixture.rest.succeedWith(ApiContract.AppInbox.MessagesCount, """{"unreadCount":2}""")
        fixture.sut.subscribeInboxCount(removed)
        fixture.sut.subscribeInboxCount(kept)
        fixture.sut.requestStart()
        runCurrent()
        fixture.sut.dataSync.stopScheduler()
        fixture.rest.succeedWith(ApiContract.AppInbox.MessagesCount, """{"unreadCount":5}""")

        whenn()
        fixture.sut.unsubscribeInboxCount(removed)
        advanceTimeBy(InboxCountPoller.POLL_INTERVAL_MILLIS)
        runCurrent()

        then()
        assertEquals(listOf(2), removed.results)
        assertEquals(listOf(2, 5), kept.results)
    }

    @Test
    fun `should stop polling unread count after unsubscribing all callbacks`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.sut.requestStart()
        fixture.sut.subscribeInboxCount(RecordingResultCallback())
        fixture.sut.subscribeInboxCount(RecordingResultCallback())
        runCurrent()

        whenn()
        fixture.sut.unsubscribeAllInboxCount()
        advanceTimeBy(InboxCountPoller.POLL_INTERVAL_MILLIS * 2)
        runCurrent()

        then()
        assertEquals(1, fixture.countRequests())
    }

    @Test
    fun `should save opened message and mark all messages opened`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val callback = RecordingResultCallback<Unit>()

        whenn()
        fixture.sut.submitInboxMessageOpened("m1")
        fixture.sut.submitAllInboxMessagesOpened(callback)
        runCurrent()

        then()
        verify(exactly = 1) { fixture.inboxDb.insertAppInboxMessage(match { it.id == "m1" }) }
        verify(exactly = 1) { fixture.inboxDb.deleteAllAppInboxMessages() }
        assertEquals(listOf(Unit), callback.results)
    }
}
