package com.reteno.core.internal.action.init

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.inbox.InboxCountPolling
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StopSdkActionTest {

    @Test
    fun `should mark sdk stopped and stop its parts in order`() = runUnitTest {
        given()
        val calls = mutableListOf<String>()
        val dispatchers = testDispatchers()
        val state = SdkRunState().also { it.markStarted() }
        val sessionHandler = mockk<RetenoSessionHandler>().also { every { it.stop() } answers { calls += "stop session started=${state.isStarted}" } }
        val dataSync = mockk<SyncScheduler>().also { every { it.stopScheduler() } answers { calls += "stop scheduler" } }
        val iamView = mockk<IamView>().also { every { it.pause() } answers { calls += "pause in-app view main=${dispatchers.markedMain.isRunningHere}" } }
        val inboxCount = mockk<InboxCountPolling>().also { every { it.pause() } answers { calls += "pause inbox count polling" } }
        val sut = StopSdkAction(state, sessionHandler, dataSync, iamView, inboxCount, dispatchers, TestActionRunner(this).runner)

        whenn()
        sut()

        then()
        assertEquals(listOf("stop session started=false", "stop scheduler", "pause in-app view main=true", "pause inbox count polling"), calls)
    }
}
