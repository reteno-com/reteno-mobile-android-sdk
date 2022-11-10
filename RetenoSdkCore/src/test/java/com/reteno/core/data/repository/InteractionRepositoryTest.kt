package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.InteractionModelDb
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction
import com.reteno.core.model.interaction.InteractionStatus
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class InteractionRepositoryTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val TOKEN = "some_token"
        private const val INTERACTION_ID = "interaction_id"
        private val INTERACTION_STATUS = InteractionStatus.DELIVERED
        private const val CURRENT_TIMESTAMP = "2022-11-22T11:11:11Z"

        private val EXPECTED_API_CONTRACT_URL =
            ApiContract.RetenoApi.InteractionStatus(INTERACTION_ID).url
        private const val EXPECTED_URL =
            "https://api.reteno.com/api/v1/interactions/$INTERACTION_ID/status"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var retenoDatabaseManager: RetenoDatabaseManager
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: InteractionRepositoryImpl

    @Before
    override fun before() {
        super.before()
        mockkObject(PushOperationQueue)
        SUT = InteractionRepositoryImpl(apiClient, retenoDatabaseManager)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
    }

    @Test
    fun givenValidInteraction_whenInteractionSent_thenSaveInteraction() {
        // Given
        val interaction = Interaction(INTERACTION_STATUS, CURRENT_TIMESTAMP, TOKEN)
        val dbInteraction = interaction.toDb(INTERACTION_ID)

        // When
        SUT.saveInteraction(INTERACTION_ID, interaction)

        // Then
        verify(exactly = 1) { retenoDatabaseManager.insertInteraction(dbInteraction) }
    }

    @Test
    fun givenValidInteraction_whenInteractionPush_thenApiClientPutsInteractionWithCorrectParameters() {
        // Given
        val interaction = Interaction(INTERACTION_STATUS, CURRENT_TIMESTAMP, TOKEN)
        val dbInteraction = interaction.toDb(INTERACTION_ID)
        val expectedInteractionJson =
            "{\"status\":\"$INTERACTION_STATUS\",\"time\":\"$CURRENT_TIMESTAMP\",\"token\":\"$TOKEN\"}"

        val apiContractCaptured = slot<ApiContract>()
        val jsonBodyCaptured = slot<String>()
        every {
            apiClient.put(
                url = capture(apiContractCaptured),
                jsonBody = capture(jsonBodyCaptured),
                responseHandler = any()
            )
        } just runs
        every { retenoDatabaseManager.getInteractions(any()) } returns listOf(dbInteraction) andThen emptyList<InteractionModelDb>()

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }

        Assert.assertEquals(EXPECTED_API_CONTRACT_URL, EXPECTED_URL)
        Assert.assertEquals(EXPECTED_API_CONTRACT_URL, apiContractCaptured.captured.url)
        Assert.assertEquals(expectedInteractionJson, jsonBodyCaptured.captured)
    }

    @Test
    fun givenValidInteraction_whenInteractionPushSuccessful_thenTryPushNextInteraction() {
        val dbInteraction = mockk<InteractionModelDb>(relaxed = true)
        every { retenoDatabaseManager.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        SUT.pushInteractions()

        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 2) { retenoDatabaseManager.deleteInteractions(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val dbInteraction = mockk<InteractionModelDb>(relaxed = true)
        every { retenoDatabaseManager.getInteractions(any()) } returns listOf(dbInteraction)
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        SUT.pushInteractions()

        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsNonRepeatable_thenTryPushNextInteraction() {
        val dbInteraction = mockk<InteractionModelDb>(relaxed = true)
        every { retenoDatabaseManager.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        SUT.pushInteractions()

        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 3) { retenoDatabaseManager.getInteractions(1) }
        verify(exactly = 2) { retenoDatabaseManager.deleteInteractions(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoInteractionInDb_whenInteractionPush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { retenoDatabaseManager.getInteractions(any()) } returns emptyList()

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 0) { apiClient.put(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

    }
}