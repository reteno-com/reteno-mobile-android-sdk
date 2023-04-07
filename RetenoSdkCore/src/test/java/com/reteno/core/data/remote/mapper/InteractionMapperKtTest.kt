package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.model.interaction.InteractionRemote
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote
import org.junit.Assert.assertEquals
import org.junit.Test


class InteractionMapperKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val INTERACTION_ID = "interactionId"
        private const val INTERACTION_TIME = "interactionTime"
        private const val INTERACTION_TOKEN = "interactionToken"
        private const val INTERACTION_ACTION = "interactionAction"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenInteractionStatusDb_whenToRemote_thenInteractionStatusRemoteReturned() {
        // Given
        val interactionStatusDb = getInteractionStatusDb()
        val expectedInteractionStatusRemote = getInteractionStatusRemote()

        // When
        val actualInteractionStatusRemote = interactionStatusDb.toRemote()

        // Then
        assertEquals(expectedInteractionStatusRemote, actualInteractionStatusRemote)
    }

    @Test
    fun givenInteractionDb_whenToRemote_thenInteractionRemoteReturned() {
        // Given
        val interactionDb = getInteractionDb()
        val expectedInteractionRemote = getInteractionRemote()

        // When
        val actualInteractionRemote = interactionDb.toRemote()

        // Then
        assertEquals(expectedInteractionRemote, actualInteractionRemote)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getInteractionStatusDb() = InteractionStatusDb.DELIVERED
    private fun getInteractionStatusRemote() = InteractionStatusRemote.DELIVERED

    private fun getInteractionDb() = InteractionDb(
        interactionId = INTERACTION_ID,
        status = getInteractionStatusDb(),
        time = INTERACTION_TIME,
        token = INTERACTION_TOKEN
    )

    private fun getInteractionRemote() = InteractionRemote(
        status = getInteractionStatusRemote(),
        time = INTERACTION_TIME,
        token = INTERACTION_TOKEN
    )
    // endregion helper methods --------------------------------------------------------------------
}