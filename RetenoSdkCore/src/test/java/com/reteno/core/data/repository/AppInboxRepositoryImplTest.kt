package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toDomain
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.appinbox.AppInboxMessagesStatusRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import io.mockk.MockKException
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AppInboxRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val INBOX_ID = "ehc3-5hdh4-fde4yh-3d5g"
        private const val INBOX_OCCURRED_TIME = "2022-11-22T13:38:01Z"
        private const val INBOX_DEVICE_ID = "device_test"
        private val INBOX_STATUS = AppInboxMessageStatusDb.OPENED
        private const val PAGE = 2
        private const val PAGE_SIZE = 12
        private val STATUS = AppInboxStatus.UNOPENED

        private const val ERROR_CODE = 400
        private const val ERROR_CODE_REPEATABLE = 500
        private const val ERROR_MSG = "error_msg"
        private val ERROR_EXCEPTION = MockKException(ERROR_MSG)

        private const val REGULAR_DELAY = 30_000L
        private const val INITIAL_DELAY = 0L

        private const val MESSAGES_COUNT = 7
        private const val MESSAGES_COUNT_ZERO = 0

        private lateinit var scheduler: ScheduledExecutorService

        private fun mockStaticJsonMappers() {
            mockkStatic("com.reteno.core.data.remote.mapper.JsonMappersKt")
            mockkStatic("com.reteno.core.data.remote.mapper.AppInboxMapperKt")
        }

        private fun unMockStaticJsonMappers() {
            unmockkStatic("com.reteno.core.data.remote.mapper.JsonMappersKt")
            unmockkStatic("com.reteno.core.data.remote.mapper.AppInboxMapperKt")
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    @RelaxedMockK
    private lateinit var databaseManagerAppInbox: RetenoDatabaseManagerAppInbox

    @RelaxedMockK
    private lateinit var databaseManagerDevice: RetenoDatabaseManagerDevice

    private lateinit var inboxRepository: AppInboxRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockStaticJsonMappers()
        scheduler = application.scheduler
        inboxRepository = AppInboxRepositoryImpl(apiClient, databaseManagerAppInbox, configRepository)
    }

    override fun after() {
        super.after()
        unMockStaticJsonMappers()
    }

    @Test
    fun whenMessageOpened_thenInsertDbAndPush() {
        // Given
        val inboxStatus = getTestAppInboxDb()
        every { Util.getCurrentTimeStamp() } returns INBOX_OCCURRED_TIME
        every { configRepository.getDeviceId().id } returns INBOX_DEVICE_ID
        every { databaseManagerAppInbox.getAppInboxMessages(any()) } returnsMany listOf(
            listOf(inboxStatus),
            emptyList()
        )

        // When
        inboxRepository.saveMessageOpened(INBOX_ID)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { databaseManagerAppInbox.insertAppInboxMessage(inboxStatus) }
    }

    @Test
    fun whenDbHasSavedMessage_thenSendToApi() {
        // Given
        val inboxStatus = getTestAppInboxDb()
        val resultJson = "result_json"

        every { any<AppInboxMessagesStatusRemote>().toJson() } returns resultJson
        every { databaseManagerAppInbox.getAppInboxMessages(any()) } returnsMany listOf(
            listOf(inboxStatus),
            emptyList()
        )

        // When
        inboxRepository.pushMessagesStatus()

        // Then
        verify(exactly = 1) { any<AppInboxMessagesStatusRemote>().toJson() }
        verify(exactly = 1) {
            apiClient.post(
                eq(ApiContract.AppInbox.MessagesStatus),
                eq(resultJson),
                any()
            )
        }
    }

    @Test
    fun whenMessagesPushSuccessful_thenNextOperation() {
        // Given
        val inboxStatus = getTestAppInboxDb()
        val spyRepository = spyk(inboxRepository, recordPrivateCalls = true)
        every { databaseManagerAppInbox.getAppInboxMessages(any()) } returnsMany listOf(
            listOf(
                inboxStatus
            ), listOf(inboxStatus), emptyList()
        )
        every { apiClient.post(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        spyRepository.pushMessagesStatus()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { databaseManagerAppInbox.deleteAppInboxMessages(listOf(inboxStatus)) }
        verify(exactly = 1) { spyRepository["fetchCount"]() }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
        verify(exactly = 0) { PushOperationQueue.removeAllOperations() }

    }

    @Test
    fun whenMessagesPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val inboxStatus = getTestAppInboxDb()
        every { databaseManagerAppInbox.getAppInboxMessages(any()) } returns listOf(inboxStatus)
        every { apiClient.post(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE_REPEATABLE, null, null)
        }

        // When
        inboxRepository.pushMessagesStatus()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun whenMessagesPushFailedAndErrorIsNonRepeatable_thenNextOperation() {
        // Given
        val inboxStatus = getTestAppInboxDb()
        every { databaseManagerAppInbox.getAppInboxMessages(any()) } returnsMany listOf(
            listOf(
                inboxStatus
            ), listOf(inboxStatus), emptyList()
        )
        every { apiClient.post(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE, null, null)
        }

        // When
        inboxRepository.pushMessagesStatus()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { databaseManagerAppInbox.deleteAppInboxMessages(listOf(inboxStatus)) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
        verify(exactly = 0) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenNoMessagesInDb_whenDevicePush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { databaseManagerDevice.getDevices(any()) } returns emptyList()

        // When
        inboxRepository.pushMessagesStatus()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoOutdatedAppInbox_whenClearOldAppInbox_thenSentNothing() {
        // Given
        every { databaseManagerAppInbox.deleteAppInboxMessagesByTime(any()) } returns 0

        // When
        inboxRepository.clearOldMessages(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerAppInbox.deleteAppInboxMessagesByTime(any()) }
        verify(exactly = 0) { Logger.captureMessage(any()) }
    }

    @Test
    fun givenAreOutdatedAppInbox_whenClearOldAppInbox_thenSentCountDeleted() {
        // Given
        val deletedInbox = 2
        every { databaseManagerAppInbox.deleteAppInboxMessagesByTime(any()) } returns deletedInbox
        val expectedMsg = "Outdated Inbox: - $deletedInbox"

        // When
        inboxRepository.clearOldMessages(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerAppInbox.deleteAppInboxMessagesByTime(any()) }
        verify(exactly = 1) { Logger.captureMessage(eq(expectedMsg)) }
    }

    @Test
    fun whenSetAllMessagesOpened_thenReturnSuccessCallback() {
        // Given
        val jsonString = "{}"
        val callback = spyk<RetenoResultCallback<Unit>>()
        val spyRepository = spyk(inboxRepository, recordPrivateCalls = true)

        every { any<AppInboxMessagesStatusRemote>().toJson() } returns jsonString
        every { apiClient.post(any(), any(), any()) } answers {
            thirdArg<ResponseCallback>().onSuccess("")
        }

        // When
        spyRepository.setAllMessageOpened(callback)

        // Then
        verify(exactly = 1) {
            apiClient.post(
                ApiContract.AppInbox.MessagesStatus,
                eq(jsonString),
                any()
            )
        }
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { callback.onSuccess(Unit) }
        verify(exactly = 1) { databaseManagerAppInbox.deleteAllAppInboxMessages() }
        verify(exactly = 1) { spyRepository["fetchCount"]() }
    }

    @Test
    fun whenSetAllMessagesOpened_thenReturnErrorCallback() {
        // Given
        val jsonString = "{}"
        val callback = spyk<RetenoResultCallback<Unit>>()

        every { any<AppInboxMessagesStatusRemote>().toJson() } returns jsonString
        every { apiClient.post(any(), any(), any()) } answers {
            thirdArg<ResponseCallback>().onFailure(ERROR_CODE, ERROR_MSG, ERROR_EXCEPTION)
        }

        // When
        inboxRepository.setAllMessageOpened(callback)

        // Then
        verify(exactly = 1) {
            apiClient.post(
                ApiContract.AppInbox.MessagesStatus,
                eq(jsonString),
                any()
            )
        }
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { callback.onFailure(ERROR_CODE, eq(ERROR_MSG), ERROR_EXCEPTION) }
        verify(exactly = 0) { databaseManagerAppInbox.deleteAllAppInboxMessages() }
    }

    @Test
    fun whenGetMessagesWithParams_thenResultSuccess() {
        // Given
        val inboxMessages = mockk<AppInboxMessages>()
        val retenoCallback = mockk<RetenoResultCallback<AppInboxMessages>>(relaxed = true)
        val resultJson = "{}"
        val queryParams = mapOf(
            ApiContract.AppInbox.QUERY_PAGE to PAGE.toString(),
            ApiContract.AppInbox.QUERY_PAGE_SIZE to PAGE_SIZE.toString(),
            ApiContract.AppInbox.QUERY_STATUS to STATUS.str
        )

        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJson)
        }
        every { any<InboxMessagesRemote>().toDomain() } returns inboxMessages

        // When
        inboxRepository.getMessages(PAGE, PAGE_SIZE, STATUS, retenoCallback)

        // Then
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { retenoCallback.onSuccess(inboxMessages) }
        verify(exactly = 1) { any<InboxMessagesRemote>().toDomain() }
        verify(exactly = 1) {
            apiClient.get(
                eq(ApiContract.AppInbox.Messages),
                eq(queryParams),
                any()
            )
        }
    }

    @Test
    fun whenGetMessagesWithoutParams_thenResultSuccess() {
        // Given
        val inboxMessages = mockk<AppInboxMessages>()
        val retenoCallback = mockk<RetenoResultCallback<AppInboxMessages>>(relaxed = true)
        val resultJson = "{}"
        val queryParams = mapOf(
            ApiContract.AppInbox.QUERY_PAGE to null,
            ApiContract.AppInbox.QUERY_PAGE_SIZE to null,
            ApiContract.AppInbox.QUERY_STATUS to null
        )

        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJson)
        }
        every { any<InboxMessagesRemote>().toDomain() } returns inboxMessages

        // When
        inboxRepository.getMessages(null, null, null, retenoCallback)

        // Then
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { retenoCallback.onSuccess(inboxMessages) }
        verify(exactly = 1) { any<InboxMessagesRemote>().toDomain() }
        verify(exactly = 1) {
            apiClient.get(
                eq(ApiContract.AppInbox.Messages),
                eq(queryParams),
                any()
            )
        }
    }

    @Test
    fun givenSomeError_whenGetMessages_thenResultFailure() {
        // Given
        val retenoCallback = mockk<RetenoResultCallback<AppInboxMessages>>(relaxed = true)
        val queryParams = mapOf(
            ApiContract.AppInbox.QUERY_PAGE to null,
            ApiContract.AppInbox.QUERY_PAGE_SIZE to null,
            ApiContract.AppInbox.QUERY_STATUS to null
        )

        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE, ERROR_MSG, ERROR_EXCEPTION)
        }

        // When
        inboxRepository.getMessages(null, null, null, retenoCallback)

        // Then
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { retenoCallback.onFailure(ERROR_CODE, ERROR_MSG, ERROR_EXCEPTION) }
        verify(exactly = 1) {
            apiClient.get(
                eq(ApiContract.AppInbox.Messages),
                eq(queryParams),
                any()
            )
        }
    }

    @Test
    fun whenGetMessagesCount_thenResultSuccess() {
        // Given
        val retenoCallback = mockk<RetenoResultCallback<Int>>(relaxed = true)
        mockkCountSuccessResponse()

        // When
        inboxRepository.getMessagesCount(retenoCallback)

        // Then
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { retenoCallback.onSuccess(MESSAGES_COUNT) }
        verify(exactly = 1) { apiClient.get(eq(ApiContract.AppInbox.MessagesCount), null, any()) }
    }

    @Test
    fun givenSomeError_whenGetMessagesCount_thenResultFailure() {
        // Given
        val retenoCallback = mockk<RetenoResultCallback<Int>>(relaxed = true)
        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE, ERROR_MSG, ERROR_EXCEPTION)
        }

        // When
        inboxRepository.getMessagesCount(retenoCallback)

        // Then
        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
        verify(exactly = 1) { retenoCallback.onFailure(ERROR_CODE, eq(ERROR_MSG), ERROR_EXCEPTION) }
        verify(exactly = 1) { apiClient.get(eq(ApiContract.AppInbox.MessagesCount), null, any()) }
    }

    @Test
    fun givenPollingIsInactive_whenSubscribeCountChanges_thenStartPolling() {
        // When
        inboxRepository.subscribeOnMessagesCountChanged(mockk())

        // Then
        verify(exactly = 1) {
            scheduler.scheduleAtFixedRate(
                any(),
                INITIAL_DELAY,
                REGULAR_DELAY,
                TimeUnit.MILLISECONDS
            )
        }
    }

    @Test
    fun givenPollingIsActive_whenSubscribeCountChanges_thenPollingDoesNotChange() {
        // When
        inboxRepository.subscribeOnMessagesCountChanged(mockk())
        inboxRepository.subscribeOnMessagesCountChanged(mockk())

        // Then
        verify(exactly = 1) {
            scheduler.scheduleAtFixedRate(
                any(),
                INITIAL_DELAY,
                REGULAR_DELAY,
                TimeUnit.MILLISECONDS
            )
        }
    }

    @Test
    fun givenListenersHaveMoreThanOneItem_whenUnsubscribeCountChanges_thenDoesNotStopPolling() {
        // Given
        val listenerOne = mockk<RetenoResultCallback<Int>>()
        val listenerTwo = mockk<RetenoResultCallback<Int>>()

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listenerOne)
        inboxRepository.subscribeOnMessagesCountChanged(listenerTwo)
        inboxRepository.unsubscribeMessagesCountChanged(listenerOne)

        // Then
        verify(exactly = 0) { scheduler.shutdownNow() }
    }

    @Test
    fun givenListenersHaveOneItem_whenUnsubscribeCountChanges_thenStopPolling() {
        // Given
        val listener = mockk<RetenoResultCallback<Int>>()

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listener)
        inboxRepository.unsubscribeMessagesCountChanged(listener)

        // Then
        verify(exactly = 1) { scheduler.shutdownNow() }
    }

    @Test
    fun whenUnsubscribeAll_thenStopPolling() {
        // When
        inboxRepository.subscribeOnMessagesCountChanged(mockk())
        inboxRepository.unsubscribeAllMessagesCountChanged()

        // Then
        verify(exactly = 1) { scheduler.shutdownNow() }
    }

    @Test
    fun givenPollingIsActive_whenFetchCount_thenNotifySuccess() {
        // Given
        val listener = spyk<RetenoResultCallback<Int>>()
        mockkCountSuccessResponse()

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listener)

        // Then
        verify(exactly = 1) { listener.onSuccess(MESSAGES_COUNT) }
    }

    @Test
    fun givenPollingIsActiveWithSeveralListeners_whenFetchCount_thenNotifySuccessToAllListeners() {
        // Given
        val listenerOne = spyk<RetenoResultCallback<Int>>()
        val listenerTwo = spyk<RetenoResultCallback<Int>>()

        mockkCountSuccessResponse()

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listenerOne)
        inboxRepository.subscribeOnMessagesCountChanged(listenerTwo)

        // Then
        verify(exactly = 1) { listenerOne.onSuccess(MESSAGES_COUNT) }
        verify(exactly = 1) { listenerTwo.onSuccess(MESSAGES_COUNT) }
    }

    @Test
    fun givenPollingIsActiveAndHasValue_whenFetchCountAndValuesAreEquals_thenNotifySuccessWithoutCallback() {
        // Given
        val listener = spyk<RetenoResultCallback<Int>>()
        mockkCountSuccessResponse()
        val currentThreadExecutor = Executor(Runnable::run)
        every { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) } answers {
            currentThreadExecutor.execute(firstArg())
            currentThreadExecutor.execute(firstArg())
            mockk()
        }

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listener)

        // Then
        verify(exactly = 1) { listener.onSuccess(MESSAGES_COUNT) }
    }

    @Test
    fun givenPollingIsActiveAndHasValue_whenFetchCountAndValuesAreNotEquals_thenNotifySuccessWithoutCallback() {
        // Given
        val listener = spyk<RetenoResultCallback<Int>>()
        mockkCountSuccessResponseForTwoCalls()
        val currentThreadExecutor = Executor(Runnable::run)
        every { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) } answers {
            currentThreadExecutor.execute(firstArg())
            currentThreadExecutor.execute(firstArg())
            mockk()
        }

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listener)

        // Then
        verify(exactly = 1) { listener.onSuccess(MESSAGES_COUNT_ZERO) }
        verify(exactly = 1) { listener.onSuccess(MESSAGES_COUNT) }
    }

    @Test
    fun givenPollingIsActiveAndSomeError_whenFetchCount_thenNotifyFailure() {
        // Given
        val listener = spyk<RetenoResultCallback<Int>>()
        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE, ERROR_MSG, ERROR_EXCEPTION)
        }

        // When
        inboxRepository.subscribeOnMessagesCountChanged(listener)

        // Then
        verify(exactly = 1) { listener.onFailure(ERROR_CODE, eq(ERROR_MSG), ERROR_EXCEPTION) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getTestAppInboxDb(): AppInboxMessageDb {
        return AppInboxMessageDb(
            id = INBOX_ID,
            deviceId = INBOX_DEVICE_ID,
            occurredDate = INBOX_OCCURRED_TIME,
            status = INBOX_STATUS
        )
    }

    private fun mockkCountSuccessResponse() {
        val resultJson = """
            {
                "unreadCount": $MESSAGES_COUNT
            }
        """.trimIndent()

        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJson)
        }
    }

    private fun mockkCountSuccessResponseForTwoCalls() {
        val resultJsonFirst = """
            {
                "unreadCount": $MESSAGES_COUNT_ZERO
            }
        """.trimIndent()

        val resultJsonSecond = """
            {
                "unreadCount": $MESSAGES_COUNT
            }
        """.trimIndent()

        every { apiClient.get(any(), any(), any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJsonFirst)
        } andThenAnswer {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJsonSecond)
        }
    }
    // endregion helper methods --------------------------------------------------------------------
}
