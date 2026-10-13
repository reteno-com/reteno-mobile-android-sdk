package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.domain.SchedulerUtils
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.AfterClass
import org.junit.BeforeClass


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

}