package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseManagerAppInboxImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerAppInboxImpl(db.database)

        fun store(vararg messages: AppInboxMessageDb) {
            messages.forEach { sut.insertAppInboxMessage(it) }
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read opened messages oldest first up to the limit`() = runUnitTest {
        withSut {
            given()
            store(message("late", "2026-01-03T00:00:00Z"), message("early", "2026-01-01T00:00:00Z"))

            whenn()
            val all = sut.getAppInboxMessages()
            val limited = sut.getAppInboxMessages(limit = 1)

            then()
            assertEquals(listOf(message("early", "2026-01-01T00:00:00Z"), message("late", "2026-01-03T00:00:00Z")), all)
            assertEquals(listOf("early"), limited.map { it.id })
            assertEquals(2L, sut.getAppInboxMessagesCount())
        }
    }

    @Test
    fun `should remove stored messages missing a device, time or status`() = runUnitTest {
        withSut {
            given()
            db.execute("INSERT INTO AppInbox (messageId, status, time) VALUES ('no device', 'OPENED', 'now')")
            db.execute("INSERT INTO AppInbox (messageId, deviceId, status) VALUES ('no time', 'device', 'OPENED')")
            db.execute("INSERT INTO AppInbox (messageId, deviceId, time) VALUES ('no status', 'device', 'now')")

            whenn()
            val messages = sut.getAppInboxMessages()

            then()
            assertEquals(emptyList<AppInboxMessageDb>(), messages)
            assertEquals(0L, db.count("AppInbox"))
        }
    }

    @Test
    fun `should report but keep a stored message without id`() = runUnitTest {
        withSut {
            given()
            db.execute("INSERT INTO AppInbox (deviceId, status, time) VALUES ('device', 'OPENED', 'now')")

            whenn()
            val messages = sut.getAppInboxMessages()

            then()
            assertEquals(emptyList<AppInboxMessageDb>(), messages)
            assertEquals(1L, db.count("AppInbox"))
            assertEquals(
                listOf("readRows(): unreadable row without id in AppInbox"),
                db.remoteLog.errors.map { it.first }
            )
        }
    }

    @Test
    fun `should delete given messages`() = runUnitTest {
        withSut {
            given()
            store(message("a"), message("b"), message("c"))

            whenn()
            sut.deleteAppInboxMessages(listOf(message("a"), message("c")))

            then()
            assertEquals(listOf("b"), db.values("SELECT messageId FROM AppInbox"))
        }
    }

    @Test
    fun `should delete every message`() = runUnitTest {
        withSut {
            given()
            store(message("a"), message("b"))

            whenn()
            sut.deleteAllAppInboxMessages()

            then()
            assertEquals(0L, sut.getAppInboxMessagesCount())
        }
    }

    @Test
    fun `should delete messages opened before a time`() = runUnitTest {
        withSut {
            given()
            store(message("old", "2026-01-01T00:00:00Z"), message("new", "2026-02-01T00:00:00Z"))

            whenn()
            val deleted = sut.deleteAppInboxMessagesByTime("2026-01-15T00:00:00Z")

            then()
            assertEquals(1, deleted)
            assertEquals(listOf("new"), db.values("SELECT messageId FROM AppInbox"))
        }
    }

    @Test
    fun `should treat an outdated time as a value, not as sql`() = runUnitTest {
        withSut {
            given()
            store(message("kept", "2026-02-01T00:00:00Z"))

            whenn()
            val deleted = sut.deleteAppInboxMessagesByTime("2026' OR '1'='1")

            then()
            assertEquals(0, deleted)
            assertTrue(db.count("AppInbox") == 1L)
        }
    }

    private fun message(id: String, time: String = "2026-01-02T03:04:05Z") = AppInboxMessageDb(
        id = id,
        deviceId = "device-body",
        occurredDate = time,
        status = AppInboxMessageStatusDb.OPENED
    )
}
