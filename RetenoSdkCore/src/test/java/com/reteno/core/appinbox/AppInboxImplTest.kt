package com.reteno.core.appinbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AppInboxImplTest {

    private companion object {
        private const val PAGE = 2
        private const val PAGE_SIZE = 12
        private const val MESSAGE_ID = "dsdg-4352-sdgsdg-3525-sdggse"
    }

    @RelaxedMockK
    private lateinit var appInboxController: AppInboxController


    private lateinit var inbox: AppInboxImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        inbox = AppInboxImpl(appInboxController)
    }

    @Test
    fun whenGetAppInboxMessages_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        inbox.getAppInboxMessages(PAGE, PAGE_SIZE, callback)

        verify(exactly = 1) {
            appInboxController.getAppInboxMessages(
                eq(PAGE),
                eq(PAGE_SIZE),
                callback
            )
        }
    }

    @Test
    fun givenPageAndSizeAreNull_whenGetAppInboxMessages_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        inbox.getAppInboxMessages(null, null, callback)

        verify(exactly = 1) {
            appInboxController.getAppInboxMessages(
                null,
                null,
                callback
            )
        }
    }

    @Test
    fun whenGetAppInboxMessagesCount_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.getAppInboxMessagesCount(callback)

        verify(exactly = 1) {
            appInboxController.getMessagesCount(callback)
        }
    }

    @Test
    fun whenSubscribeOnMessagesCountChanged_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.subscribeOnMessagesCountChanged(callback)

        verify(exactly = 1) {
            appInboxController.subscribeCountChanges(callback)
        }
    }

    @Test
    fun whenUnsubscribeMessagesCountChanged_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<Int>>()

        inbox.unsubscribeMessagesCountChanged(callback)

        verify(exactly = 1) {
            appInboxController.unsubscribeCountChanges(callback)
        }
    }

    @Test
    fun whenUnsubscribeAllMessagesCountChanged_thenCallAppInboxController() {
        inbox.unsubscribeAllMessagesCountChanged()

        verify(exactly = 1) {
            appInboxController.unsubscribeAllCountChanges()
        }
    }

    @Test
    fun whenMarkAsOpened_thenCallAppInboxController() {
        inbox.markAsOpened(MESSAGE_ID)

        verify(exactly = 1) {
            appInboxController.markAsOpened(eq(MESSAGE_ID))
        }
    }

    @Test
    fun whenMarkAllMessagesAsOpened_thenCallAppInboxController() {
        val callback = mockk<RetenoResultCallback<Unit>>()

        inbox.markAllMessagesAsOpened(callback)

        verify(exactly = 1) {
            appInboxController.markAllMessagesAsOpened(callback)
        }
    }
}