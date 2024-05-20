package com.reteno.core.view.iam

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.MutableSharedFlow
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
    @RelaxedMockK
    private lateinit var interactionController: InteractionController
    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    private lateinit var SUT: IamView
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        every { iamController.inAppMessagesFlow } returns MutableSharedFlow()
        SUT = IamViewImpl(activityHelper, iamController, interactionController, scheduleController)
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