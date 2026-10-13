package com.reteno.core.internal.action.push

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.view.iam.IamView
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InitializeIamFromPushActionTest {

    @Test
    fun `should show in-app message from push on main thread`() = runUnitTest {
        given()
        val dispatchers = testDispatchers()
        val traced = TestActionRunner(this)
        val shown = mutableListOf<Pair<String, Boolean>>()
        val iamView = mockk<IamView>().also {
            every { it.initialize(any<String>()) } answers { shown += firstArg<String>() to dispatchers.markedMain.isRunningHere }
        }
        val sut = InitializeIamFromPushAction(iamView, dispatchers, traced.runner)

        whenn()
        val result = sut("interaction-1")

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("interaction-1" to true), shown)
        assertEquals(
            traced.successfulTrace("InitializeIamFromPush", "interactionId=interaction-1", "show in-app message from push"),
            traced.sink.lines
        )
    }
}
