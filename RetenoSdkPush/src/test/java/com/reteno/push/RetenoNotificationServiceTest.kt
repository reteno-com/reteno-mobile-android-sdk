package com.reteno.push

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.push.Constants.KEY_ES_CONTENT
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_IMAGE
import com.reteno.push.Constants.KEY_ES_TITLE
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.receiver.NotificationsEnabledManager
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationServiceTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEFAULT_CHANNEL_ID: String = "DEFAULT_CHANNEL_ID"
        private const val INTERACTION_ID: String = "interaction_id_1231_4321_9900_0011"
        private const val TOKEN: String = "4bf5c8e5-72d5-4b3c-81d6-85128928e296"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var pushService: RetenoNotificationService? = null

    @RelaxedMockK
    private lateinit var interactionController: InteractionController
    @RelaxedMockK
    private lateinit var contactController: ContactController
    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController
    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        every { reteno.serviceLocator.interactionControllerProvider.get() } returns interactionController
        every { reteno.serviceLocator.contactControllerProvider.get() } returns contactController
        every { reteno.serviceLocator.scheduleControllerProvider.get() } returns scheduleController
        every { reteno.serviceLocator.retenoActivityHelperProvider.get() } returns activityHelper
        justRun { activityHelper.registerActivityLifecycleCallbacks(any(), any()) }

        pushService = RetenoNotificationService()

        mockkObject(Util)
        justRun { Util.tryToSendToCustomReceiverPushReceived(any()) }
        justRun { Util.tryToSendToCustomReceiverNotificationClicked(any()) }
        mockkObject(RetenoNotificationChannel)
        mockkObject(NotificationsEnabledManager)
        justRun { NotificationsEnabledManager.onCheckState(any()) }
    }

    override fun after() {
        super.after()
        pushService = null

        unmockkObject(Util)
        unmockkObject(RetenoNotificationChannel)
        unmockkObject(NotificationsEnabledManager)
    }

    @Test
    @Throws(Exception::class)
    fun givenValidToken_whenOnNewToken_thenSavedToRepository() {
        // When
        pushService!!.onNewToken(TOKEN)

        // Then
        verify { contactController.onNewFcmToken(eq(TOKEN)) }
    }

    @Test
    @Throws(Exception::class)
    fun givenValidNotification_whenHandleRetenoNotification_thenNotificationShown() {
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        // When
        pushService!!.handleRetenoNotification(bundle)

        // Then
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(1, Shadows.shadowOf(notificationManager).size())
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationChannelEnabledAndPermissionsGranted_whenHandleRetenoNotification_thenDeliveredStatusSent() {
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }
        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns true
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true

        justRun { interactionController.onInteraction(any(), any()) }
        pushService = spyk(RetenoNotificationService())

        // When
        pushService!!.handleRetenoNotification(bundle)

        // Then
        verify(exactly = 1) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationChannelDisabled_whenHandleRetenoNotification_thenDeliveredStatusNotSent() {
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }
        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns false

        justRun { interactionController.onInteraction(any(), any()) }
        pushService = spyk(RetenoNotificationService())

        // When
        pushService!!.handleRetenoNotification(bundle)

        // Then
        verify(exactly = 0) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 0) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun givenNotificationPermissionsNotGranted_whenHandleRetenoNotification_thenDeliveredStatusNotSent() {
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false

        justRun { interactionController.onInteraction(any(), any()) }
        pushService = spyk(RetenoNotificationService())

        // When
        pushService!!.handleRetenoNotification(bundle)

        // Then
        verify(exactly = 0) { interactionController.onInteraction(eq(INTERACTION_ID), eq(InteractionStatus.DELIVERED)) }
        verify(exactly = 0) { scheduleController.forcePush() }
    }

    @Test
    @Throws(Exception::class)
    fun whenHandleRetenoNotification_thenNotificationsEnabledManagerOnCheckStateCalled() {
        // Given
        val bundle = buildBundle(INTERACTION_ID)

        justRun { RetenoNotificationChannel.createDefaultChannel(any()) }
        every { RetenoNotificationChannel.DEFAULT_CHANNEL_ID } returns DEFAULT_CHANNEL_ID
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), DEFAULT_CHANNEL_ID) } returns true
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true

        justRun { interactionController.onInteraction(any(), any()) }
        pushService = spyk(RetenoNotificationService())

        // When
        pushService!!.handleRetenoNotification(bundle)

        // Then
        verify(exactly = 1) { NotificationsEnabledManager.onCheckState(any()) }
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
    // endregion helper methods --------------------------------------------------------------------
}