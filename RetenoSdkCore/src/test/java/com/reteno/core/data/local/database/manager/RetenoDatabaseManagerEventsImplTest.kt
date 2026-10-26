package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseManagerEventsImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerEventsImpl(db.database)
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    private fun events(externalUserId: String?, vararg events: EventDb) =
        EventsDb(deviceId = "device-body", externalUserId = externalUserId, eventList = events.toList())

    @Test
    fun `should group stored events by device and user`() = runUnitTest {
        withSut {
            given()
            sut.insertEvents(events("user-1", PURCHASE))
            sut.insertEvents(events(null, VIEW))
            sut.insertEvents(events("user-1", VIEW))
            sut.insertEvents(events(null, PURCHASE))

            whenn()
            val stored = sut.getEvents()

            then()
            val expected = listOf(
                events("user-1", PURCHASE.copy(rowId = "1"), VIEW.copy(rowId = "3")),
                events(null, VIEW.copy(rowId = "2"), PURCHASE.copy(rowId = "4"))
            )
            assertEquals(expected, stored)
            assertEquals(4L, sut.getEventsCount())
            assertEquals(2L, db.count("Events"))
        }
    }

    @Test
    fun `should read at most the limit of events per group`() = runUnitTest {
        withSut {
            given()
            sut.insertEvents(events("user-1", PURCHASE, VIEW))

            whenn()
            val stored = sut.getEvents(limit = 1)

            then()
            assertEquals(listOf(events("user-1", PURCHASE.copy(rowId = "1"))), stored)
        }
    }

    @Test
    fun `should drop groups without events and remove unreadable rows`() = runUnitTest {
        withSut {
            given()
            sut.insertEvents(events("user-1", PURCHASE))
            db.execute("INSERT INTO Events (externalUserId) VALUES ('no device')")
            db.execute("INSERT INTO Events (deviceId) VALUES ('empty group')")
            db.execute("INSERT INTO Event (events_id, occurred) VALUES (1, 'no type')")
            db.execute("INSERT INTO Event (events_id, eventTypeKey) VALUES (1, 'no time')")

            whenn()
            val stored = sut.getEvents()

            then()
            assertEquals(listOf(events("user-1", PURCHASE.copy(rowId = "1"))), stored)
            assertEquals(listOf("device-body"), db.values("SELECT deviceId FROM Events"))
            assertEquals(1L, sut.getEventsCount())
        }
    }

    @Test
    fun `should delete given events and their emptied group`() = runUnitTest {
        withSut {
            given()
            sut.insertEvents(events("user-1", PURCHASE, VIEW))
            sut.insertEvents(events(null, VIEW))
            val (firstGroup) = sut.getEvents()

            whenn()
            sut.deleteEvents(firstGroup.copy(eventList = firstGroup.eventList + VIEW))

            then()
            assertEquals(listOf(events(null, VIEW.copy(rowId = "3"))), sut.getEvents())
            assertEquals(1L, db.count("Events"))
        }
    }

    @Test
    fun `should delete and give back events that occurred before a time`() = runUnitTest {
        withSut {
            given()
            sut.insertEvents(events("user-1", PURCHASE.copy(occurred = "2026-01-01T00:00:00.000Z")))
            sut.insertEvents(events(null, VIEW.copy(occurred = "2026-02-01T00:00:00.000Z")))

            whenn()
            val deleted = sut.deleteEventsByTime("2026-01-15T00:00:00.000Z")

            then()
            assertEquals(listOf(PURCHASE.copy(rowId = "1", occurred = "2026-01-01T00:00:00.000Z")), deleted)
            assertEquals(listOf("device-body"), db.values("SELECT deviceId FROM Events"))
            assertEquals(1L, sut.getEventsCount())
        }
    }

    companion object {
        private val PURCHASE = EventDb(
            eventTypeKey = "purchase",
            occurred = "2026-01-02T03:04:05.000Z",
            params = listOf(ParameterDb("sku", "sku-1"), ParameterDb("coupon", null))
        )
        private val VIEW = EventDb(eventTypeKey = "screenView", occurred = "2026-01-02T03:04:06.000Z")
    }
}
