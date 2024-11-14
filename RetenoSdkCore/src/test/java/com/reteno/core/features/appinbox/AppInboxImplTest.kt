package com.reteno.core.features.appinbox

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.controller.AppInboxController
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppInboxImplTest : BaseUnitTest() {

    @RelaxedMockK
    private lateinit var controller: AppInboxController


    @Test
    fun whenGetAppInboxMessagesCalled_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        every { controller.getAppInboxMessages(any(), any(), any(), any()) } answers {
            arg<RetenoResultCallback<AppInboxMessages>>(3).onSuccess(
                AppInboxMessages(
                    emptyList(),
                    totalPages = 0
                )
            )
        }

        sut.getAppInboxMessages(0, 20, null, object : RetenoResultCallback<AppInboxMessages> {
            override fun onSuccess(result: AppInboxMessages) {

            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            }
        })

        verify { controller.getAppInboxMessages(any(), any(), any(), any()) }
    }

    @Test
    fun whenGetAppInboxMessagesResponseSuccess_thenCallbackSuccessInvoked() = runTest {
        val sut = createSUT()
        every { controller.getAppInboxMessages(any(), any(), any(), any()) } answers {
            arg<RetenoResultCallback<AppInboxMessages>>(3).onSuccess(
                AppInboxMessages(
                    emptyList(),
                    totalPages = 0
                )
            )
        }

        var response: AppInboxMessages? = null
        sut.getAppInboxMessages(0, 20, null, object : RetenoResultCallback<AppInboxMessages> {
            override fun onSuccess(result: AppInboxMessages) {
                response = result
            }

            override fun onFailure(statusCode: Int?, resp: String?, throwable: Throwable?) {
                response = null
            }
        })

        withTimeout(1000L) {
            while (isActive && response == null) {
                delay(10L)
            }
        }
        assertEquals(AppInboxMessages(emptyList(), totalPages = 0), response)
    }

    @Test
    fun whenGetAppInboxMessagesResponseError_thenCallbackErrorInvoked() = runTest {
        val sut = createSUT()
        every { controller.getAppInboxMessages(any(), any(), any(), any()) } answers {
            arg<RetenoResultCallback<AppInboxMessages>>(3).onFailure(
                400,
                null,
                IllegalArgumentException()
            )
        }

        var response: Throwable? = null
        sut.getAppInboxMessages(0, 20, null, object : RetenoResultCallback<AppInboxMessages> {
            override fun onSuccess(result: AppInboxMessages) {
                response = null
            }

            override fun onFailure(statusCode: Int?, resp: String?, throwable: Throwable?) {
                response = throwable
            }
        })

        withTimeout(1000L) {
            while (isActive && response == null) {
                delay(10L)
            }
        }
        assertTrue(response != null)
    }

    @Test
    fun whenGetAppInboxMessagesCountCalled_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        every { controller.getMessagesCount(any()) } answers {
            firstArg<RetenoResultCallback<AppInboxMessages>>().onSuccess(
                AppInboxMessages(
                    emptyList(),
                    totalPages = 0
                )
            )
        }

        sut.getAppInboxMessagesCount(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {

            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            }
        })

        verify { controller.getMessagesCount(any()) }
    }

    @Test
    fun whenGetAppInboxMessagesCountResponseSuccess_thenCallbackSuccessInvoked() = runTest {
        val sut = createSUT()

        var response: Int? = null
        every { controller.getMessagesCount(any()) } answers {
            firstArg<RetenoResultCallback<Int>>().onSuccess(
                2
            )
        }

        sut.getAppInboxMessagesCount(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {
                response = result
            }

            override fun onFailure(statusCode: Int?, resp: String?, throwable: Throwable?) {
                response = null
            }
        })

        withTimeout(1000L) {
            while (isActive && response == null) {
                delay(10L)
            }
        }
        assertEquals(2, response)
    }

    @Test
    fun whenGetAppInboxMessagesCountResponseError_thenCallbackErrorInvoked() = runTest {
        val sut = createSUT()
        var response: Throwable? = null
        every { controller.getMessagesCount(any()) } answers {
            firstArg<RetenoResultCallback<Int>>().onFailure(
                404, null, IllegalArgumentException()
            )
        }

        sut.getAppInboxMessagesCount(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {
                response = null
            }

            override fun onFailure(statusCode: Int?, resp: String?, throwable: Throwable?) {
                response = throwable
            }
        })

        withTimeout(1000L) {
            while (isActive && response == null) {
                delay(10L)
            }
        }
        assertTrue(response is IllegalArgumentException)
    }

    @Test
    fun whenSubscribeOnMessagesCountChangedCalled_thenCallMirroredToController() = runTest {
        val sut = createSUT()

        sut.subscribeOnMessagesCountChanged(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {

            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            }
        })

        verify { controller.subscribeCountChanges(any()) }
    }

    @Test
    fun whenUnsubscribeOnMessagesCountChangedCalledNewResult_thenCallMirroredToController() =
        runTest {
            val sut = createSUT()

            val callback = object : RetenoResultCallback<Int> {
                override fun onSuccess(result: Int) {

                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                }
            }

            sut.unsubscribeMessagesCountChanged(callback)

            verify { controller.unsubscribeCountChanges(any()) }
        }

    @Test
    fun whenUnsubscribeOnAllMessagesCountChangedCalledNewResult_thenCallMirroredToController() =
        runTest {
            val sut = createSUT()
            sut.unsubscribeAllMessagesCountChanged()

            verify { controller.unsubscribeAllCountChanges() }
        }

    @Test
    fun whenMarkAsOpened_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        sut.markAsOpened("10")

        verify { controller.markAsOpened(eq("10")) }
    }

    @Test
    fun whenMarkAllMessagesAsOpened_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        val callback = object : RetenoResultCallback<Unit> {
            override fun onSuccess(result: Unit) {

            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            }
        }
        sut.markAllMessagesAsOpened(callback)

        verify { controller.markAllMessagesAsOpened(eq(callback)) }
    }

    @Test
    fun whenMarkAllMessagesAsOpenedRespondsWithSuccess_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        every { controller.markAllMessagesAsOpened(any()) } answers {
            firstArg<RetenoResultCallback<Unit>>().onSuccess(Unit)
        }
        var res: Unit? = null
        val callback = object : RetenoResultCallback<Unit> {
            override fun onSuccess(result: Unit) {
                res = result
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                res = null
            }
        }
        sut.markAllMessagesAsOpened(callback)

        assertTrue(res != null)
    }

    @Test
    fun whenMarkAllMessagesAsOpenedRespondsWithError_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        every { controller.markAllMessagesAsOpened(any()) } answers {
            firstArg<RetenoResultCallback<Unit>>().onFailure(400, null, IllegalArgumentException())
        }
        var res: Throwable? = null
        val callback = object : RetenoResultCallback<Unit> {
            override fun onSuccess(result: Unit) {
                res = null
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                res = throwable
            }
        }
        sut.markAllMessagesAsOpened(callback)

        assertTrue(res is IllegalArgumentException)
    }


    private fun createSUT() = AppInboxImpl(controller)

}