package com.reteno.push.interceptor.click

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.view.iam.IamView
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_ES_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_LINK_WRAPPED
import com.reteno.push.Util
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [26])
class RetenoNotificationClickedReceiverTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic("com.reteno.core.util.UtilKt")
            mockkConstructor(ServiceLocator::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic("com.reteno.core.util.UtilKt")
            unmockkConstructor(ServiceLocator::class)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var receiver: RetenoNotificationClickedReceiver? = null

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var interactionController: InteractionController

    @RelaxedMockK
    private lateinit var deeplinkController: DeeplinkController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        every { anyConstructed<ServiceLocator>().interactionControllerProvider.get() } returns interactionController
        every { anyConstructed<ServiceLocator>().deeplinkControllerProvider.get() } returns deeplinkController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        receiver = RetenoNotificationClickedReceiver()
    }

    override fun after() {
        super.after()
        receiver = null
    }

    @Test
    fun sendToCustomReceiver_extrasIsNotNull() = runRetenoTest {
        val extra = Bundle().apply { putString("key", "value") }
        val intent = Intent()
        intent.putExtras(extra)

        receiver!!.onReceive(context, intent)

        verify { Util.tryToSendToCustomReceiverNotificationClicked(any()) }
    }

    @Test
    fun doNotSendToCustomReceiver_extrasIsNull() {
        val intent = Intent()

        receiver!!.onReceive(context, intent)

        verify(exactly = 0) { Util.tryToSendToCustomReceiverNotificationClicked(any()) }
    }

    @Test
    fun launchIntent_doNotHaveDeepLink() = runRetenoTest {

        receiver!!.onReceive(context, Intent())
        every { context.packageManager.getLaunchIntentForPackage(any()) } returns application.packageManager.getLaunchIntentForPackage(application.packageName)
        every { context.packageName } returns "com.reteno.example"
        verify { context.startActivity(any()) }

    }

    @Test
    fun doNotLaunchIntent_doNotFindLaunchIntentAndDoNotHaveDeepLink() {
        mockkObject(RetenoInternalImpl)

        val application = mockk<Application>()

        every { context.packageManager.getLaunchIntentForPackage(any()) } returns null
        every { context.packageName } returns "com.reteno.example"
        every { application.applicationContext } returns context
        every { RetenoInternalImpl.instance.application } returns application

        receiver!!.onReceive(context, Intent())

        verify(exactly = 0) { context.startActivity(any()) }

        unmockkObject(RetenoInternalImpl)
    }

    @Test
    fun launchDeepLink_whenExist() = runRetenoTest {
        val intentSlot = slot<Intent>()
        val deepLink = "com.reteno.example"

        val extra = Bundle().apply { putString("key", "value") }
        val intent = Intent()
        intent.putExtras(extra)
        intent.putExtra(Constants.KEY_ES_LINK_UNWRAPPED, deepLink)
        justRun { context.startActivity(capture(intentSlot)) }

        receiver!!.onReceive(context, intent)

        intentSlot.captured.apply {
            assertEquals(Intent.ACTION_VIEW, action)
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, flags)
            assertTrue(extras?.containsKey(extra.keySet().first()) == true)
            assertEquals(deepLink, data.toString())
        }
    }

    @Test
    fun saveInteraction() = runRetenoTest {

        val interactionId = "interaction_id"
        val extra = Bundle().apply { putString(Constants.KEY_ES_INTERACTION_ID, interactionId) }
        val intent = Intent()
        intent.putExtras(extra)

        coJustRun { interactionController.onInteraction(any(), any()) }

        receiver!!.onReceive(context, intent)
        advanceUntilIdle()
        coVerify { interactionController.onInteraction(eq(interactionId), InteractionStatus.CLICKED) }
        verify(exactly = 1) { scheduleController.forcePush() }

    }

    @Test
    fun givenPushWithCustomDataNoDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() = runRetenoTest {
        // Given
        mockkObject(IntentHandler.AppLaunchIntent)

        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val intentSlot = slot<Intent>()
        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent()
        intent.putExtras(extra)
        justRun { context.startActivity(capture(intentSlot)) }

        // When
        receiver!!.onReceive(context, intent)

        // Then
        intentSlot.captured.run {
            assertTrue(extras?.containsKey(customDataKey) ?: false)
            assertEquals(customDataValue, extras?.getString(customDataKey))
        }

        unmockkObject(IntentHandler.AppLaunchIntent)
    }

    @Test
    fun givenPushWithCustomDataDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() = runRetenoTest {
        // Given
        val intentSlot = slot<Intent>()
        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent().apply {
            putExtras(extra)
            putExtra(KEY_ES_LINK_WRAPPED, DEEPLINK_WRAPPED)
            putExtra(KEY_ES_LINK_UNWRAPPED, DEEPLINK_UNWRAPPED)
        }
        justRun { context.startActivity(capture(intentSlot)) }

        // When
        receiver!!.onReceive(context, intent)

        // Then
        intentSlot.captured.run {
            assertTrue(extras?.containsKey(customDataKey) ?: false)
            assertEquals(customDataValue, extras?.getString(customDataKey))
        }
    }

    @Test
    fun givenPushWithIam_whenNotificationClicked_thenIamViewInitializeCalled() = runTest {
        // Given
        val iamView = mockk<IamView>(relaxed = true)
        val controller = mockk<ContactController>(relaxed = true)
        val iamWidgetId = "123"

        val extra = Bundle().apply {
            putString(Constants.KEY_ES_IAM, "1")
            putString(KEY_ES_INTERACTION_ID, iamWidgetId)
        }
        val intent = Intent().apply { putExtras(extra) }
        justRun { context.startActivity(any()) }
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns intent
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns controller
        coEvery { controller.awaitDeviceId() } returns "temp"

         createReteno()

        justRun { context.startActivity(any()) }

        // When
        receiver!!.onReceive(context, intent)

        // Then
        verify { iamView.initialize(iamWidgetId) }
        RetenoInternalImpl.swapInstance(null)
    }
}