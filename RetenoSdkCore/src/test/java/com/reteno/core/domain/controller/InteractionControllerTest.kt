package com.reteno.core.domain.controller

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Util
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class InteractionControllerTest : BaseRobolectricTest() {

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

        every { Util.getCurrentTimeStamp() } returns CURRENT_TIMESTAMP

        SUT = InteractionController(configRepository, interactionsRepository)
    }

    @Test
    fun givenTokenAvailable_whenOnInteractionDelivered_thenInteractionDeliveredPassedToRepository() = runTest {
        // Given
        coEvery { configRepository.getFcmToken() } answers { TOKEN }
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
    fun givenTokenAvailable_whenOnInteractionClicked_thenInteractionOpenedPassedToRepository() = runTest {
        // Given
        coEvery { configRepository.getFcmToken() } answers { TOKEN }
        // When
        SUT.onInteraction(INTERACTION_ID, InteractionStatus.CLICKED)

        // Then
        val expectedInteraction = Interaction(InteractionStatus.CLICKED, CURRENT_TIMESTAMP, TOKEN)
        verify(exactly = 1) {
            interactionsRepository.saveInteraction(
                eq(INTERACTION_ID),
                eq(expectedInteraction)
            )
        }
    }

    @Test
    fun givenTokenNotAvailable_whenOnInteraction_thenRepositoryNotCalled() = runTest {
        // Given
        coEvery { configRepository.getFcmToken() } answers { "" }
        // When
        SUT.onInteraction(INTERACTION_ID, InteractionStatus.DELIVERED)

        // Then
        verify(exactly = 0) { interactionsRepository.saveInteraction(any(), any()) }
    }

    @Test
    fun whenPushInteraction_thenRepositoryInteractionPushCalled() {
        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { interactionsRepository.pushInteractions() }
    }

    @Test
    fun whenClearOldInteractions_thenRepositoryInteractionClearOldCalledWithOutdatedDate() {
        // Given
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        // When
        SUT.clearOldInteractions()

        // Then
        verify(exactly = 1) { interactionsRepository.clearOldInteractions(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }
    }

}