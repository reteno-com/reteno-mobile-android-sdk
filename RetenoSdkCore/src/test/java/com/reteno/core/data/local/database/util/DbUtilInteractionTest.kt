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
        private const val INTERACTION_ID = "interactionId1"
        private val INTERACTION_STATUS = InteractionStatusDb.DELIVERED
        private const val INTERACTION_TIME = "interactionTime1"
        private const val INTERACTION_TOKEN = "interactionToken1"

        private const val COLUMN_INDEX_INTERACTION_ID = 1
        private const val COLUMN_INDEX_INTERACTION_TIME = 2
        private const val COLUMN_INDEX_INTERACTION_STATUS = 3
        private const val COLUMN_INDEX_INTERACTION_TOKEN = 4
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
            token = INTERACTION_TOKEN
        )

        val keySet = arrayOf(
            InteractionSchema.COLUMN_INTERACTION_ID,
            InteractionSchema.COLUMN_INTERACTION_STATUS,
            InteractionSchema.COLUMN_INTERACTION_TIME,
            InteractionSchema.COLUMN_INTERACTION_TOKEN
        )

        // When
        contentValues.putInteraction(interaction)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(INTERACTION_ID, contentValues.get(InteractionSchema.COLUMN_INTERACTION_ID))
        assertEquals(INTERACTION_STATUS.toString(), contentValues.get(InteractionSchema.COLUMN_INTERACTION_STATUS))
        assertEquals(INTERACTION_TIME, contentValues.get(InteractionSchema.COLUMN_INTERACTION_TIME))
        assertEquals(INTERACTION_TOKEN, contentValues.get(InteractionSchema.COLUMN_INTERACTION_TOKEN))
    }

    @Test
    fun givenInteractionFull_whenGetInteraction_thenInteractionReturned() {
        // Given
        mockInteractionFull()

        val expectedInteraction = InteractionDb(
            interactionId = INTERACTION_ID,
            status = INTERACTION_STATUS,
            time = INTERACTION_TIME,
            token = INTERACTION_TOKEN
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
        assertNull(actualInteraction)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockInteractionFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
    }

    private fun mockInteractionEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns null
    }

    private fun mockInteractionIdNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
    }

    private fun mockInteractionStatusNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
    }

    private fun mockInteractionTimeNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns INTERACTION_TOKEN
    }

    private fun mockInteractionTokenNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_ID) } returns INTERACTION_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TIME) } returns INTERACTION_TIME
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_STATUS) } returns INTERACTION_STATUS.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_INTERACTION_TOKEN) } returns null
    }

    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_ID) } returns COLUMN_INDEX_INTERACTION_ID
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TIME) } returns COLUMN_INDEX_INTERACTION_TIME
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_STATUS) } returns COLUMN_INDEX_INTERACTION_STATUS
        every { cursor.getColumnIndex(InteractionSchema.COLUMN_INTERACTION_TOKEN) } returns COLUMN_INDEX_INTERACTION_TOKEN
    }
    // endregion helper methods --------------------------------------------------------------------
}