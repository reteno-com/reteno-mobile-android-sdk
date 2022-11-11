package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
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

    private lateinit var SUT: InteractionController
    // endregion helper fields ---------------------------------------------------------------------

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
            interactionsRepository.saveInteraction(
                eq(INTERACTION_ID),
                eq(expectedInteraction)
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
            interactionsRepository.saveInteraction(
                eq(INTERACTION_ID),
                eq(expectedInteraction)
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
        verify(exactly = 0) { interactionsRepository.saveInteraction(any(), any()) }
    }

    @Test
    fun whenPushInteraction_thenRepositoryInteractionPushCalled() {
        SUT.pushInteractions()
        verify(exactly = 1) { interactionsRepository.pushInteractions() }
    }
}