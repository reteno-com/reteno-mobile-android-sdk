package com.reteno.push.receiver

import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test


class NotificationsEnabledManagerTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectRetenoNotificationsChannel()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectRetenoNotificationsChannel()
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT = NotificationsEnabledManager


    override fun before() {
        super.before()

        every { reteno.serviceLocator.contactControllerProvider.get() } returns contactController
        every { reteno.serviceLocator.scheduleControllerProvider.get() } returns scheduleController
    }

    @Test
    fun whenOnCheckState_thenScheduleControllerStart() {
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns true
        justRun { contactController.notificationsEnabled(any()) }

        // When
        SUT.onCheckState(application)

        // Then
        verify(exactly = 1) { scheduleController.startScheduler() }
    }

    @Test
    fun givenNotificationsDisabledChannelDisabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() {
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns false
        justRun { scheduleController.startScheduler() }

        // When
        SUT.onCheckState(application)

        // Then
        verify(exactly = 1) { contactController.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsDisabledChannelEnabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() {
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns true
        justRun { scheduleController.startScheduler() }

        // When
        SUT.onCheckState(application)

        // Then
        verify(exactly = 1) { contactController.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsEnabledChannelDisabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() {
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns false
        justRun { scheduleController.startScheduler() }

        // When
        SUT.onCheckState(application)

        // Then
        verify(exactly = 1) { contactController.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsEnabledChannelEnabled_whenOnCheckState_thenContactControllerNotificationsEnabledTrue() {
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns true
        justRun { scheduleController.startScheduler() }

        // When
        SUT.onCheckState(application)

        // Then
        verify(exactly = 1) { contactController.notificationsEnabled(true) }
    }
}