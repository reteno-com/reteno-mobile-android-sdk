package com.reteno.core.view.iam

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.controller.IamController
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test


class IamViewImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper
    @RelaxedMockK
    private lateinit var iamController: IamController
    @RelaxedMockK
    private lateinit var inAppMessage: InAppMessage

    private lateinit var SUT: IamView
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = IamViewImpl(activityHelper, iamController)
    }

    @Test
    fun given_whenInitializeWithId_thenControllerFetchIamFullHtmlTriggered() {
        // When
        SUT.initialize(WIDGET_ID)

        // Then
        //verify(exactly = 1) { iamController.reset() }
        //verify(exactly = 1) { iamController.fetchIamFullHtml(eq(WIDGET_ID)) }
    }

    @Test
    fun given_whenInitializeWithMessage_thenControllerFetchIamFullHtmlTriggered() {
        // When
        SUT.initialize(inAppMessage)

        // Then
        //verify(exactly = 1) { iamController.reset() }
        //verify(exactly = 1) { iamController.fetchIamFullHtml(inAppMessage.content) }
    }
}