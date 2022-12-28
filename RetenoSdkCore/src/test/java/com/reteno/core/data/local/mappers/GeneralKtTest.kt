package com.reteno.core.data.local.mappers

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.BooleanDb
import org.junit.Assert.assertEquals
import org.junit.Test


class GeneralKtTest : BaseUnitTest() {

    @Test
    fun givenTrue_whenToDb_thenBooleanDbTrueReturned() {
        // Given
        val input = true

        // When
        val result = input.toDb()

        // Then
        assertEquals(BooleanDb.TRUE, result)
    }

    @Test
    fun givenFalse_whenToDb_thenBooleanDbFalseReturned() {
        // Given
        val input = false

        // When
        val result = input.toDb()

        // Then
        assertEquals(BooleanDb.FALSE, result)
    }

    @Test
    fun givenTrue_whenToIntValue_then1Returned() {
        // Given
        val input = true

        // When
        val result = input.toIntValue()

        // Then
        assertEquals(1, result)
    }

    @Test
    fun givenFalse_whenToIntValue_then0Returned() {
        // Given
        val input = false

        // When
        val result = input.toIntValue()

        // Then
        assertEquals(0, result)
    }
}