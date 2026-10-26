package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
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
internal class RetenoDatabaseManagerRecomEventsImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerRecomEventsImpl(db.database)
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should group stored recommendation events by variant`() = runUnitTest {
        withSut {
            given()
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(CLICK)))
            sut.insertRecomEvents(RecomEventsDb("variant-2", listOf(IMPRESSION)))
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(IMPRESSION)))

            whenn()
            val stored = listOf(sut.getRecomEvents(), sut.getRecomEvents(limit = 1))

            then()
            val all = listOf(
                RecomEventsDb("variant-1", listOf(CLICK.copy(rowId = "1"), IMPRESSION.copy(rowId = "3"))),
                RecomEventsDb("variant-2", listOf(IMPRESSION.copy(rowId = "2")))
            )
            val limited = listOf(
                RecomEventsDb("variant-1", listOf(CLICK.copy(rowId = "1"))),
                RecomEventsDb("variant-2", listOf(IMPRESSION.copy(rowId = "2")))
            )
            assertEquals(listOf(all, limited), stored)
            assertEquals(3L, sut.getRecomEventsCount())
        }
    }

    @Test
    fun `should report and skip recommendation events without any event`() = runUnitTest {
        withSut {
            given()
            val empty = listOf(RecomEventsDb("variant-1", null), RecomEventsDb("variant-2", emptyList()))

            whenn()
            empty.forEach { sut.insertRecomEvents(it) }

            then()
            assertEquals(0L, db.count("RecomEvents"))
            assertEquals(2, db.remoteLog.errors.size)
        }
    }

    @Test
    fun `should skip events when their variant cannot be stored`() = runUnitTest {
        withSut {
            given()
            db.dropTable("RecomEvents")
            db.execute("CREATE TABLE RecomEvents (recomVariantId TEXT PRIMARY KEY NOT NULL, required TEXT NOT NULL)")

            whenn()
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(CLICK)))

            then()
            assertEquals(0L, sut.getRecomEventsCount())
        }
    }

    @Test
    fun `should drop variants without events and remove unreadable events`() = runUnitTest {
        withSut {
            given()
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(CLICK)))
            db.execute("INSERT INTO RecomEvents (recomVariantId) VALUES ('empty')")
            listOf(
                "INSERT INTO RecomEvent (recomVariantId, occurred, eventType) VALUES ('variant-1', 'now', 'CLICKS')",
                "INSERT INTO RecomEvent (recomVariantId, productId, eventType) VALUES ('variant-1', 'p', 'CLICKS')",
                "INSERT INTO RecomEvent (recomVariantId, productId, occurred) VALUES ('variant-1', 'p', 'now')"
            ).forEach { db.execute(it) }

            whenn()
            val stored = sut.getRecomEvents()

            then()
            assertEquals(listOf(RecomEventsDb("variant-1", listOf(CLICK.copy(rowId = "1")))), stored)
            assertEquals(listOf("variant-1"), db.values("SELECT recomVariantId FROM RecomEvents"))
            assertEquals(1L, sut.getRecomEventsCount())
        }
    }

    @Test
    fun `should delete given events and their emptied variant`() = runUnitTest {
        withSut {
            given()
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(CLICK)))
            sut.insertRecomEvents(RecomEventsDb("variant-2", listOf(IMPRESSION)))
            val stored = sut.getRecomEvents()

            whenn()
            sut.deleteRecomEvents(stored.take(1) + RecomEventsDb("variant-2", null) + RecomEventsDb("x", listOf(CLICK)))

            then()
            assertEquals(listOf(RecomEventsDb("variant-2", listOf(IMPRESSION.copy(rowId = "2")))), sut.getRecomEvents())
        }
    }

    @Test
    fun `should delete events that occurred before a time`() = runUnitTest {
        withSut {
            given()
            val old = CLICK.copy(occurred = "2026-01-01T00:00:00Z")
            val recent = IMPRESSION.copy(occurred = "2026-02-01T00:00:00Z")
            sut.insertRecomEvents(RecomEventsDb("variant-1", listOf(old)))
            sut.insertRecomEvents(RecomEventsDb("variant-2", listOf(recent)))

            whenn()
            val deleted = sut.deleteRecomEventsByTime("2026-01-15T00:00:00Z")

            then()
            assertEquals(1, deleted)
            assertEquals(listOf("variant-2"), db.values("SELECT recomVariantId FROM RecomEvents"))
        }
    }

    companion object {
        private val CLICK = RecomEventDb(
            recomEventType = RecomEventTypeDb.CLICKS,
            occurred = "2026-01-02T03:04:05Z",
            productId = "sku-1"
        )
        private val IMPRESSION = RecomEventDb(
            recomEventType = RecomEventTypeDb.IMPRESSIONS,
            occurred = "2026-01-02T03:04:06Z",
            productId = "sku-2"
        )
    }
}
