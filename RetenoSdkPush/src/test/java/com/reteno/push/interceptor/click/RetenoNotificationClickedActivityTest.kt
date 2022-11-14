package com.reteno.push.interceptor.click

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.push.Constants
import com.reteno.push.Util
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.Assert.assertNotNull
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric.buildActivity
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationClickedActivityTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var context: Context
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    fun setUp() {
        mockkObject(Util)
    }

    @After
    fun tearDown() {
        unmockkObject(Util)
    }

    @Test
    fun saveInteraction_extrasIsNotNull() {
        val interactionId = "interaction_id"

        val extra = Bundle().apply {
            putString(Constants.KEY_ES_INTERACTION_ID, interactionId)
        }
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

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent)
            .create()
            .get()

        verify { interactionController.onInteraction(eq(interactionId), InteractionStatus.OPENED) }
        unmockkObject(RetenoImpl)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun sendToCustomReceiver_extrasIsNotNull() {
        val extra = Bundle().apply { putString("key", "value") }
        val intent = Intent()
        intent.putExtras(extra)

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent)
            .create()
            .get()

        verify { Util.tryToSendToCustomReceiverNotificationClicked(any()) }
        assertTrue(activity.isFinishing)
    }

    @Test
    fun launchApp_doNotHaveDeepLinkAndExtrasIsNull() {
        mockkObject(IntentHandler.AppLaunchIntent)
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val extra = Bundle().apply { putString("key", "value") }
        val intent = Intent()
        intent.putExtras(extra)

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent)
            .create()
            .get()

        val shadow = shadowOf(activity)
        val shadowIntent = shadow.peekNextStartedActivity()

        assertNotNull(shadowIntent)
        assertTrue(activity.isFinishing)

        unmockkObject(IntentHandler.AppLaunchIntent)
    }

    @Test
    fun launchDeepLink() {
        val deepLink = "com.reteno.example"
        val intent = Intent()
        intent.putExtra(Constants.KEY_ES_LINK_WRAPPED, deepLink)

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent)
            .create()
            .get()

        val shadow = shadowOf(activity)
        val shadowIntent = shadow.peekNextStartedActivity()

        shadowIntent.apply {
            Assert.assertEquals(Intent.ACTION_VIEW, action)
            Assert.assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, flags)
            Assert.assertEquals(deepLink, data.toString())
        }
        assertTrue(activity.isFinishing)
    }

    @Test
    fun launchApp_extrasIsNull() {
        mockkObject(IntentHandler.AppLaunchIntent)
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java).create().get()

        val shadow = shadowOf(activity)
        val intent = shadow.peekNextStartedActivity()

        assertNotNull(intent)
        assertTrue(activity.isFinishing)

        unmockkObject(IntentHandler.AppLaunchIntent)
    }

    @Test
    fun givenPushWithCustomDataNoDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() {
        // Given
        mockkObject(IntentHandler.AppLaunchIntent)
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent()
        intent.putExtras(extra)
        justRun { context.startActivity(any()) }

        // When
        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        val shadow = shadowOf(activity)
        val appLaunchIntent = shadow.peekNextStartedActivity()

        // Then
        assertNotNull(appLaunchIntent)
        assertNotNull(appLaunchIntent.extras)
        assertTrue(appLaunchIntent.hasExtra(customDataKey))
        assertEquals(customDataValue, appLaunchIntent.getStringExtra(customDataKey))

        unmockkObject(IntentHandler.AppLaunchIntent)
    }

    @Test
    fun givenPushWithCustomDataDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() {
        // Given
        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent().apply {
            putExtras(extra)
            putExtra(
                Constants.KEY_ES_LINK_WRAPPED,
                DEEPLINK_WRAPPED
            )
            putExtra(
                Constants.KEY_ES_LINK_UNWRAPPED,
                DEEPLINK_UNWRAPPED
            )
        }
        justRun { context.startActivity(any()) }

        // When
        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        val shadow = shadowOf(activity)
        val appLaunchIntent = shadow.peekNextStartedActivity()

        // Then
        assertNotNull(appLaunchIntent)
        assertNotNull(appLaunchIntent.extras)
        assertTrue(appLaunchIntent.hasExtra(customDataKey))
        assertEquals(customDataValue, appLaunchIntent.getStringExtra(customDataKey))
    }
}