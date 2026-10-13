package com.reteno.push.receiver

import com.reteno.core.di.ServiceLocator
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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

    // endregion helper fields ---------------------------------------------------------------------

    private var SUT = NotificationsEnabledManager


    override fun before() {
        super.before()

    }

    @Test
    fun givenNotificationsDisabledChannelDisabled_whenOnCheckState_thenSdkNotifiedNotificationsEnabledFalse() = runRetenoTest { reteno ->
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns false

        // When
        SUT.onCheckState(application)
        advanceUntilIdle()
        // Then
        verify(exactly = 1) { reteno.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsDisabledChannelEnabled_whenOnCheckState_thenSdkNotifiedNotificationsEnabledFalse() = runRetenoTest { reteno ->
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns false
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns true

        // When
        SUT.onCheckState(application)
        advanceUntilIdle()
        // Then
        verify(exactly = 1) { reteno.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsEnabledChannelDisabled_whenOnCheckState_thenSdkNotifiedNotificationsEnabledFalse() = runRetenoTest { reteno ->
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns false

        // When
        SUT.onCheckState(application)

        advanceUntilIdle()

        // Then
        verify(exactly = 1) { reteno.notificationsEnabled(false) }
    }

    @Test
    fun givenNotificationsEnabledChannelEnabled_whenOnCheckState_thenSdkNotifiedNotificationsEnabledTrue() = runRetenoTest { reteno ->
        // Given
        every { RetenoNotificationChannel.isNotificationsEnabled(any()) } returns true
        every { RetenoNotificationChannel.isNotificationChannelEnabled(any(), any()) } returns true

        // When
        SUT.onCheckState(application)
        advanceUntilIdle()
        // Then
        verify(exactly = 1) { reteno.notificationsEnabled(true) }
    }
}