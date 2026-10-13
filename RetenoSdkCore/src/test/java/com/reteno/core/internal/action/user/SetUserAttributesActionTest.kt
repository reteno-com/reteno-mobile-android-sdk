package com.reteno.core.internal.action.user

import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SetUserAttributesActionTest {

    private class SutFixture(scope: TestScope, suffix: String? = null) {
        val env = UserActionFixture(scope, suffix)
        var segmentationRequests = 0
        val segmentationRefresh = mockk<SegmentationRefresh>().also { every { it.request() } answers { segmentationRequests++ } }
        val sut = SetUserAttributesAction(env.writer, env.sdk, segmentationRefresh, env.traced.runner)
    }

    @Test
    fun `should write contact for single account user and sync it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("new-user", stubUser())

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("external id new-user", "user saved"), fixture.env.log)
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
    }

    @Test
    fun `should leave multi account mode by clearing suffix`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = "account")

        whenn()
        fixture.sut("new-user", null)

        then()
        assertEquals(null, fixture.env.deviceId.idSuffix)
        assertEquals(listOf("stop on main", "start on main"), fixture.env.sdk.calls)
    }

    @Test
    fun `should schedule segmentation refresh when sdk is started`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("new-user", null)

        then()
        assertEquals(1, fixture.segmentationRequests)
    }

    @Test
    fun `should not schedule segmentation refresh when sdk is stopped`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.sdk.isStarted = false

        whenn()
        fixture.sut("new-user", null)

        then()
        assertEquals(0, fixture.segmentationRequests)
    }

    @Test
    fun `should trace redacted user id and attribute names only`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("customer-42-secret", stubUser())

        then()
        val start = fixture.env.traced.sink.lines.first()
        assertEquals("┌─ [r1] SetUserAttributes  externalUserId=…cret  user={userAttributes}", start)
        assertFalse(fixture.env.traced.sink.lines.any { it.contains("john@example.com") || it.contains("customer-42") })
    }
}
