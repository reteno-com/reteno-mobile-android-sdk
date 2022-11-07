package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.DeeplinkRepository
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class DeeplinkControllerTest : BaseUnitTest() {

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
    fun givenWrappedDeeplink_whenTriggerWrappedLink_thenWrappedDeeplinkClickedEventSentToRepository(){
        //Given
        val deeplinkWrapped = "https://wrapped.com"
        val deeplinkUnwrapped = "https://unwrapped.com"

        // When
        deeplinkController.triggerDeeplinkClicked(deeplinkWrapped, deeplinkUnwrapped)

        // Then
        verify(exactly = 1) { deeplinkRepository.triggerWrappedLinkClicked(deeplinkWrapped) }
    }
}