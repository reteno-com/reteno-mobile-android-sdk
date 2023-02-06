package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.domain.SchedulerUtils
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime


class DeeplinkControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkObject(SchedulerUtils)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkObject(SchedulerUtils)
        }
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
    fun givenWrappedLink_whenDeeplinkClicked_thenWrappedLinkSavedInRepositoryAndPushed() {
        // When
        deeplinkController.deeplinkClicked(DEEPLINK_WRAPPED, DEEPLINK_UNWRAPPED)

        // Then
        verify(exactly = 1) { deeplinkRepository.saveWrappedLink(DEEPLINK_WRAPPED) }
        verify(exactly = 1) { deeplinkRepository.pushWrappedLink() }
    }

    @Test
    fun givenWrappedLink_whenPushDeeplink_thenRepositoryPushWrappedDeeplinkCalled() {
        // When
        deeplinkController.pushDeeplink()

        // Then
        verify(exactly = 1) { deeplinkRepository.pushWrappedLink() }
    }

    @Test
    fun givenWrappedLink_whenClearOldDeeplinks_thenRepositoryClearOldWrappedLinksCalledWithOutdatedDate() {
        // Given
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        // When
        deeplinkController.clearOldDeeplinks()

        // Then
        verify(exactly = 1) { deeplinkRepository.clearOldWrappedLinks(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }
    }
}