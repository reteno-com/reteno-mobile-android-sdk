package com.reteno.core.view.inapp

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.controller.InAppMessagesController
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class InAppMessagesViewImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper
    @RelaxedMockK
    private lateinit var inAppMessagesController: InAppMessagesController

    private lateinit var SUT: InAppMessagesViewImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = InAppMessagesViewImpl(activityHelper, inAppMessagesController)
    }

    @Test
    fun given_whenInitialize_thenControllerFetchInAppMessagesFullHtmlTriggered() {
        // When
        SUT.initialize(WIDGET_ID)

        // Then
        verify(exactly = 1) { inAppMessagesController.reset() }
        verify(exactly = 1) { inAppMessagesController.fetchInAppMessagesFullHtml(eq(WIDGET_ID)) }
    }
}