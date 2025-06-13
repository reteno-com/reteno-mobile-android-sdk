package com.reteno.push

import android.app.NotificationManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.util.getApplicationMetaData
import com.reteno.core.util.queryBroadcastReceivers
import com.reteno.push.Constants.KEY_ES_CONTENT
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_IMAGE
import com.reteno.push.Constants.KEY_ES_TITLE
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.receiver.NotificationsEnabledManager
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowNotificationManager

@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [26], shadows = [ShadowNotificationManager::class])
class RetenoNotificationServiceTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEFAULT_CHANNEL_ID: String = "DEFAULT_CHANNEL_ID"
        private const val INTERACTION_ID: String = "interaction_id_1231_4321_9900_0011"
        private const val TOKEN: String = "4bf5c8e5-72d5-4b3c-81d6-85128928e296"

        private const val TRANSCRIPT_CUSTOM_PUSH = "TRANSCRIPT_CUSTOM_PUSH"

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
    private lateinit var interactionController: InteractionController
    @RelaxedMockK
    private lateinit var contactController: ContactController
    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController
    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper

    private var contextWrapper: ContextWrapper? = null
    private val transcript: MutableList<String> = mutableListOf()
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        every { anyConstructed<ServiceLocator>().interactionControllerProvider.get() } returns interactionController
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns activityHelper
        justRun { activityHelper.registerActivityLifecycleCallbacks(any(), any()) }

        justRun { Util.tryToSendToCustomReceiverPushReceived(any()) }
        justRun { Util.tryToSendToCustomReceiverNotificationClicked(any()) }
        justRun { NotificationsEnabledManager.onCheckState(any()) }

        contextWrapper = ContextWrapper(application)
        TestCase.assertNotNull(contextWrapper)
        transcript.clear()
    }

    override fun after() {
        super.after()

        contextWrapper = null
        transcript.clear()
        unmockkObject(RetenoNotificationHelper)
    }

    @Test
    @Throws(Exception::class)
    fun givenValidToken_whenOnNewToken_thenSavedToRepository() = runTest {
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        val reteno = createReteno()
        // When
        val sut = RetenoNotificationService(application, reteno)
        sut.onNewToken(TOKEN)
        advanceUntilIdle()
        // Then
        coVerify { contactController.onNewFcmToken(eq(TOKEN)) }
    }

    @Test
    @Throws(Exception::class)
    fun givenValidNotification_whenHandleNotification_thenNotificationShown() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        // When
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        pushService.handleNotification(bundle)

        // Then
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(1, Shadows.shadowOf(notificationManager).size())
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationChannelEnabledAndPermissionsGranted_whenHandleNotification_thenDeliveredStatusSent() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns true
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }

        coJustRun { interactionController.onInteraction(any(), any()) }

        // When
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        pushService.handleNotification(bundle)
        advanceUntilIdle()
        // Then
        coVerify(exactly = 1) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationChannelDisabled_whenHandleNotification_thenDeliveredStatusNotSent() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }
        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns false

        coJustRun { interactionController.onInteraction(any(), any()) }

        // When
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        pushService.handleNotification(bundle)

        // Then
        coVerify(exactly = 0) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 0) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationPermissionsNotGranted_whenHandleNotification_thenDeliveredStatusNotSent() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false

        coJustRun { interactionController.onInteraction(any(), any()) }

        // When
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        pushService.handleNotification(bundle)

        // Then
        coVerify(exactly = 0) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 0) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun whenHandleNotification_thenNotificationsEnabledManagerOnCheckStateNotCalled() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }
        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns true
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true

        coJustRun { interactionController.onInteraction(any(), any()) }

        // When
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        pushService.handleNotification(bundle)

        // Then
        verify(exactly = 0) { NotificationsEnabledManager.onCheckState(any()) }
    }

    @Test
    fun givenEsInteractionIdPresent_whenOnMessageReceived_thenPushServiceHandleNotificationCalled() = runTest {
        val reteno = createReteno()
        // Given
        val bundle = buildBundle(INTERACTION_ID)
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        justRun { pushService.handleNotification(bundle) }
        every { application.getApplicationMetaData().getInt(any()) } returns 0
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        // When
        val pushServiceSpy = spyk<RetenoNotificationService>(recordPrivateCalls = true, objToCopy = pushService)
        pushServiceSpy.handleNotification(bundle)

        // Then
        verify(exactly = 1) { pushServiceSpy invoke "handleRetenoNotification" withArguments listOf(allAny<Bundle>()) }
    }

    @Test
    fun givenEsInteractionNotPresent_whenOnMessageReceived_thenBroadcastIsSent() = runTest {
        val reteno = createReteno()
        // Given
        val pushService = RetenoNotificationService(ApplicationProvider.getApplicationContext(), reteno)
        val bundle = Bundle()

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    transcript.add(TRANSCRIPT_CUSTOM_PUSH)
                }
            }
        contextWrapper!!.registerReceiver(
            receiver,
            IntentFilter(com.reteno.core.util.Constants.BROADCAST_ACTION_CUSTOM_PUSH)
        )
        mockQueryBroadcastReceivers()

        // When
        pushService.handleNotification(bundle)

        // Then
        ShadowLooper.shadowMainLooper().idle()
        TestCase.assertTrue(transcript.contains(TRANSCRIPT_CUSTOM_PUSH))
    }

    // region helper methods -----------------------------------------------------------------------
    private fun buildBundle(interactionId: String): Bundle {
        val bundle = Bundle().apply {
            putString(KEY_ES_INTERACTION_ID, interactionId)
            putString(KEY_ES_TITLE, "Title")
            putString(KEY_ES_CONTENT, "content")
            putString(
                KEY_ES_NOTIFICATION_IMAGE,
                "https://png.pngtree.com/png-clipart/20210425/original/pngtree-lying-down-a-friend-png-image_6248990.jpg"
            )
        }
        return bundle
    }

    private fun mockQueryBroadcastReceivers() {
        val mockResolveInfo = mockk<ResolveInfo>(relaxed = true).apply {
            activityInfo = ActivityInfo().apply {
                packageName = "packageName"
                name = "name"
            }
        }
        every { application.queryBroadcastReceivers(any()) } returns listOf(mockResolveInfo)
    }
    // endregion helper methods --------------------------------------------------------------------
}