package com.reteno.core.domain.controller

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.ZonedDateTime

class AppInboxControllerTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    private companion object {
        private const val PAGE = 2
        private const val PAGE_SIZE = 12
        private const val MESSAGE_ID = "dsdg-4352-sdgsdg-3525-sdggse"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var appInboxRepository: AppInboxRepository

    private lateinit var inbox: AppInboxController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        inbox = AppInboxController(appInboxRepository)
    }

    @Test
    fun whenGetAppInboxMessages_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        // When
        inbox.getAppInboxMessages(PAGE, PAGE_SIZE, callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.getMessages(
                eq(PAGE),
                eq(PAGE_SIZE),
                callback
            )
        }
    }

    @Test
    fun givenPageAndSizeAreNull_whenGetAppInboxMessages_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        // When
        inbox.getAppInboxMessages(null, null, callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.getMessages(
                null,
                null,
                callback
            )
        }
    }

    @Test
    fun whenGetAppInboxMessagesCount_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.getMessagesCount(callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.getMessagesCount(callback)
        }
    }

    @Test
    fun whenSubscribeOnMessagesCountChanged_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.subscribeCountChanges(callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.subscribeOnMessagesCountChanged(callback)
        }
    }

    @Test
    fun whenUnsubscribeMessagesCountChanged_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.unsubscribeCountChanges(callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.unsubscribeMessagesCountChanged(callback)
        }
    }

    @Test
    fun whenUnsubscribeAllMessagesCountChanged_thenCallAppInboxRepository() {
        // When
        inbox.unsubscribeAllCountChanges()

        // Then
        verify(exactly = 1) {
            appInboxRepository.unsubscribeAllMessagesCountChanged()
        }
    }

    @Test
    fun whenMarkAsOpened_thenCallAppInboxRepository() {
        // When
        inbox.markAsOpened(MESSAGE_ID)

        // Then
        verify(exactly = 1) {
            appInboxRepository.saveMessageOpened(eq(MESSAGE_ID))
        }
    }

    @Test
    fun whenMarkAllMessagesAsOpened_thenCallAppInboxRepository() {
        // Given
        val callback = mockk<RetenoResultCallback<Unit>>()

        // When
        inbox.markAllMessagesAsOpened(callback)

        // Then
        verify(exactly = 1) {
            appInboxRepository.setAllMessageOpened(callback)
        }
    }

    @Test
    fun whenPushAppInboxMessagesStatus_thenCallAppInboxRepository() {
        // When
        inbox.pushAppInboxMessagesStatus()

        // Then
        verify(exactly = 1) {
            appInboxRepository.pushMessagesStatus()
        }
    }

    @Test
    fun whenClearOldMessagesStatus_thenRepositoryCalledWithOutdatedDate() {
        // Given
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        // When
        inbox.clearOldMessagesStatus()

        // Then
        verify(exactly = 1) { appInboxRepository.clearOldMessages(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }
    }
}