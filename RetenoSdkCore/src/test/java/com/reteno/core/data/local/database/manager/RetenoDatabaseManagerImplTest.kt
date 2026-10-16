package com.reteno.core.data.local.database.manager

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RetenoDatabaseManagerImplTest {

    private class SutFixture(private val pending: Map<String, Long> = emptyMap()) {
        private fun count(name: String) = pending[name] ?: 0L

        val devices = mockk<RetenoDatabaseManagerDevice> { every { getUnSyncedDeviceCount() } returns count(DEVICES) }
        val users = mockk<RetenoDatabaseManagerUser> { every { getUnSyncedUserCount() } returns count(USERS) }
        val interactions = mockk<RetenoDatabaseManagerInteraction> {
            every { getInteractionCount() } returns count(INTERACTIONS)
        }
        val events = mockk<RetenoDatabaseManagerEvents> { every { getEventsCount() } returns count(EVENTS) }
        val inbox = mockk<RetenoDatabaseManagerAppInbox> { every { getAppInboxMessagesCount() } returns count(INBOX) }
        val recomEvents = mockk<RetenoDatabaseManagerRecomEvents> {
            every { getRecomEventsCount() } returns count(RECOM_EVENTS)
        }
        val links = mockk<RetenoDatabaseManagerWrappedLink> { every { getWrappedLinksCount() } returns count(LINKS) }
        val logEvents = mockk<RetenoDatabaseManagerLogEvent> { every { getLogEventsCount() } returns count(LOG_EVENTS) }
        val inAppInteractions = mockk<RetenoDatabaseManagerInAppInteraction> {
            every { getInAppInteractionsCount() } returns count(IN_APP_INTERACTIONS)
        }
        val sut = RetenoDatabaseManagerImpl(
            deviceManager = devices,
            userManager = users,
            interactionManager = interactions,
            eventsManager = events,
            appInboxManager = inbox,
            recomEventsManager = recomEvents,
            wrappedLinkManager = links,
            logEventManager = logEvents,
            inAppInteractionManager = inAppInteractions
        )
    }

    @Test
    fun `should have nothing to sync when every table is empty`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val hasData = fixture.sut.hasDataForSync()

        then()
        assertEquals(false, hasData)
    }

    @Test
    fun `should have data to sync when any table has rows to send`() = runUnitTest {
        given()
        val fixtures = ALL_TABLES.map { SutFixture(mapOf(it to 1L)) }

        whenn()
        val hasData = fixtures.map { it.sut.hasDataForSync() }

        then()
        assertEquals(List(ALL_TABLES.size) { true }, hasData)
    }

    companion object {
        private const val DEVICES = "devices"
        private const val USERS = "users"
        private const val INTERACTIONS = "interactions"
        private const val EVENTS = "events"
        private const val INBOX = "inbox"
        private const val RECOM_EVENTS = "recomEvents"
        private const val LINKS = "links"
        private const val LOG_EVENTS = "logEvents"
        private const val IN_APP_INTERACTIONS = "inAppInteractions"
        private val ALL_TABLES = listOf(
            DEVICES, USERS, INTERACTIONS, EVENTS, INBOX, RECOM_EVENTS, LINKS, LOG_EVENTS, IN_APP_INTERACTIONS
        )
    }
}
