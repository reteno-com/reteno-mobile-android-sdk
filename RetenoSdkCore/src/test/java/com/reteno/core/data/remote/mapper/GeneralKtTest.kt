package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.BooleanDb
import org.junit.Assert.assertEquals
import org.junit.Test


class GeneralKtTest : BaseUnitTest() {

    @Test
    fun givenBooleanDbTrue_whenToRemote_thenTrueReturned() {
        // Given
        val input = BooleanDb.TRUE

        // When
        val result = input.toRemote()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun givenBooleanDbFalse_whenToRemote_thenFalseReturned() {
        // Given
        val input = BooleanDb.FALSE

        // When
        val result = input.toRemote()

        // Then
        assertEquals(false, result)
    }
}