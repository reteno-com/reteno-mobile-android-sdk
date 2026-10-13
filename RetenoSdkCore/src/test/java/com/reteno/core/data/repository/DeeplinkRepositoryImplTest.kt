package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.api.ApiClient
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


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
    private lateinit var databaseManagerWrappedLink: RetenoDatabaseManagerWrappedLink

    private lateinit var deeplinkRepository: DeeplinkRepository
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        deeplinkRepository = DeeplinkRepositoryImpl(databaseManagerWrappedLink) {}
    }

    @Test
    fun givenWrappedLinkNotBlank_whenSaveWrappedLink_thenWrappedLinkSavedToDatabase() {
        // When
        deeplinkRepository.saveWrappedLink(DEEPLINK_WRAPPED)

        // Then
        verify(exactly = 1) { OperationQueue.addParallelOperation(any()) }
        verify(exactly = 1) { databaseManagerWrappedLink.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

    @Test
    fun givenWrappedLinkIsEmpty_whenSaveWrappedLink_thenNothingHappens() {
        // When
        deeplinkRepository.saveWrappedLink("")

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { databaseManagerWrappedLink.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

    @Test
    fun givenWrappedLinkIsBlank_whenSaveWrappedLink_thenNothingHappens() {
        // When
        deeplinkRepository.saveWrappedLink(" ")

        // Then
        verify(exactly = 0) { OperationQueue.addOperation(any()) }
        verify(exactly = 0) { databaseManagerWrappedLink.insertWrappedLink(DEEPLINK_WRAPPED) }
    }

}