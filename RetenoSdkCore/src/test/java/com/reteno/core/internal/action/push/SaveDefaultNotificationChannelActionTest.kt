package com.reteno.core.internal.action.push

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SaveDefaultNotificationChannelActionTest {

    @Test
    fun `should save default notification channel to local settings on io`() = runUnitTest {
        given()
        val dispatchers = testDispatchers()
        val traced = TestActionRunner(this)
        val saved = mutableListOf<Pair<String, Boolean>>()
        val configRepository = mockk<ConfigRepository>().also {
            every { it.saveDefaultNotificationChannel(any()) } answers { saved += firstArg<String>() to dispatchers.markedIo.isRunningHere }
        }
        val sut = SaveDefaultNotificationChannelAction(configRepository, dispatchers, traced.runner)

        whenn()
        val result = sut(CHANNEL)

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(CHANNEL to true), saved)
        assertEquals(
            traced.successfulTrace("SaveDefaultNotificationChannel", "", "save default notification channel to local settings"),
            traced.sink.lines
        )
    }

    private companion object {
        const val CHANNEL = "{\"id\":\"defaultId\",\"name\":\"name\"}"
    }
}
