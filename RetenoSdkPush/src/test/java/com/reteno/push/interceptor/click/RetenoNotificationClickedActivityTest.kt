package com.reteno.push.interceptor.click

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.view.iam.IamView
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_IAM
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
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
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric.buildActivity
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [26])
class RetenoNotificationClickedActivityTest : BaseRobolectricTest() {

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
    }

    @Test
    fun saveInteraction_extrasIsNotNull() = runRetenoTest {
        val interactionId = "interaction_id"

        val extra = Bundle().apply {
            putString(Constants.KEY_ES_INTERACTION_ID, interactionId)
        }
        val intent = Intent()
        intent.putExtras(extra)
        coJustRun { interactionController.onInteraction(any(), any()) }

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java, intent)
            .create()
            .get()

        advanceUntilIdle()
        coVerify { interactionController.onInteraction(eq(interactionId), InteractionStatus.CLICKED) }
        verify(exactly = 1) { scheduleController.forcePush() }
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
    fun launchApp_doNotHaveDeepLinkAndExtrasIsNull() = runRetenoTest {
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
    }

    @Test
    fun launchDeepLink() = runTest {
        val reteno = createReteno()
        val deepLink = "com.reteno.example"
        val intent = Intent()
        intent.putExtra(Constants.KEY_ES_LINK_UNWRAPPED, deepLink)

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
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val activity = buildActivity(RetenoNotificationClickedActivity::class.java).create().get()

        val shadow = shadowOf(activity)
        val intent = shadow.peekNextStartedActivity()

        assertNotNull(intent)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun givenPushWithCustomDataNoDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity()  = runTest{
        createReteno()
        // Given
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns Intent()

        val customDataKey = "customDataKey"
        val customDataValue = "customDataValue"

        val extra = Bundle().apply { putString(customDataKey, customDataValue) }
        val intent = Intent()
        intent.putExtras(extra)
        justRun { context.startActivity(any()) }

        // When
        val activity =
            buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        val shadow = shadowOf(activity)
        val appLaunchIntent = shadow.peekNextStartedActivity()

        // Then
        assertNotNull(appLaunchIntent)
        assertNotNull(appLaunchIntent.extras)
        assertTrue(appLaunchIntent.hasExtra(customDataKey))
        assertEquals(customDataValue, appLaunchIntent.getStringExtra(customDataKey))
    }

    @Test
    fun givenPushWithCustomDataDeeplinkReceived_whenNotificationClicked_thenCustomDataDeliveredToLaunchActivity() = runTest {
        val reteno = createReteno()
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
        val activity =
            buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        val shadow = shadowOf(activity)
        val appLaunchIntent = shadow.peekNextStartedActivity()

        // Then
        assertNotNull(appLaunchIntent)
        assertNotNull(appLaunchIntent.extras)
        assertTrue(appLaunchIntent.hasExtra(customDataKey))
        assertEquals(customDataValue, appLaunchIntent.getStringExtra(customDataKey))
    }

    @Test
    fun givenPushWithIam_whenNotificationClicked_thenIamViewInitializeCalled() = runTest {
        // Given
        val iamView = mockk<IamView>(relaxed = true)
        val controller = mockk<ContactController>(relaxed = true)
        val iamWidgetId = "123"

        val extra = Bundle().apply {
            putString(KEY_ES_INTERACTION_ID, iamWidgetId)
            putString(KEY_ES_IAM, "1")
        }
        val intent = Intent().apply { putExtras(extra) }
        justRun { context.startActivity(any()) }
        every { IntentHandler.AppLaunchIntent.getAppLaunchIntent(any()) } returns intent
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns controller
        coEvery { controller.awaitDeviceId() } returns "temp"

        createReteno()

        // When
        val activity =
            buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        activity.onCreate(extra, null)

        // Then
        verify { iamView.initialize(iamWidgetId) }
    }
}