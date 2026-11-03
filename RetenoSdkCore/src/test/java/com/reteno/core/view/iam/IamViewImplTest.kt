package com.reteno.core.view.iam

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import android.app.Activity
import com.google.gson.JsonObject
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.view.iam.container.IamContainer
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import java.time.Instant
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class IamViewImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var inAppMessage: InAppMessage

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
    @Test
    fun workflow_initialization_waits_for_native_visibility_and_records_server_iid_once() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkObject(IamContainer.Companion)
        val container = mockk<IamContainer>(relaxed = true)
        val bridge = slot<RetenoAndroidHandler>()
        val visible = slot<() -> Unit>()
        val lifecycle = slot<RetenoLifecycleCallbacks>()
        val activity = mockk<Activity>(relaxed = true)
        val html = MutableStateFlow<ResultDomain<IamFetchResult>>(ResultDomain.Idle)
        var view: IamViewImpl? = null
        try {
            createRetenoAndAdvanceInit()
            every { iamController.fullHtmlStateFlow } returns html
            every { iamController.canPresentWorkflow(any()) } returns true
            every { iamController.canResumeWorkflow(any()) } returns true
            every { activityHelper.canPresentMessages() } returns true
            every { activityHelper.isActivityFullyReady() } returns true
            every { activityHelper.currentActivity } returns activity
            every { activityHelper.registerActivityLifecycleCallbacks(any(), capture(lifecycle)) } just Runs
            every { IamContainer.create(any(), any(), capture(bridge), any()) } returns container
            every { container.show(any(), capture(visible)) } just Runs
            every { container.show(any()) } just Runs
            val message = workflowMessage()
            view = IamViewImpl(activityHelper, iamController, interactionController, scheduleController)
            view.start()
            view.initialize(message)
            runCurrent()
            html.value = ResultDomain.Success(IamFetchResult("render", "html",
                InAppMessageContent.InAppLayoutType.FULL,
                InAppMessageContent.InAppLayoutParams(InAppMessageContent.InAppLayoutParams.Position.TOP)))
            runCurrent()
            bridge.captured.onMessagePosted("""{"type":"WIDGET_INIT_SUCCESS"}""")
            runCurrent()
            verify(exactly = 0) { interactionController.onInAppInteraction(any()) }
            visible.captured()
            visible.captured()
            verify(exactly = 1) { interactionController.onInAppInteraction(match {
                it.interactionId == message.iid && it.workflowContext == "owner" &&
                    it.status == InAppInteractionStatus.OPENED
            }) }
            verify(exactly = 0) { iamController.updateInAppMessage(any()) }
            lifecycle.captured.stop(activity)
            every { iamController.canPresentWorkflow(any()) } returns false
            lifecycle.captured.start(activity)
            runCurrent()
            verify(exactly = 1) { container.dismiss() }
            verify(exactly = 1) { container.show(activity) }
            verify(exactly = 1) { interactionController.onInAppInteraction(match {
                it.status == InAppInteractionStatus.OPENED
            }) }
            bridge.captured.onMessagePosted(
                """{"type":"OPEN_URL","payload":{"targetComponentId":"order-details","url":"https://example.com/orders/123"}}"""
            )
            verify(exactly = 1) { interactionController.onInAppInteraction(match {
                it.interactionId == message.iid && it.status == InAppInteractionStatus.CLICKED &&
                    it.messageInstanceId == message.messageInstanceId && it.workflowContext == "owner" &&
                    it.action?.type == "OPEN_URL" && it.action?.targetComponentId == "order-details" &&
                    it.action?.url == "https://example.com/orders/123"
            }) }
            coVerify(exactly = 0) { interactionController.onInteractionClickAction(any(), any()) }
            runCurrent()
            assertEquals(false, view.isViewShown())
            every { iamController.canPresentWorkflow(any()) } returns true
            val nextContent = InAppMessageContent(8002, InAppMessageContent.InAppLayoutType.FULL, null, JsonObject())
            val next = workflowMessage().copy(
                iid = "22222222-2222-4222-8222-222222222222",
                messageInstanceId = 8002,
                content = nextContent
            )
            view.initialize(next)
            runCurrent()
            verify(exactly = 1) { iamController.fetchIamFullHtml(nextContent) }
        } finally {
            view?.pause()
            unmockkObject(IamContainer.Companion)
            RetenoInternalImpl.swapInstance(null)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun workflow_background_view_is_destroyed_on_identity_change_and_next_identity_can_present() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkObject(IamContainer.Companion)
        val container = mockk<IamContainer>(relaxed = true)
        val bridge = slot<RetenoAndroidHandler>()
        val visible = slot<() -> Unit>()
        val lifecycle = slot<RetenoLifecycleCallbacks>()
        val activity = mockk<Activity>(relaxed = true)
        val html = MutableStateFlow<ResultDomain<IamFetchResult>>(ResultDomain.Idle)
        var currentOwner = "owner"
        var view: IamViewImpl? = null
        try {
            createRetenoAndAdvanceInit()
            every { iamController.fullHtmlStateFlow } returns html
            every { iamController.canPresentWorkflow(any()) } returns true
            every { iamController.canResumeWorkflow(any()) } answers {
                firstArg<InAppMessage>().workflowContext == currentOwner
            }
            every { activityHelper.canPresentMessages() } returns true
            every { activityHelper.isActivityFullyReady() } returns true
            every { activityHelper.currentActivity } returns activity
            every { activityHelper.registerActivityLifecycleCallbacks(any(), capture(lifecycle)) } just Runs
            every { IamContainer.create(any(), any(), capture(bridge), any()) } returns container
            every { container.show(any(), capture(visible)) } just Runs
            val message = workflowMessage()
            view = IamViewImpl(activityHelper, iamController, interactionController, scheduleController)
            view.start()
            view.initialize(message)
            runCurrent()
            html.value = ResultDomain.Success(IamFetchResult("render", "html",
                InAppMessageContent.InAppLayoutType.FULL,
                InAppMessageContent.InAppLayoutParams(InAppMessageContent.InAppLayoutParams.Position.TOP)))
            runCurrent()
            bridge.captured.onMessagePosted("""{"type":"WIDGET_INIT_SUCCESS"}""")
            runCurrent()
            visible.captured()
            lifecycle.captured.stop(activity)
            currentOwner = "other-owner"
            lifecycle.captured.start(activity)
            runCurrent()
            verify(exactly = 0) { container.show(any()) }
            verify(exactly = 1) { container.destroy() }
            verify(exactly = 1) { interactionController.onInAppInteraction(match {
                it.interactionId == message.iid && it.status == InAppInteractionStatus.OPENED
            }) }
            assertEquals(false, view.isViewShown())
            val nextContent = InAppMessageContent(8002, InAppMessageContent.InAppLayoutType.FULL, null, JsonObject())
            view.initialize(workflowMessage().copy(
                iid = "22222222-2222-4222-8222-222222222222",
                messageInstanceId = 8002,
                workflowContext = currentOwner,
                content = nextContent
            ))
            runCurrent()
            verify(exactly = 1) { iamController.fetchIamFullHtml(nextContent) }
        } finally {
            view?.pause()
            unmockkObject(IamContainer.Companion)
            RetenoInternalImpl.swapInstance(null)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun workflow_initialization_failure_preserves_iid_without_an_opened_observation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkObject(IamContainer.Companion)
        val bridge = slot<RetenoAndroidHandler>()
        val html = MutableStateFlow<ResultDomain<IamFetchResult>>(ResultDomain.Idle)
        var view: IamViewImpl? = null
        try {
            createRetenoAndAdvanceInit()
            every { iamController.fullHtmlStateFlow } returns html
            every { iamController.canPresentWorkflow(any()) } returns true
            every { IamContainer.create(any(), any(), capture(bridge), any()) } returns mockk(relaxed = true)
            val message = workflowMessage()
            view = IamViewImpl(activityHelper, iamController, interactionController, scheduleController)
            view.start()
            view.initialize(message)
            runCurrent()
            html.value = ResultDomain.Success(IamFetchResult("render", "html",
                InAppMessageContent.InAppLayoutType.FULL,
                InAppMessageContent.InAppLayoutParams(InAppMessageContent.InAppLayoutParams.Position.TOP)))
            runCurrent()
            bridge.captured.onMessagePosted("""{"type":"WIDGET_INIT_FAILED","payload":{"reason":"initialization failed"}}""")
            runCurrent()
            verify(exactly = 1) { interactionController.onInAppInteraction(match {
                it.interactionId == message.iid && it.status == InAppInteractionStatus.FAILED && it.workflowContext == "owner"
            }) }
            verify(exactly = 0) { interactionController.onInAppInteraction(match { it.status == InAppInteractionStatus.OPENED }) }
        } finally {
            view?.pause()
            unmockkObject(IamContainer.Companion)
            RetenoInternalImpl.swapInstance(null)
            Dispatchers.resetMain()
        }
    }

    private fun workflowMessage() = InAppMessage(7, 8001, JsonObject(), DisplayRules(null, null, null, null),
        inWorkflow = true, iid = "11111111-1111-4111-8111-111111111111",
        expiresAt = Instant.now().plusSeconds(3600).toString(), workflowContext = "owner")

}