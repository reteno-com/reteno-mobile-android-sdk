package com.reteno.core.data.local.database.util

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertNull
import net.sqlcipher.Cursor
import org.junit.Assert.assertEquals
import org.junit.Test


class DbUtilInteractionTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID = "909"
        private const val INTERACTION_ID = "interactionId1"
        private val INTERACTION_STATUS = InteractionStatusDb.DELIVERED
        private const val INTERACTION_TIME = "interactionTime1"
        private const val INTERACTION_TOKEN = "interactionToken1"
        private const val INTERACTION_ACTION = "interactionAction1"

        private const val COLUMN_INDEX_ROW_ID = 1
        private const val COLUMN_INDEX_INTERACTION_ID = 2
        private const val COLUMN_INDEX_INTERACTION_TIME = 3
        private const val COLUMN_INDEX_INTERACTION_STATUS = 4
        private const val COLUMN_INDEX_INTERACTION_TOKEN = 5
        private const val COLUMN_INDEX_INTERACTION_ACTION = 6
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private val contentValues = ContentValues()

    @MockK
    private lateinit var cursor: Cursor
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        contentValues.clear()
        mockColumnIndexes()
    }

    override fun after() {
        super.after()
        contentValues.clear()
        clearMocks(cursor)
    }

    @Test
    fun givenInteractionProvided_whenPutInteraction_thenContentValuesUpdated() {
        // Given
        val interaction = InteractionDb(
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            token = INTERACTION_TOKEN,
            action = INTERACTION_ACTION,
        )

        val keySet = arrayOf(
            InteractionSchema.COLUMN_INTERACTION_ID,
            InteractionSchema.COLUMN_INTERACTION_STATUS,
            InteractionSchema.COLUMN_INTERACTION_TIME,
            InteractionSchema.COLUMN_INTERACTION_TOKEN,
            InteractionSchema.COLUMN_INTERACTION_ACTION,
        )

        // When
        contentValues.putInteraction(interaction)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(
            INTERACTION_ID,
            contentValues.get(InteractionSchema.COLUMN_INTERACTION_ID)
        )
        assertEquals(
            INTERACTION_STATUS.toString(),
            contentValues.get(InteractionSchema.COLUMN_INTERACTION_STATUS)
        )
        assertEquals(
            INTERACTION_TIME,
            contentValues.get(InteractionSchema.COLUMN_INTERACTION_TIME)
        )
        assertEquals(
            INTERACTION_TOKEN,
            contentValues.get(InteractionSchema.COLUMN_INTERACTION_TOKEN)
        )
        assertEquals(
            INTERACTION_ACTION,
            contentValues.get(InteractionSchema.COLUMN_INTERACTION_ACTION)
        )
    }

    @Test
    fun givenInteractionFull_whenGetInteraction_thenInteractionReturned() {
        // Given
        mockInteractionFull()

        val expectedInteraction = InteractionDb(
            rowId = ROW_ID,
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            token = INTERACTION_TOKEN,
            action = INTERACTION_ACTION,
        )

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertEquals(expectedInteraction, actualInteraction)
    }

    @Test
    fun givenInteractionAction_whenGetInteraction_thenInteractionReturned() {
        // Given
        mockInteractionWithAction()

        val expectedInteraction = InteractionDb(
            rowId = ROW_ID,
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            action = INTERACTION_ACTION,
        )

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertEquals(expectedInteraction, actualInteraction)
    }

    @Test
    fun givenInteractionEmpty_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionEmpty()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction)
    }

    @Test
    fun givenInteractionIdNull_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionIdNull()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction)
    }

    @Test
    fun givenInteractionStatusNull_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionStatusNull()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction)
    }

    @Test
    fun givenInteractionTimeNull_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionTimeNull()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction)
    }

    @Test
    fun givenInteractionTokenNull_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionTokenNull()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction?.token)
    }

    @Test
    fun givenInteractionWithAction_whenGetInteraction_thenNullReturned() {
        // Given
        mockInteractionActionNull()

        // When
        val actualInteraction = cursor.getInteraction()

        // Then
        assertNull(actualInteraction?.action)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockInteractionFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns INTERACTION_ACTION
    }

    private fun mockInteractionEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns null
    }

    private fun mockInteractionIdNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns null
    }

    private fun mockInteractionStatusNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns null
    }

    private fun mockInteractionTimeNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns null
    }

    private fun mockInteractionTokenNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns INTERACTION_ACTION
    }

    private fun mockInteractionActionNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns null
    }

    private fun mockInteractionWithAction() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_ROW_ID) } returns ROW_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ACTION) } returns INTERACTION_ACTION
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ROW_ID) } returns COLUMN_INDEX_ROW_ID
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ID) } returns COLUMN_INDEX_INTERACTION_ID
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TIME) } returns COLUMN_INDEX_INTERACTION_TIME
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_STATUS) } returns COLUMN_INDEX_INTERACTION_STATUS
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TOKEN) } returns COLUMN_INDEX_INTERACTION_TOKEN
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ACTION) } returns COLUMN_INDEX_INTERACTION_ACTION

    }
    // endregion helper methods --------------------------------------------------------------------
}