package com.reteno.push.receiver

import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsEnabledManagerTest : BaseRobolectricTest() {

    companion object {
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

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT = NotificationsEnabledManager


    override fun before() {
        super.before()

        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
    }

    @Test
    fun whenOnCheckState_thenScheduleControllerStart() = runRetenoTest {
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
    fun givenNotificationsDisabledChannelDisabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() = runRetenoTest {
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
    fun givenNotificationsDisabledChannelEnabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() = runRetenoTest {
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
    fun givenNotificationsEnabledChannelDisabled_whenOnCheckState_thenContactControllerNotificationsEnabledFalse() = runRetenoTest {
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
    fun givenNotificationsEnabledChannelEnabled_whenOnCheckState_thenContactControllerNotificationsEnabledTrue() = runRetenoTest {
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