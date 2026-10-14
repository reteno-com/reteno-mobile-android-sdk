package com.reteno.core.internal.platform

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class WatchedHomeFragment : Fragment()

class WatchedDetailsFragment : Fragment()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class FragmentScreenWatcherTest {

    private class SutFixture {
        val activityHelper = mockk<RetenoActivityHelper>()
        val callbacks = slot<RetenoLifecycleCallbacks>()
        val screens = mutableListOf<Pair<String, ScreenTrackingTrigger>>()
        val sut = FragmentScreenWatcher(activityHelper) { name, trigger -> screens += name to trigger }

        init {
            every { activityHelper.registerActivityLifecycleCallbacks(any(), capture(callbacks)) } returns Unit
        }
    }

    @Test
    fun `should report fragments started and resumed in started activity`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fixture.sut.register()
        fixture.callbacks.captured.start(activity)

        whenn()
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, WatchedHomeFragment())
            .commitNow()

        then()
        assertEquals(
            listOf(
                "WatchedHomeFragment" to ScreenTrackingTrigger.ON_START,
                "WatchedHomeFragment" to ScreenTrackingTrigger.ON_RESUME
            ),
            fixture.screens
        )
    }

    @Test
    fun `should stop reporting fragments after activity stopped`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fixture.sut.register()
        fixture.callbacks.captured.start(activity)
        fixture.callbacks.captured.stop(activity)

        whenn()
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, WatchedDetailsFragment())
            .commitNow()

        then()
        assertTrue(fixture.screens.isEmpty())
    }

    @Test
    fun `should ignore activities without fragment support`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.register()
        val plain = mockk<Activity>()

        whenn()
        fixture.callbacks.captured.start(plain)
        fixture.callbacks.captured.stop(plain)
        fixture.callbacks.captured.pause(plain)
        fixture.callbacks.captured.resume(plain)

        then()
        assertTrue(fixture.screens.isEmpty())
    }
}
