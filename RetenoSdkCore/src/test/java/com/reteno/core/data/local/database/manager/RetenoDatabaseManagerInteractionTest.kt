package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.database.util.getInteraction
import com.reteno.core.data.local.database.util.putInteraction
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerInteractionTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val TIMESTAMP = "TimeStampHere_Z"

        private const val INTERACTION_ID = "interactionId"
        private val INTERACTION_STATUS = InteractionStatusDb.DELIVERED
        private const val INTERACTION_TIME = "interactionTime"
        private const val INTERACTION_TOKEN = "interactionToken"

        private val interaction1 = InteractionDb(
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            token = INTERACTION_TOKEN
        )
        private val interaction2 = InteractionDb(
            interactionId = "${INTERACTION_ID}_2",
            status = INTERACTION_STATUS,
            time = "${INTERACTION_TIME}_2}",
            token = "${INTERACTION_TOKEN}_2"
        )

        private const val COLUMN_INDEX_TIMESTAMP = 1
        private const val COLUMN_INDEX_INTERACTION_ROW_ID = 2
        private const val COLUMN_INDEX_INTERACTION_ID = 3
        private const val COLUMN_INDEX_INTERACTION_STATUS = 4
        private const val COLUMN_INDEX_INTERACTION_TIME = 5
        private const val COLUMN_INDEX_INTERACTION_TOKEN = 6

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic(Cursor::getInteraction)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(Cursor::getInteraction)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor

    private lateinit var SUT: RetenoDatabaseManagerInteraction
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_TIMESTAMP) } returns TIMESTAMP
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerInteractionImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
    }

    @Test
    fun givenValidInteractionProvided_whenInsertInteraction_thenInteractionIsSavedToDb() {
        // Given
        val expectedContentValues = ContentValues().apply {
            putInteraction(interaction1)
        }

        var actualContentValues = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValues = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT.insertInteraction(interaction1)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValues, actualContentValues)
    }

    @Test
    fun givenInteractionsAvailableInDatabase_whenGetInteraction_thenInteractionsReturned() {
        // Given
        mockCursorRecordsNumber(2)
        mockDatabaseQuery()

        every { cursor.getInteraction() } returns interaction1 andThen interaction2

        // When
        val interactions = SUT.getInteractions(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertEquals(2, interactions.size)
        assertEquals(interaction1, interactions[0])
        assertEquals(interaction2, interactions[1])
    }

    @Test
    fun givenInteractionNotAvailableInDatabase_whenGetInteraction_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        // When
        val interactions = SUT.getInteractions(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInteractionCorruptedInDatabaseAndRowIdDetected_whenGetInteraction_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getInteraction() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_INTERACTION_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val interactions = SUT.getInteractions(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) {
            database.delete(
                InteractionSchema.TABLE_NAME_INTERACTION,
                "${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?",
                arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInteractionCorruptedInDatabaseAndRowIdNotDetected_whenGetInteraction_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getInteraction() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_INTERACTION_ROW_ID) } returns null

        // When
        val interactions = SUT.getInteractions(null)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                orderBy = eq("${DbSchema.COLUMN_TIMESTAMP} ASC"),
                limit = null
            )
        }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(interactions.isEmpty())
    }

    @Test
    fun givenInteractionCountEmpty_whenGetInteractionCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(InteractionSchema.TABLE_NAME_INTERACTION) } returns recordsCount

        // When
        val count = SUT.getInteractionCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenInteractionCountNonEmpty_whenGetInteractionCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(InteractionSchema.TABLE_NAME_INTERACTION) } returns recordsCount

        // When
        val count = SUT.getInteractionCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteInteractionsOldest_thenInteractionsDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID} " +
                    "in (select ${InteractionSchema.COLUMN_INTERACTION_ROW_ID} " +
                    "from ${InteractionSchema.TABLE_NAME_INTERACTION} " +
                    "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                    "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteInteractions(count, true)

        // Then
        verify(exactly = 1) { database.delete(InteractionSchema.TABLE_NAME_INTERACTION, whereClauseExpected) }
    }

    @Test
    fun given_whenDeleteInteractionsNewest_thenInteractionsDeleted() {
        // Given
        val order = "DESC"
        val count = 4
        val whereClauseExpected = "${InteractionSchema.COLUMN_INTERACTION_ROW_ID} " +
                "in (select ${InteractionSchema.COLUMN_INTERACTION_ROW_ID} " +
                "from ${InteractionSchema.TABLE_NAME_INTERACTION} " +
                "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"

        every { database.delete(any(), any(), any()) } returns 0

        // When
        SUT.deleteInteractions(count, false)

        // Then
        verify(exactly = 1) { database.delete(InteractionSchema.TABLE_NAME_INTERACTION, whereClauseExpected) }
    }

    @Test
    fun whenDeleteInteractionsByTime_thenInteractionsDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val countExpected = 2
        val whereClauseExpected = "${InteractionSchema.COLUMN_INTERACTION_TIME} < '$outdatedTime'"

        every { database.delete(any(), any(), any()) } returns countExpected

        // When
        SUT.deleteInteractionByTime(outdatedTime)

        // Then
        verify(exactly = 1) { database.delete(InteractionSchema.TABLE_NAME_INTERACTION, whereClauseExpected) }
    }


    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP) } returns COLUMN_INDEX_TIMESTAMP

        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ROW_ID) } returns COLUMN_INDEX_INTERACTION_ROW_ID
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ID) } returns COLUMN_INDEX_INTERACTION_ID
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_STATUS) } returns COLUMN_INDEX_INTERACTION_STATUS
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TIME) } returns COLUMN_INDEX_INTERACTION_TIME
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TOKEN) } returns COLUMN_INDEX_INTERACTION_TOKEN
    }

    private fun mockCursorRecordsNumber(number: Int) {
        val responses = generateSequence(0) { it + 1 }
            .map { it < number }
            .take(number + 1)
            .toList()
        every { cursor.moveToNext() } returnsMany responses
    }

    private fun mockDatabaseQuery() {
        every {
            database.query(
                table = InteractionSchema.TABLE_NAME_INTERACTION,
                columns = InteractionSchema.getAllColumns(),
                orderBy = any(),
                limit = null
            )
        } returns cursor
    }
    // endregion helper methods --------------------------------------------------------------------
}