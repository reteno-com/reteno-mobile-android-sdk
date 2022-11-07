package com.reteno.push.interceptor.click

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_LINK_WRAPPED
import com.reteno.push.Util
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationClickedReceiverTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var receiver: RetenoNotificationClickedReceiver? = null

    @RelaxedMockK
    private lateinit var context: Context
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    fun setUp() {
        receiver = RetenoNotificationClickedReceiver()
        mockkObject(Util)
    }

    @After
    fun tearDown() {
        unmockkObject(Util)
        receiver = null
    }

    @Test
    fun sendToCustomReceiver_extrasIsNotNull() {
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
    fun launchIntent_doNotHaveDeepLink() {
        mockkObject(RetenoImpl)

        val application = mockk<Application>()
        val mockIntent = Intent()

        every { context.packageManager.getLaunchIntentForPackage(any()) } returns mockIntent
        every { context.packageName } returns "com.reteno.example"
        every { application.applicationContext } returns context
        every { RetenoImpl.application } returns application

        receiver!!.onReceive(context, Intent())

        verify { context.startActivity(any()) }

        unmockkObject(RetenoImpl)
    }

    @Test
    fun doNotLaunchIntent_doNotFindLaunchIntentAndDoNotHaveDeepLink() {
        mockkObject(RetenoImpl)

        val application = mockk<Application>()

        every { context.packageManager.getLaunchIntentForPackage(any()) } returns null
        every { context.packageName } returns "com.reteno.example"
        every { application.applicationContext } returns context
        every { RetenoImpl.application } returns application

        receiver!!.onReceive(context, Intent())

        verify(exactly = 0) { context.startActivity(any()) }

        unmockkObject(RetenoImpl)
    }

    @Test
    fun launchDeepLink_whenExist() {
        val intentSlot = slot<Intent>()
        val deepLink = "com.reteno.example"

        val extra = Bundle().apply { putString("key", "value") }
        val intent = Intent()
        intent.putExtras(extra)
        intent.putExtra(Constants.KEY_ES_LINK_WRAPPED, deepLink)
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
    fun saveInteraction() {
        val interactionId = "interaction_id"
        val extra = Bundle().apply { putString(Constants.KEY_ES_INTERACTION_ID, interactionId) }
        val intent = Intent()
        intent.putExtras(extra)

        mockkObject(RetenoImpl)
        val application = mockk<Application>(moreInterfaces = arrayOf(RetenoApplication::class))
        val reteno = mockk<RetenoImpl>()
        val interactionController = mockk<InteractionController>()

        every { reteno.serviceLocator.interactionControllerProvider.get() } returns interactionController
        every { (application as RetenoApplication).getRetenoInstance() } returns reteno
        every { RetenoImpl.application } returns application
        justRun { interactionController.onInteraction(any(), any()) }

        receiver!!.onReceive(context, intent)

        verify { interactionController.onInteraction(eq(interactionId), InteractionStatus.OPENED) }
        unmockkObject(RetenoImpl)
    }

    @Test
    fun givenPushWithCustomDataNoDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() {
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
    fun givenPushWithCustomDataDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() {
        // Given
        val intentSlot = slot<Intent>()
        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent().apply {
            putExtras(extra)
            putExtra(KEY_ES_LINK_WRAPPED, DEEPLINK_WRAPPED)
            putExtra(KEY_ES_LINK_UNWRAPPED,DEEPLINK_UNWRAPPED)
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
}