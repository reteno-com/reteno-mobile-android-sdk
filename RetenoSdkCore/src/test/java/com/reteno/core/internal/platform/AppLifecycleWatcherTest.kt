package com.reteno.core.internal.platform

import android.app.Activity
import androidx.lifecycle.Lifecycle
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AppLifecycleWatcherTest {

    private class SutFixture {
        val owner = ManualLifecycleOwner()
        val activityHelper = mockk<RetenoActivityHelper>()
        val callbacks = slot<RetenoLifecycleCallbacks>()
        val calls = mutableListOf<String>()
        val paused = mutableListOf<Activity>()
        val sut = AppLifecycleWatcher(
            lifecycle = owner.lifecycle,
            activityHelper = activityHelper,
            onForeground = { calls += "foreground" },
            onBackground = { calls += "background" },
            onActivityPaused = { paused += it }
        )

        init {
            every { activityHelper.registerActivityLifecycleCallbacks(any(), capture(callbacks)) } returns Unit
        }
    }

    @Test
    fun `should report foreground and background transitions of the app`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.register()

        whenn()
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        then()
        assertEquals(listOf("foreground", "background"), fixture.calls)
    }

    @Test
    fun `should report paused activities`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.register()
        val activity = mockk<Activity>()

        whenn()
        fixture.callbacks.captured.pause(activity)

        then()
        assertSame(activity, fixture.paused.single())
    }
}
