package com.reteno.core.appinbox

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxImpl
import com.reteno.core.features.appinbox.AppInboxStatus
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AppInboxImplTest: BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    private companion object {
        private const val PAGE = 2
        private const val PAGE_SIZE = 12
        private val STATUS = AppInboxStatus.OPENED
        private const val MESSAGE_ID = "dsdg-4352-sdgsdg-3525-sdggse"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var appInboxController: AppInboxController


    private lateinit var inbox: AppInboxImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        inbox = AppInboxImpl(appInboxController)
    }

    @Test
    fun whenGetAppInboxMessages_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        // When
        inbox.getAppInboxMessages(PAGE, PAGE_SIZE, STATUS, callback)

        // Then
        verify(exactly = 1) {
            appInboxController.getAppInboxMessages(
                eq(PAGE),
                eq(PAGE_SIZE),
                eq(STATUS),
                callback
            )
        }
    }

    @Test
    fun givenPageAndSizeAreNull_whenGetAppInboxMessages_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<AppInboxMessages>>()

        // When
        inbox.getAppInboxMessages(null, null, null, callback)

        // Then
        verify(exactly = 1) {
            appInboxController.getAppInboxMessages(
                null,
                null,
                null,
                callback
            )
        }
    }

    @Test
    fun whenGetAppInboxMessagesCount_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.getAppInboxMessagesCount(callback)

        // Then
        verify(exactly = 1) {
            appInboxController.getMessagesCount(callback)
        }
    }

    @Test
    fun whenSubscribeOnMessagesCountChanged_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.subscribeOnMessagesCountChanged(callback)

        // Then
        verify(exactly = 1) {
            appInboxController.subscribeCountChanges(callback)
        }
    }

    @Test
    fun whenUnsubscribeMessagesCountChanged_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<Int>>()

        // When
        inbox.unsubscribeMessagesCountChanged(callback)

        // Then
        verify(exactly = 1) {
            appInboxController.unsubscribeCountChanges(callback)
        }
    }

    @Test
    fun whenUnsubscribeAllMessagesCountChanged_thenCallAppInboxController() {
        // When
        inbox.unsubscribeAllMessagesCountChanged()

        // Then
        verify(exactly = 1) {
            appInboxController.unsubscribeAllCountChanges()
        }
    }

    @Test
    fun whenMarkAsOpened_thenCallAppInboxController() {
        // When
        inbox.markAsOpened(MESSAGE_ID)

        // Then
        verify(exactly = 1) {
            appInboxController.markAsOpened(eq(MESSAGE_ID))
        }
    }

    @Test
    fun whenMarkAllMessagesAsOpened_thenCallAppInboxController() {
        // Given
        val callback = mockk<RetenoResultCallback<Unit>>()

        // When
        inbox.markAllMessagesAsOpened(callback)

        // Then
        verify(exactly = 1) {
            appInboxController.markAllMessagesAsOpened(callback)
        }
    }
}