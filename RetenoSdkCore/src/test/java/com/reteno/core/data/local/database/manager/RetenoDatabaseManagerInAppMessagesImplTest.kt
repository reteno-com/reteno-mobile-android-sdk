package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.model.iam.SegmentDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.core.util.Util
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
internal class RetenoDatabaseManagerInAppMessagesImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerInAppMessagesImpl(db.database)

        fun store(vararg messages: InAppMessageDb) {
            sut.insertInAppMessages(messages.toList())
            db.stampRows("InAppMessage")
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    private fun message(id: Long, segment: SegmentDb? = null, position: String? = null) = InAppMessageDb(
        messageId = id,
        messageInstanceId = id * 10,
        displayRules = """{"frequency":{}}""",
        lastShowTime = if (segment == null) null else 1_700_000_000_000L,
        showCount = id,
        layoutType = if (segment == null) null else "FULL",
        model = if (segment == null) null else """{"title":"Sale"}""",
        position = position
    ).also { it.segment = segment }

    @Test
    fun `should read back every field of stored messages with their segments up to the limit`() = runUnitTest {
        withSut {
            given()
            store(message(1, CHECKED_SEGMENT, position = "TOP"), message(2, UNCHECKED_SEGMENT), message(3))

            whenn()
            val messages = listOf(sut.getInAppMessages(), sut.getInAppMessages(limit = 1))

            then()
            val stored = listOf(
                message(1, CHECKED_SEGMENT, position = "TOP").copy(rowId = "1", createdAt = STORED_AT_MILLIS),
                message(2, UNCHECKED_SEGMENT).copy(rowId = "2", createdAt = STORED_AT_MILLIS),
                message(3).copy(rowId = "3", createdAt = STORED_AT_MILLIS, model = STORED_NULL_MODEL)
            )
            assertEquals(listOf(stored, stored.take(1)), messages)
            assertEquals(
                listOf(CHECKED_SEGMENT, UNCHECKED_SEGMENT, null),
                messages.first().map { it.segment }
            )
            assertEquals(3L, sut.getInAppMessagesCount())
        }
    }

    @Test
    fun `should store no segments when the messages could not be stored`() = runUnitTest {
        withSut {
            given()
            db.database.rawQuery("SELECT 1").close()
            db.database.writableDatabase.close()

            whenn()
            sut.insertInAppMessages(listOf(message(1, CHECKED_SEGMENT)))

            then()
            assertEquals(0L, sut.getInAppMessagesCount())
        }
    }

    @Test
    fun `should remove a stored message without time`() = runUnitTest {
        withSut {
            given()
            store(message(1), message(2))
            db.execute("UPDATE InAppMessage SET timeStamp = NULL WHERE iamId = 2")

            whenn()
            val messages = sut.getInAppMessages()

            then()
            assertEquals(listOf(1L), messages.map { it.messageId })
            assertEquals(1L, sut.getInAppMessagesCount())
        }
    }

    @Test
    fun `should delete given messages by their id`() = runUnitTest {
        withSut {
            given()
            store(message(1), message(2), message(3))

            whenn()
            sut.deleteInAppMessages(listOf(message(1), message(3)))

            then()
            assertEquals(listOf(2L), sut.getInAppMessages().map { it.messageId })
        }
    }

    @Test
    fun `should delete every message with its segment`() = runUnitTest {
        withSut {
            given()
            store(message(1, CHECKED_SEGMENT), message(2))

            whenn()
            sut.deleteAllInAppMessages()

            then()
            assertEquals(listOf(0L, 0L), listOf(sut.getInAppMessagesCount(), db.count("Segment")))
        }
    }

    companion object {
        private val STORED_AT_MILLIS = Util.formatSqlDateToTimestamp(TestDatabase.STORED_AT)
        private const val STORED_NULL_MODEL = "null"

        private val CHECKED_SEGMENT = SegmentDb(
            segmentId = 42,
            isInSegment = true,
            lastCheckTime = 1_700_000_000_000L,
            checkStatusCode = 200,
            retryAfter = 60_000L
        )

        private val UNCHECKED_SEGMENT = SegmentDb(
            segmentId = 43,
            isInSegment = false,
            lastCheckTime = null,
            checkStatusCode = null,
            retryAfter = null
        )
    }
}
