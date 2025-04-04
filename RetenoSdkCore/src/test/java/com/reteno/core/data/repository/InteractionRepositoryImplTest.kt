package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class InteractionRepositoryImplTest : BaseRobolectricTest() {

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

        private const val SERVER_ERROR_NON_REPEATABLE = 500
        private const val SERVER_ERROR_REPEATABLE = 400
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var databaseManagerInteraction: RetenoDatabaseManagerInteraction
    @RelaxedMockK
    private lateinit var databaseManagerInAppInteraction: RetenoDatabaseManagerInAppInteraction

    private lateinit var SUT: InteractionRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        SUT = InteractionRepositoryImpl(apiClient, databaseManagerInteraction, databaseManagerInAppInteraction)
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
    fun givenValidInteraction_whenInteractionPushSuccessfulAndCacheUpdated_thenTryPushNextInteraction() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { databaseManagerInteraction.deleteInteraction(dbInteraction) } returns true
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerInteraction.deleteInteraction(dbInteraction) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushSuccessfulAndCacheNotUpdated_thenNextOperation() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { databaseManagerInteraction.deleteInteraction(dbInteraction) } returns false
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 1) { databaseManagerInteraction.deleteInteraction(dbInteraction) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returns listOf(dbInteraction)
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(SERVER_ERROR_NON_REPEATABLE, null, null)
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsNonRepeatableAndCacheUpdated_thenTryPushNextInteraction() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { databaseManagerInteraction.deleteInteraction(dbInteraction) } returns true
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(SERVER_ERROR_REPEATABLE, null, null)
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 2) { apiClient.put(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerInteraction.getInteractions(1) }
        verify(exactly = 2) { databaseManagerInteraction.deleteInteraction(dbInteraction) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
        verify(exactly = 0) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidInteraction_whenInteractionPushFailedAndErrorIsNonRepeatableAndCacheNotUpdated_thenDeleteInteraction() {
        // Given
        val dbInteraction = mockk<InteractionDb>(relaxed = true)
        every { databaseManagerInteraction.getInteractions(any()) } returnsMany listOf(listOf(dbInteraction), listOf(dbInteraction), emptyList())
        every { databaseManagerInteraction.deleteInteraction(dbInteraction) } returns false
        every { apiClient.put(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        // When
        SUT.pushInteractions()

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }
        verify(exactly = 1) { databaseManagerInteraction.getInteractions(1) }
        verify(exactly = 1) { databaseManagerInteraction.deleteInteraction(dbInteraction) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
        verify(exactly = 0) { PushOperationQueue.removeAllOperations() }
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
        every { databaseManagerInteraction.deleteInteractionByTime(any()) } returns emptyList()

        // When
        SUT.clearOldInteractions(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerInteraction.deleteInteractionByTime(any()) }
        verify(exactly = 0) { Logger.captureMessage(any()) }
    }

    @Test
    fun thereAreOutdatedInteraction_whenClearOldInteractions_thenSentCountDeleted() = runRetenoTest {
        // Given
        val deletedInteractions = listOf<InteractionDb>(
            InteractionDb(
                interactionId = "1",
                status = InteractionStatusDb.CLICKED,
                time = "occurred1",
                token = "token"
            ),
            InteractionDb(
                interactionId = "2",
                status = InteractionStatusDb.OPENED,
                time = "occurred2",
                token = "token"
            )
        )
        every { databaseManagerInteraction.deleteInteractionByTime(any()) } returns deletedInteractions

        // When
        SUT.clearOldInteractions(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerInteraction.deleteInteractionByTime(any()) }
    }
}