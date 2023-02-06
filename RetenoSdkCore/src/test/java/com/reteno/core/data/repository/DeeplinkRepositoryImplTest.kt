package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.api.ApiClient
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class DeeplinkRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        const val DEEPLINK_WRAPPED = "https://wrapped.com"
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
    fun givenWrappedLink_whenSaveWrappedLink_thenWrappedLinkSavedToDatabase() {
        // When
        deeplinkRepository.saveWrappedLink(DEEPLINK_WRAPPED)

        // Then
        verify(exactly = 1) {
            databaseManager.insertWrappedLink(DEEPLINK_WRAPPED)
        }
    }
}