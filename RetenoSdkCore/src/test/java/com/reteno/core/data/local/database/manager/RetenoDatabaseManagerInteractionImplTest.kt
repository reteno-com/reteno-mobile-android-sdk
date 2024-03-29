package com.reteno.core.data.local.database.manager

import android.content.ContentValues
import android.database.Cursor
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
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.ZonedDateTime


class RetenoDatabaseManagerInteractionImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val TIMESTAMP = "TimeStampHere_Z"

        private const val ROW_ID_1 = "1"
        private const val ROW_ID_2 = "2"
        private const val INTERACTION_ID = "interactionId"
        private val INTERACTION_STATUS = InteractionStatusDb.DELIVERED
        private const val INTERACTION_TIME = "interactionTime"
        private const val INTERACTION_TOKEN = "interactionToken"

        private val interaction1 = InteractionDb(
            rowId = ROW_ID_1,
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            token = INTERACTION_TOKEN
        )
        private val interaction2 = InteractionDb(
            rowId = ROW_ID_2,
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
    fun givenInteractionProvided_wheDeleteInteraction_thenDeleteFromDatabaseCalled() {
        // When
        SUT.deleteInteraction(interaction1)

        // Then
        verify(exactly = 1) {
            database.delete(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                whereClause = eq("${InteractionSchema.COLUMN_INTERACTION_ROW_ID}=?"),
                whereArgs = eq(arrayOf(ROW_ID_1))
            )
        }
    }

    @Test
    fun givenDatabaseDeleteReturns1_wheDeleteInteraction_thenResultIsTrue() {
        // Given
        every { database.delete(table = any(), whereClause = any(), whereArgs = any()) } returns 1

        // When
        val result = SUT.deleteInteraction(interaction1)

        // Then
        assertTrue(result)
    }

    @Test
    fun givenDatabaseDeleteReturns0_wheDeleteInteraction_thenResultIsFalse() {
        // Given
        every { database.delete(table = any(), whereClause = any(), whereArgs = any()) } returns 0

        // When
        val result = SUT.deleteInteraction(interaction1)

        // Then
        assertFalse(result)
    }

    @Test
    fun givenOutdatedInteractionsFoundInDatabase_whenDeleteInteractionsByTime_thenInteractionsDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val whereClauseExpected = "${InteractionSchema.COLUMN_INTERACTION_TIME} < '$outdatedTime'"

        mockCursorRecordsNumber(2)
        every { database.query(
            table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
            columns = eq(InteractionSchema.getAllColumns()),
            selection = eq(whereClauseExpected)
        ) } returns cursor
        every { cursor.getInteraction() } returns interaction1 andThen interaction2

        // When
        val deletedInteractions = SUT.deleteInteractionByTime(outdatedTime)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        }
        verify(exactly = 1) {
            database.delete(
                eq(InteractionSchema.TABLE_NAME_INTERACTION),
                eq(whereClauseExpected)
            )
        }
        assertEquals(listOf(interaction1, interaction2), deletedInteractions)
    }

    @Test
    fun givenOutdatedInteractionsNotFoundInDatabase_whenDeleteInteractionsByTime_thenInteractionsNotDeleted() {
        // Given
        val outdatedTime = ZonedDateTime.now().formatToRemote()
        val whereClauseExpected = "${InteractionSchema.COLUMN_INTERACTION_TIME} < '$outdatedTime'"

        mockCursorRecordsNumber(0)
        every { database.query(
            table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
            columns = eq(InteractionSchema.getAllColumns()),
            selection = eq(whereClauseExpected)
        ) } returns cursor

        // When
        val deletedInteractions = SUT.deleteInteractionByTime(outdatedTime)

        // Then
        verify(exactly = 1) {
            database.query(
                table = eq(InteractionSchema.TABLE_NAME_INTERACTION),
                columns = eq(InteractionSchema.getAllColumns()),
                selection = eq(whereClauseExpected)
            )
        }
        verify(exactly = 1) {
            database.delete(
                eq(InteractionSchema.TABLE_NAME_INTERACTION),
                eq(whereClauseExpected)
            )
        }
        assertEquals(emptyList<InteractionDb>(), deletedInteractions)
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