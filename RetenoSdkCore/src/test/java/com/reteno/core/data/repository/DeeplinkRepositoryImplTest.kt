package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test


class DeeplinkRepositoryImplTest : BaseUnitTest() {

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectOperationQueue()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectOperationQueue()
        }
    }

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
        // Given
        val deeplinkWrapped = "https://wrapped.com"

        // When
        deeplinkRepository.triggerWrappedLinkClicked(deeplinkWrapped)

        // Then
        verify(exactly = 1) {
            apiClient.get(ApiContract.Custom(deeplinkWrapped), null, any())
        }
    }
}