package com.reteno.core.internal.action.user

import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SetMultiAccountUserAttributesActionTest {

    private class SutFixture(scope: TestScope, suffix: String? = null) {
        val env = UserActionFixture(scope, suffix)
        var segmentationRequests = 0
        val sut = SetMultiAccountUserAttributesAction(env.writer, { env.sdk }, { segmentationRequests++ }, env.traced.runner)
    }

    @Test
    fun `should switch to account named after external user id`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = null)

        whenn()
        fixture.sut("account-2", null)

        then()
        assertEquals("account-2", fixture.env.deviceId.idSuffix)
        assertEquals(listOf("stop on main", "start on main"), fixture.env.sdk.calls)
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
    }

    @Test
    fun `should update current account without restart`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = "account-2")

        whenn()
        fixture.sut("account-2", null)

        then()
        assertTrue(fixture.env.sdk.calls.isEmpty())
        assertEquals(1, fixture.segmentationRequests)
    }

    @Test
    fun `should not schedule segmentation refresh when sdk is stopped`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = "account-2")
        fixture.env.sdk.isStarted = false

        whenn()
        fixture.sut("account-2", null)

        then()
        assertEquals(0, fixture.segmentationRequests)
        assertTrue(fixture.env.traced.sink.lines.first().contains("SetMultiAccountUserAttributes  externalUserId=…nt-2  user={}"))
    }
}
