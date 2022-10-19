package com.reteno.core.domain.controller

import com.reteno.core.BaseUnitTest
import com.reteno.core.data.local.ds.ConfigRepository
import com.reteno.core.data.remote.ds.InteractionRepository
import com.reteno.core.model.interaction.Interaction
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.core.util.Util
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test


class InteractionControllerTest : BaseUnitTest() {
    // region constants ----------------------------------------------------------------------------
    companion object {
        const val TOKEN = "some_token"
        const val INTERACTION_ID = "interaction_id"
        const val CURRENT_TIMESTAMP = "2022-11-22T11:11:11Z"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @MockK
    private lateinit var configRepository: ConfigRepository

    @RelaxedMockK
    private lateinit var interactionsRepository: InteractionRepository
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: InteractionController

    @Before
    override fun before() {
        super.before()

        mockkStatic(Util::class)
        every { Util.getCurrentTimeStamp() } returns CURRENT_TIMESTAMP

        SUT = InteractionController(configRepository, interactionsRepository)
    }

    @After
    override fun after() {
        super.after()
        unmockkStatic(Util::class)
    }

    @Test
    fun givenTokenAvailable_whenOnInteractionDelivered_thenInteractionDeliveredPassedToRepository() {
        // Given
        every { configRepository.getFcmToken() } returns TOKEN

        // When
        SUT.onInteraction(INTERACTION_ID, InteractionStatus.DELIVERED)

        // Then
        val expectedInteraction = Interaction(InteractionStatus.DELIVERED, CURRENT_TIMESTAMP, TOKEN)
        verify(exactly = 1) {
            interactionsRepository.sendInteraction(
                eq(INTERACTION_ID),
                eq(expectedInteraction),
                any()
            )
        }
    }

    @Test
    fun givenTokenAvailable_whenOnInteractionOpened_thenInteractionOpenedPassedToRepository() {
        // Given
        every { configRepository.getFcmToken() } returns TOKEN

        // When
        SUT.onInteraction(INTERACTION_ID, InteractionStatus.OPENED)

        // Then
        val expectedInteraction = Interaction(InteractionStatus.OPENED, CURRENT_TIMESTAMP, TOKEN)
        verify(exactly = 1) {
            interactionsRepository.sendInteraction(
                eq(INTERACTION_ID),
                eq(expectedInteraction),
                any()
            )
        }
    }

    @Test
    fun givenTokenNotAvailable_whenOnInteraction_thenRepositoryNotCalled() {
        // Given
        every { configRepository.getFcmToken() } returns ""

        // When
        SUT.onInteraction(INTERACTION_ID, InteractionStatus.DELIVERED)

        // Then
        verify(exactly = 0) { interactionsRepository.sendInteraction(any(), any(), any()) }
    }

    // region helper methods -----------------------------------------------------------------------

    // endregion helper methods --------------------------------------------------------------------


    // region helper classes -----------------------------------------------------------------------

    // endregion helper classes --------------------------------------------------------------------
}