package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
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

    private lateinit var deeplinkRepository: DeeplinkRepository
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        deeplinkRepository = DeeplinkRepositoryImpl(apiClient)
    }

    @Test
    fun givenWrappedDeeplink_whenTriggerWrappedLink_thenWrappedDeeplinkClickedEventSentToBackend() {
        // When
        deeplinkRepository.triggerWrappedLinkClicked(DEEPLINK_WRAPPED)

        // Then
        verify(exactly = 1) {
            apiClient.get(ApiContract.Custom(DEEPLINK_WRAPPED), null, any())
        }
    }
}