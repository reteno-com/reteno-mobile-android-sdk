package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.ZonedDateTime


class DeeplinkRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_WRAPPED_2 = "https://wrapped2.com"

        private const val SERVER_ERROR_NON_REPEATABLE = 500
        private const val SERVER_ERROR_REPEATABLE = 400
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var databaseManager: RetenoDatabaseManagerWrappedLink

    private lateinit var deeplinkRepository: DeeplinkRepository
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        deeplinkRepository = DeeplinkRepositoryImpl(apiClient, databaseManager)
    }

    @Test
    fun givenWrappedLinkNotBlank_whenSaveWrappedLink_thenWrappedLinkSavedToDatabase() {
        // When
        deeplinkRepository.saveWrappedLink(DEEPLINK_WRAPPED)

        // Then
        verify(exactly = 1) { OperationQueue.addParallelOperation(any()) }
        verify(exactly = 1) { databaseManager.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

    @Test
    fun givenWrappedLinkIsEmpty_whenSaveWrappedLink_thenNothingHappens() {
        // When
        deeplinkRepository.saveWrappedLink("")

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { databaseManager.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

    @Test
    fun givenWrappedLinkIsBlank_whenSaveWrappedLink_thenNothingHappens() {
        // When
        deeplinkRepository.saveWrappedLink(" ")

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { databaseManager.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

    @Test
    fun givenNoWrappedLinksCachedInDatabase_whenPushWrappedLink_thenNothingHappensNextOperation() {
        // Given
        every { databaseManager.getWrappedLinks(any()) } returns emptyList<String>()

        // When
        deeplinkRepository.pushWrappedLink()

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { apiClient.head(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenWrappedLinkCachedInDatabase_whenPushWrappedLink_thenApiClientPutsWrappedLinkWithCorrectParameters() {
        // Given
        every { databaseManager.getWrappedLinks(any()) } returns listOf(DEEPLINK_WRAPPED) andThen emptyList<String>()

        val apiContractCaptured = slot<ApiContract>()
        every {
            apiClient.head(
                url = capture(apiContractCaptured),
                queryParams = any(),
                responseHandler = any()
            )
        } just runs

        // When
        deeplinkRepository.pushWrappedLink()

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
        verify { apiClient.head(any(), any(), any()) }
        assert(apiContractCaptured.captured is ApiContract.Custom)
        assertEquals(DEEPLINK_WRAPPED, apiContractCaptured.captured.url)
    }

    @Test
    fun givenWrappedLink_whenPushWrappedLinkSuccessful_thenTryPushNextWrappedLink() {
        // Given
        every { databaseManager.getWrappedLinks(any()) } returnsMany listOf(
            listOf(DEEPLINK_WRAPPED),
            listOf(DEEPLINK_WRAPPED_2),
            emptyList<String>()
        )

        every { apiClient.head(url = any(), queryParams = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        deeplinkRepository.pushWrappedLink()

        // Then
        verify(exactly = 3) { OperationQueue.addOperation(any()) }
        verify(exactly = 2) { apiClient.head(any(), any(), any()) }
        verify(exactly = 2) { databaseManager.deleteWrappedLinks(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
        verify(exactly = 0) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenWrappedLink_whenPushWrappedLinkFailedAndErrorIsNonRepeatable_thenCancelPushOperations() {
        // Given
        every { databaseManager.getWrappedLinks(any()) } returnsMany listOf(
            listOf(DEEPLINK_WRAPPED),
            emptyList<String>()
        )

        every { apiClient.head(url = any(), queryParams = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(SERVER_ERROR_NON_REPEATABLE, null, null)
        }

        // When
        deeplinkRepository.pushWrappedLink()

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) { apiClient.head(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenWrappedLink_whenPushWrappedLinkFailedAndErrorIsRepeatable_thenTryPushNextWrappedLink() {
        // Given
        every { databaseManager.getWrappedLinks(any()) } returnsMany listOf(
            listOf(DEEPLINK_WRAPPED),
            listOf(DEEPLINK_WRAPPED_2),
            emptyList<String>()
        )

        every { apiClient.head(url = any(), queryParams = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(SERVER_ERROR_REPEATABLE, null, null)
        }

        // When
        deeplinkRepository.pushWrappedLink()

        // Then
        verify(exactly = 3) { OperationQueue.addOperation(any()) }
        verify(exactly = 2) { apiClient.head(any(), any(), any()) }
        verify(exactly = 3) { databaseManager.getWrappedLinks(1) }
        verify(exactly = 2) { databaseManager.deleteWrappedLinks(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoOutdatedWrappedLinks_whenClearOldWrappedLinks_thenSentNothing() {
        // Given
        every { databaseManager.deleteWrappedLinksByTime(any()) } returns 0

        // When
        deeplinkRepository.clearOldWrappedLinks(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) { databaseManager.deleteWrappedLinksByTime(any()) }
    }

    @Test
    fun givenOutdatedWrappedLinksPresent_whenClearOldWrappedLinks_thenSentDeleteWrappedLinksDataToLogger() = runRetenoTest {
        // Given
        val deletedWrappedLinks = 2
        every { databaseManager.deleteWrappedLinksByTime(any()) } returns deletedWrappedLinks

        // When
        deeplinkRepository.clearOldWrappedLinks(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) { databaseManager.deleteWrappedLinksByTime(any()) }
    }
}