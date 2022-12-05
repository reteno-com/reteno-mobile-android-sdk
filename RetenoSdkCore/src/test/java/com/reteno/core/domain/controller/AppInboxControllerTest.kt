package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Test
import java.time.ZonedDateTime

class AppInboxControllerTest : BaseUnitTest() {
    
    private companion object {
        private const val PAGE = 2
        private const val PAGE_SIZE = 12
        private const val MESSAGE_ID = "dsdg-4352-sdgsdg-3525-sdggse"
    }

    @RelaxedMockK
    private lateinit var appInboxRepository: AppInboxRepository

    private lateinit var inbox: AppInboxController

    override fun before() {
        super.before()
        inbox = AppInboxController(appInboxRepository)
    }

    @Test
    fun whenGetAppInboxMessages_thenCallAppInboxRepository() {
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        inbox.getAppInboxMessages(PAGE, PAGE_SIZE, callback)

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
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        inbox.getAppInboxMessages(null, null, callback)

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
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.getMessagesCount(callback)

        verify(exactly = 1) {
            appInboxRepository.getMessagesCount(callback)
        }
    }

    @Test
    fun whenSubscribeOnMessagesCountChanged_thenCallAppInboxRepository() {
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.subscribeCountChanges(callback)

        verify(exactly = 1) {
            appInboxRepository.subscribeOnMessagesCountChanged(callback)
        }
    }

    @Test
    fun whenUnsubscribeMessagesCountChanged_thenCallAppInboxRepository() {
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.unsubscribeCountChanges(callback)

        verify(exactly = 1) {
            appInboxRepository.unsubscribeMessagesCountChanged(callback)
        }
    }

    @Test
    fun whenUnsubscribeAllMessagesCountChanged_thenCallAppInboxRepository() {
        inbox.unsubscribeAllCountChanges()

        verify(exactly = 1) {
            appInboxRepository.unsubscribeAllMessagesCountChanged()
        }
    }

    @Test
    fun whenMarkAsOpened_thenCallAppInboxRepository() {
        inbox.markAsOpened(MESSAGE_ID)

        verify(exactly = 1) {
            appInboxRepository.saveMessageOpened(eq(MESSAGE_ID))
        }
    }

    @Test
    fun whenMarkAllMessagesAsOpened_thenCallAppInboxRepository() {
        val callback = mockk<RetenoResultCallback<Unit>>()

        inbox.markAllMessagesAsOpened(callback)

        verify(exactly = 1) {
            appInboxRepository.setAllMessageOpened(callback)
        }
    }

    @Test
    fun whenPushAppInboxMessagesStatus_thenCallAppInboxRepository() {
        inbox.pushAppInboxMessagesStatus()

        verify(exactly = 1) {
            appInboxRepository.pushMessagesStatus()
        }
    }

    @Test
    fun whenClearOldMessagesStatus_thenRepositoryCalledWithOutdatedDate() {
        mockkObject(SchedulerUtils)
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        inbox.clearOldMessagesStatus()

        verify(exactly = 1) { appInboxRepository.clearOldMessages(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }

        unmockkObject(SchedulerUtils)
    }
}