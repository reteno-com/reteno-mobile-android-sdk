package com.reteno.core.lifecycle

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoActivityHelperImplTest {

    private class SutFixture {
        val controller = Robolectric.buildActivity(Activity::class.java)
        val activity: Activity = controller.get()
        val callbacks = RecordingLifecycleCallbacks()
        val sut = RetenoActivityHelperImpl()

        init {
            sut.enableLifecycleCallbacks(ApplicationProvider.getApplicationContext<Application>())
        }
    }

    private class RecordingLifecycleCallbacks : RetenoLifecycleCallbacks {
        val events = mutableListOf<Pair<String, Activity>>()

        override fun start(activity: Activity) {
            events += "start" to activity
        }

        override fun resume(activity: Activity) {
            events += "resume" to activity
        }

        override fun pause(activity: Activity) {
            events += "pause" to activity
        }

        override fun stop(activity: Activity) {
            events += "stop" to activity
        }
    }

    @Test
    fun `should notify registered callbacks when activity starts`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)
        fixture.controller.create()

        whenn()
        fixture.controller.start()

        then()
        assertEquals(listOf("start" to fixture.activity), fixture.callbacks.events)
    }

    @Test
    fun `should notify registered callbacks when activity resumes`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)
        fixture.controller.create().start()

        whenn()
        fixture.controller.resume()

        then()
        val expected = listOf("start" to fixture.activity, "resume" to fixture.activity)
        assertEquals(expected, fixture.callbacks.events)
    }

    @Test
    fun `should replay start and resume of current activity to callbacks registered later`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.controller.setup()

        whenn()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)

        then()
        val expected = listOf("start" to fixture.activity, "resume" to fixture.activity)
        assertEquals(expected, fixture.callbacks.events)
    }

    @Test
    fun `should not replay a stopped activity to callbacks registered later`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.controller.setup().pause().stop()

        whenn()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)

        then()
        assertTrue(fixture.callbacks.events.isEmpty())
    }

    @Test
    fun `should stop notifying callbacks once they are unregistered`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)
        fixture.sut.unregisterActivityLifecycleCallbacks(KEY)

        whenn()
        fixture.controller.setup()

        then()
        assertTrue(fixture.callbacks.events.isEmpty())
    }

    private companion object {
        const val KEY = "screen-watcher"
    }
}
