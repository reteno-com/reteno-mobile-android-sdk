package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.domain.ResponseCallback
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