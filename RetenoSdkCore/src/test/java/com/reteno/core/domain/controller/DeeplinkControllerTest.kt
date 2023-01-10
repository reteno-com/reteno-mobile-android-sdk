package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.DeeplinkRepository
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class DeeplinkControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var deeplinkRepository: DeeplinkRepository

    private lateinit var deeplinkController: DeeplinkController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        deeplinkController = DeeplinkController(deeplinkRepository)
    }

    @Test
    fun givenWrappedDeeplink_whenTriggerWrappedLink_thenWrappedDeeplinkClickedEventSentToRepository() {
        // When
        deeplinkController.triggerDeeplinkClicked(DEEPLINK_WRAPPED, DEEPLINK_UNWRAPPED)

        // Then
        verify(exactly = 1) { deeplinkRepository.triggerWrappedLinkClicked(DEEPLINK_WRAPPED) }
    }
}