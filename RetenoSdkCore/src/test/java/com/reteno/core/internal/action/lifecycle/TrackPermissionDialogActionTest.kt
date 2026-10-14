package com.reteno.core.internal.action.lifecycle

import android.app.Activity
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TrackPermissionDialogActionTest {

    private class SutFixture(scope: TestScope, topPackage: String?) {
        val env = LifecycleActionFixture(scope)
        val sut = TrackPermissionDialogAction(
            platform = env.platform.also { it.topPackage = topPackage },
            lifecycleEvents = env.lifecycleEvents,
            runner = env.traced.runner
        )
    }

    @Test
    fun `should log permission asked when permission controller is on top`() = runUnitTest {
        given()
        val fixture = SutFixture(this, TrackPermissionDialogAction.PERMISSION_DIALOG_PACKAGE)

        whenn()
        fixture.sut(mockk<Activity>())

        then()
        assertEquals(listOf("PushNotificationsPermissionAsked"), fixture.env.loggedKeys)
    }

    @Test
    fun `should ignore other activities on top`() = runUnitTest {
        given()
        val fixture = SutFixture(this, "com.example.app")

        whenn()
        fixture.sut(mockk<Activity>())

        then()
        assertTrue(fixture.env.logged.isEmpty())
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ permission dialog is not shown") })
    }

    @Test
    fun `should ignore unknown top activity`() = runUnitTest {
        given()
        val fixture = SutFixture(this, null)

        whenn()
        fixture.sut(mockk<Activity>())

        then()
        assertTrue(fixture.env.logged.isEmpty())
    }
}
