package com.reteno.core.di

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.ActionGraphFixture
import com.reteno.core.fixtures.stubAppInboxMessageDb
import com.reteno.core.fixtures.stubDeviceDb
import com.reteno.core.fixtures.stubEventsDb
import com.reteno.core.fixtures.stubInAppInteractionDb
import com.reteno.core.fixtures.stubInteractionDb
import com.reteno.core.fixtures.stubRecomEventsDb
import com.reteno.core.fixtures.stubRetenoLogEventDb
import com.reteno.core.fixtures.stubUserDb
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionGraphSyncWiringTest {

    @Test
    fun `should run every sync step in order against its own table and endpoint`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())
        every { fixture.usersDb.getUsers(any()) } returns listOf(stubUserDb())
        every { fixture.interactionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInteractionDb(interactionId = "i1")),
            emptyList()
        )
        every { fixture.inAppInteractionsDb.getInteractions(1) } returnsMany listOf(
            listOf(stubInAppInteractionDb()),
            emptyList()
        )
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(stubEventsDb()), emptyList())
        every { fixture.inboxDb.getAppInboxMessages(any()) } returns listOf(stubAppInboxMessageDb())
        every { fixture.recomEventsDb.getRecomEvents(any()) } returns listOf(stubRecomEventsDb())
        every { fixture.wrappedLinksDb.getWrappedLinks(1) } returnsMany listOf(
            listOf("https://t.example/1"),
            emptyList()
        )
        every { fixture.logEventsDb.getLogEvents(any()) } returnsMany listOf(
            listOf(stubRetenoLogEventDb()),
            emptyList()
        )
        every { fixture.interactionsDb.deleteInteraction(any()) } returns true
        every { fixture.inAppInteractionsDb.deleteInteraction(any()) } returns true
        every { fixture.logEventsDb.deleteLogEvents(any()) } returns true

        whenn()
        val result = fixture.graph.sync.dataSync.syncNow(SyncScope.ALL)

        then()
        assertEquals(SyncResult.COMPLETED, result)
        assertEquals(
            listOf(
                ApiContract.MobileApi.Device.url,
                ApiContract.MobileApi.User.url,
                ApiContract.RetenoApi.InteractionStatus("i1").url,
                ApiContract.InAppMessages.RegisterInteraction.url,
                ApiContract.MobileApi.Events.url,
                ApiContract.AppInbox.MessagesStatus.url,
                ApiContract.Recommendation.PostRecoms.url,
                "https://t.example/1",
                ApiContract.LogEvent.Events.url
            ),
            fixture.rest.requests.map { it.contract.url }
        )
        verify(exactly = 1) { fixture.configRepository.saveDeviceRegistered(true) }
        assertTrue(fixture.sink.lines.any { it.contains("✓ refresh unread messages count") })
    }

    @Test
    fun `should enqueue periodic background sync when scheduler starts`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)

        whenn()
        fixture.graph.sync.dataSync.startScheduler()
        runCurrent()
        fixture.graph.sync.dataSync.stopScheduler()

        then()
        assertEquals(listOf("schedule background sync"), fixture.platform.calls)
    }

    @Test
    fun `should clear old data and report removed rows at info level`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        every { fixture.wrappedLinksDb.deleteWrappedLinksByTime(any()) } returns 2

        whenn()
        fixture.graph.sync.dataSync.clearOldData()
        advanceTimeBy(4_000)
        runCurrent()

        then()
        verify(exactly = 1) { fixture.eventsDb.deleteEventsByTime(any()) }
        assertEquals(listOf("Removed wrapped links - 2" to LogLevel.INFO), fixture.remoteLogs)
    }

    @Test
    fun `should cancel periodic work when background sync finds no data`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        val sut = fixture.sut.backgroundSync

        whenn()
        val outcome = sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.NO_DATA, outcome)
        assertEquals(listOf("cancel background sync"), fixture.platform.calls)
    }

    @Test
    fun `should run full sync when background sync finds unsent data`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        every { fixture.databaseManager.hasDataForSync() } returns true
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())
        val sut = fixture.sut.backgroundSync

        whenn()
        val outcome = sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.SYNCED, outcome)
        assertEquals(ApiContract.MobileApi.Device.url, fixture.rest.requests.first().contract.url)
    }

    @Test
    fun `should start requested sync only after sdk initialization`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this, initialized = false)
        every { fixture.devicesDb.getDevices(any()) } returns listOf(stubDeviceDb())

        whenn()
        fixture.graph.sync.dataSync.request()
        runCurrent()
        val requestsBeforeInit = fixture.rest.requests.size
        fixture.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(0, requestsBeforeInit)
        assertEquals(ApiContract.MobileApi.Device.url, fixture.rest.requests.first().contract.url)
    }
}
