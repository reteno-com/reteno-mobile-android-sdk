package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test

class LogEventRepositoryImplTest : BaseRobolectricTest() {

    @RelaxedMockK
    private lateinit var manager: RetenoDatabaseManagerLogEvent

    @RelaxedMockK
    private lateinit var apiClient: ApiClient


    @Test
    fun givenEvent_whenSaveLogEventCalled_thenManagerCalled() = runRetenoTest {
        val event = RetenoLogEvent()
        coEvery { manager.getLogEvents(any()) } returns listOf(event.toDb())
        val sut = createSUT()
        sut.saveLogEvent(event)

        verify { manager.insertLogEvent(event.toDb()) }
    }

    @Test
    fun givenEvent_whenSaveLogEventCalledAndEventsPushedWithSuccess_thenDatabaseCleared() =
        runRetenoTest {
            val event = RetenoLogEvent()
            coEvery { manager.getLogEvents(any()) } returns listOf(event.toDb())
            coEvery { apiClient.post(any(), any(), any()) } answers {
                thirdArg<ResponseCallback>().onSuccess("")
            }
            val sut = createSUT()
            sut.saveLogEvent(event)

            verify { manager.deleteLogEvents(listOf(event.toDb())) }
        }

    @Test
    fun givenEvent_whenSaveLogEventCalledAndEventsPushedWithRepeatableError_thenDatabaseCleared() =
        runRetenoTest {
            val event = RetenoLogEvent()
            coEvery { manager.getLogEvents(any()) } returns listOf(event.toDb())
            coEvery { apiClient.post(any(), any(), any()) } answers {
                thirdArg<ResponseCallback>().onFailure(404, null, IllegalArgumentException())
            }
            val sut = createSUT()
            sut.saveLogEvent(event)

            verify { manager.deleteLogEvents(listOf(event.toDb())) }
        }

    @Test
    fun givenEvent_whenSaveLogEventCalledAndEventsPushedWithNonRepeatableError_thenDatabaseNotCleared() =
        runRetenoTest {
            val event = RetenoLogEvent()
            coEvery { manager.getLogEvents(any()) } returns listOf(event.toDb())
            coEvery { apiClient.post(any(), any(), any()) } answers {
                thirdArg<ResponseCallback>().onFailure(500, null, IllegalArgumentException())
            }
            val sut = createSUT()
            sut.saveLogEvent(event)

            verify(exactly = 0) { manager.deleteLogEvents(listOf(event.toDb())) }
        }

    @Test
    fun whenPushEventsWithEmptyDB_thenNoRequest() = runRetenoTest {
        coEvery { manager.getLogEvents(any()) } returns listOf()
        coEvery { apiClient.post(any(), any(), any()) } answers {
            thirdArg<ResponseCallback>().onFailure(500, null, IllegalArgumentException())
        }

        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
    }


    private fun createSUT() = LogEventRepositoryImpl(
        databaseManager = manager,
        apiClient = apiClient
    )
}