package com.reteno.core.internal.action.inbox

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MarkInboxMessageOpenedActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val saved = mutableListOf<Pair<AppInboxMessageDb, Boolean>>()
        val dispatchers = scope.testDispatchers()
        val inboxDb = mockk<RetenoDatabaseManagerAppInbox>().also {
            every { it.insertAppInboxMessage(any()) } answers { saved += firstArg<AppInboxMessageDb>() to dispatchers.markedIo.isRunningHere }
        }
        val configRepository = mockk<ConfigRepository>().also { every { it.getDeviceId() } returns stubDeviceId() }
        val sync = FakeSyncTrigger()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val sut = MarkInboxMessageOpenedAction(inboxDb, configRepository, sync, initGate, dispatchers, { TIME }, traced.runner)
    }

    @Test
    fun `should save opened message for current device and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("m1")

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(AppInboxMessageDb(id = "m1", deviceId = stubDeviceId().id, occurredDate = TIME) to true), fixture.saved)
        assertEquals(1, fixture.sync.requests)
        assertTrue(fixture.traced.sink.lines.first().contains("MarkInboxMessageOpened  messageId=m1"))
    }

    @Test
    fun `should wait for sdk initialization before saving`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        launch { fixture.sut("m1") }
        runCurrent()
        val savedBeforeInit = fixture.saved.size

        whenn()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(0, savedBeforeInit)
        assertEquals(1, fixture.saved.size)
    }

    private companion object {
        const val TIME = "2026-01-02T03:04:05Z"
    }
}
