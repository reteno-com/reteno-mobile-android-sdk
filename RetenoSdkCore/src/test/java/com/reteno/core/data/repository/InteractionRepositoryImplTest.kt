package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.*
import java.time.ZonedDateTime

class InteractionRepositoryImplTest : BaseUnitTest() {

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

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectOperationQueue()
            mockObjectPushOperationQueue()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectOperationQueue()
            unMockObjectPushOperationQueue()
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var databaseManagerInteraction: RetenoDatabaseManagerInteraction

    private lateinit var SUT: InteractionRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        SUT = InteractionRepositoryImpl(apiClient, databaseManagerInteraction)
    }

    @Test
    fun givenValidInteraction_whenInteractionSent_thenSaveInteraction() {
        // Given
        val interaction = Interaction(INTERACTION_STATUS, CURRENT_TIMESTAMP, TOKEN)
        val dbInteraction = interaction.toDb(INTERACTION_ID)

        // When
        SUT.saveInteraction(INTERACTION_ID, interaction)

        // Then
        verify(exactly = 1) { databaseManagerInteraction.insertInteraction(dbInteraction) }
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
        every { databaseManagerInteraction.getInteractions(any()) } returns listOf(dbInteraction) andThen emptyList()

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
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerInteraction.deleteInteractions(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returns listOf(dbInteraction)
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsNonRepeatable_thenTryPushNextInteraction() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerInteraction.getInteractions(1) }
        verify(exactly = 2) { databaseManagerInteraction.deleteInteractions(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoInteractionInDb_whenInteractionPush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { databaseManagerInteraction.getInteractions(any()) } returns emptyList()

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 0) { apiClient.put(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

    }

    @Test
    fun noOutdatedInteraction_whenClearOldInteractions_thenSentNothing() {
        // Given
        every { databaseManagerInteraction.deleteInteractionByTime(any()) } returns 0

        // When
        SUT.clearOldInteractions(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerInteraction.deleteInteractionByTime(any()) }
        verify(exactly = 0) { Logger.captureEvent(any()) }
    }

    @Test
    fun thereAreOutdatedInteraction_whenClearOldInteractions_thenSentCountDeleted() {
        // Given
        val deletedInteractions = 2
        every { databaseManagerInteraction.deleteInteractionByTime(any()) } returns deletedInteractions
        val expectedMsg = "Outdated Interactions: - $deletedInteractions"

        // When
        SUT.clearOldInteractions(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerInteraction.deleteInteractionByTime(any()) }
        verify(exactly = 1) { Logger.captureEvent(eq(expectedMsg)) }
    }
}