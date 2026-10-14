package com.reteno.core.internal.action.lifecycle

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.util.Constants
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UpdatePushPermissionStatusActionTest {

    @Test
    fun `should broadcast push permission change to registered receivers`() = runUnitTest {
        given()
        val platform = mockk<AppPlatform>().also { every { it.sendBroadcast(any()) } returns 2 }
        val traced = TestActionRunner(this)
        val sut = UpdatePushPermissionStatusAction(platform, traced.runner)

        whenn()
        sut()

        then()
        verify(exactly = 1) { platform.sendBroadcast(Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED) }
        val lines = traced.sink.lines
        assertTrue(lines.any { it.contains("✓ notify push permission receivers") && it.contains("receivers=2") })
    }
}
