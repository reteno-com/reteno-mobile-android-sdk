package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.STUB_TIME
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubEventDb
import com.reteno.core.fixtures.stubInAppInteractionDb
import com.reteno.core.fixtures.stubInteractionDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ClearOldDataActionTest {

    private class SutFixture(scope: TestScope, debug: Boolean = false) {
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>()
        val inAppInteractionsDb = mockk<RetenoDatabaseManagerInAppInteraction>()
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val inboxDb = mockk<RetenoDatabaseManagerAppInbox>()
        val recomEventsDb = mockk<RetenoDatabaseManagerRecomEvents>()
        val wrappedLinksDb = mockk<RetenoDatabaseManagerWrappedLink>()
        val threshold = if (debug) HOUR_BEFORE_STUB_TIME else DAY_BEFORE_STUB_TIME
        val remoteLog = FakeRemoteLog()
        val reports: List<String>
            get() = remoteLog.infos
        private val configuration = mockk<SdkConfiguration>().also { every { it.isDebug } returns debug }
        val onIo = mutableListOf<Boolean>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = ClearOldDataAction(
            interactionsDb = interactionsDb,
            inAppInteractionsDb = inAppInteractionsDb,
            eventsDb = eventsDb,
            inboxDb = inboxDb,
            recomEventsDb = recomEventsDb,
            wrappedLinksDb = wrappedLinksDb,
            configuration = configuration,
            remoteLog = remoteLog,
            clock = { STUB_TIME.toInstant().toEpochMilli() },
            dispatchers = dispatchers,
            runner = traced.runner
        )

        init {
            every { interactionsDb.deleteInteractionByTime(threshold) } answers {
                onIo += dispatchers.markedIo.isRunningHere
                emptyList()
            }
            every { inAppInteractionsDb.deleteInteractionsByTime(threshold) } returns emptyList()
            every { eventsDb.deleteEventsByTime(threshold) } returns emptyList()
            every { inboxDb.deleteAppInboxMessagesByTime(threshold) } returns 0
            every { recomEventsDb.deleteRecomEventsByTime(threshold) } returns 0
            every { wrappedLinksDb.deleteWrappedLinksByTime(threshold) } returns 0
        }
    }

    @Test
    fun `should delete outdated rows from every table`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.interactionsDb.deleteInteractionByTime(fixture.threshold) }
        verify(exactly = 1) { fixture.inAppInteractionsDb.deleteInteractionsByTime(fixture.threshold) }
        verify(exactly = 1) { fixture.eventsDb.deleteEventsByTime(fixture.threshold) }
        verify(exactly = 1) { fixture.inboxDb.deleteAppInboxMessagesByTime(fixture.threshold) }
        verify(exactly = 1) { fixture.recomEventsDb.deleteRecomEventsByTime(fixture.threshold) }
        verify(exactly = 1) { fixture.wrappedLinksDb.deleteWrappedLinksByTime(fixture.threshold) }
        assertEquals(listOf(true), fixture.onIo)
    }

    @Test
    fun `should delete rows older than 24 hours before now by default`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.interactionsDb.deleteInteractionByTime("2026-01-01T03:04:05Z") }
    }

    @Test
    fun `should not report anything when nothing was outdated`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.reports.isEmpty())
    }

    @Test
    fun `should report removed interactions per status`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.deleteInteractionByTime(fixture.threshold) } returns listOf(
            stubInteractionDb(rowId = "1", status = InteractionStatusDb.CLICKED),
            stubInteractionDb(rowId = "2", status = InteractionStatusDb.CLICKED),
            stubInteractionDb(rowId = "3", status = InteractionStatusDb.DELIVERED)
        )

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf("Removed interactions(CLICKED) - 2", "Removed interactions(DELIVERED) - 1"),
            fixture.reports
        )
    }

    @Test
    fun `should report removed in app interactions per status`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every {
            fixture.inAppInteractionsDb.deleteInteractionsByTime(fixture.threshold)
        } returns listOf(stubInAppInteractionDb(status = "OPEN"))

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("Removed inAppInteractions(OPEN) - 1"), fixture.reports)
    }

    @Test
    fun `should report removed events per type`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.deleteEventsByTime(fixture.threshold) } returns listOf(
            stubEventDb(rowId = "1"),
            stubEventDb(rowId = "2")
        )

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("Removed events(purchase) - 2"), fixture.reports)
    }

    @Test
    fun `should report removed inbox statuses recommendation events and links`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inboxDb.deleteAppInboxMessagesByTime(fixture.threshold) } returns 3
        every { fixture.recomEventsDb.deleteRecomEventsByTime(fixture.threshold) } returns 4
        every { fixture.wrappedLinksDb.deleteWrappedLinksByTime(fixture.threshold) } returns 5

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf("Outdated Inbox: - 3", "Outdated Recommendation Events: - 4", "Removed wrapped links - 5"),
            fixture.reports
        )
    }

    @Test
    fun `should keep clearing other tables when one table fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.deleteEventsByTime(fixture.threshold) } throws IllegalStateException("table locked")

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.wrappedLinksDb.deleteWrappedLinksByTime(fixture.threshold) }
        assertEquals("ClearOldEvents", fixture.traced.reports.single().action)
    }

    @Test
    fun `should trace threshold and nested table cleanups`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("ClearOldData  before=${fixture.threshold}"))
        assertTrue(
            lines.any { it.contains("✓ delete outdated interactions from local database") && it.contains("count=0") }
        )
        assertTrue(lines.any { it.contains("[r1.6] ClearOldWrappedLinks") })
    }

    @Test
    fun `should trace removed row reports as named steps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.wrappedLinksDb.deleteWrappedLinksByTime(fixture.threshold) } returns 2

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ report removed rows to remote log") })
    }

    @Test
    fun `should keep only one hour of data in debug mode`() = runUnitTest {
        given()
        val fixture = SutFixture(this, debug = true)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.interactionsDb.deleteInteractionByTime("2026-01-02T02:04:05Z") }
    }

    companion object {
        const val DAY_BEFORE_STUB_TIME = "2026-01-01T03:04:05Z"
        const val HOUR_BEFORE_STUB_TIME = "2026-01-02T02:04:05Z"
    }
}
