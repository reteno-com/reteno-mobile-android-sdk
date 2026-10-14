package com.reteno.core.internal.lifecycle

import android.app.Activity
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.lifecycle.AppBackgrounded
import com.reteno.core.internal.action.lifecycle.AppForegrounded
import com.reteno.core.internal.action.lifecycle.TrackPermissionDialog
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AppLifecycleTrackingTest {

    private class SutFixture(scope: TestScope) {
        val calls = mutableListOf<String>()
        val platform = FakeAppPlatform()
        private val dispatchers = scope.testDispatchers()
        private val appForegrounded = mockk<AppForegrounded>().also {
            coEvery { it() } answers {
                calls += "foregrounded io=${dispatchers.markedIo.isRunningHere}"
                Result.success(Unit)
            }
        }
        private val appBackgrounded = mockk<AppBackgrounded>().also {
            coEvery { it() } answers {
                calls += "backgrounded io=${dispatchers.markedIo.isRunningHere}"
                Result.success(Unit)
            }
        }
        private val trackPermissionDialog = mockk<TrackPermissionDialog>().also {
            coEvery { it(any()) } answers {
                calls += "activity paused"
                Result.success(Unit)
            }
        }
        val sut = AppLifecycleTrackingImpl(
            platform,
            appForegrounded,
            appBackgrounded,
            trackPermissionDialog,
            SerialQueue(scope.workScope(), "Lifecycle", FakeLogSink()),
            dispatchers
        )
    }

    @Test
    fun `should run lifecycle actions in order of app transitions`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.start()

        whenn()
        fixture.platform.onForeground!!.invoke()
        fixture.platform.onActivityPaused!!.invoke(mockk<Activity>())
        fixture.platform.onBackground!!.invoke()
        runCurrent()

        then()
        val expected = listOf("foregrounded io=true", "activity paused", "backgrounded io=true")
        assertEquals(expected, fixture.calls)
    }
}
