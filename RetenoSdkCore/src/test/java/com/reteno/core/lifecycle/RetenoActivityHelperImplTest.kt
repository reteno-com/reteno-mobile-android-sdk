package com.reteno.core.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.installCapturing
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
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

    @Test
    fun `should notify registered callbacks when activity pauses and stops`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.controller.setup()
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)

        whenn()
        fixture.controller.pause().saveInstanceState(Bundle()).stop().destroy()

        then()
        val expected = listOf("start", "resume", "pause", "stop").map { it to fixture.activity }
        assertEquals(expected, fixture.callbacks.events)
        assertNull(fixture.sut.currentActivity)
    }

    @Test
    fun `should keep the current activity when another one stops`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val other = Robolectric.buildActivity(Activity::class.java).setup()
        fixture.controller.setup()

        whenn()
        other.pause().stop()

        then()
        assertSame(fixture.activity, fixture.sut.currentActivity)
    }

    @Test
    fun `should present messages only over a resumed activity that is not finishing or changing`() =
        runUnitTest {
            given()
            val fixture = SutFixture()
            val presentable = mutableListOf<Boolean>()

            whenn()
            presentable += fixture.sut.canPresentMessages()
            fixture.controller.setup()
            presentable += fixture.sut.canPresentMessages()
            fixture.sut.lifecycleCallbacks.onActivityPreCreated(fixture.activity, null)
            presentable += fixture.sut.canPresentMessages()
            fixture.controller.pause()
            presentable += fixture.sut.canPresentMessages()
            fixture.controller.resume()
            presentable += fixture.sut.canPresentMessages()
            fixture.activity.finish()
            presentable += fixture.sut.canPresentMessages()

            then()
            assertEquals(listOf(false, true, false, false, true, false), presentable)
        }

    @Test
    fun `should call an activity fully ready once its window is attached`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val before = fixture.sut.isActivityFullyReady()

        whenn()
        fixture.controller.setup()

        then()
        assertFalse(before)
        val decorView = fixture.activity.window.decorView
        val attached = decorView.applicationWindowToken != null && decorView.rootWindowInsets != null
        assertEquals(attached, fixture.sut.isActivityFullyReady())
    }

    @Test
    fun `should not call an activity fully ready before its window has insets`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val decorView = mockk<View> {
            every { applicationWindowToken } returns mockk<IBinder>()
            every { rootWindowInsets } returns null
        }
        val attaching = mockk<Activity>(relaxed = true) { every { window.decorView } returns decorView }
        val unattached = Robolectric.buildActivity(Activity::class.java).create().get()

        whenn()
        val ready = listOf(attaching, unattached).map {
            fixture.sut.lifecycleCallbacks.onActivityResumed(it)
            fixture.sut.isActivityFullyReady()
        }

        then()
        assertEquals(listOf(false, false), ready)
    }

    @Test
    fun `should keep notifying other callbacks when one fails`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val remoteLog = FakeRemoteLog()
        val failing = RetenoLifecycleCallBacksAdapter(onStart = { error("broken callback") })
        fixture.sut.registerActivityLifecycleCallbacks("failing", failing)
        fixture.sut.registerActivityLifecycleCallbacks(KEY, fixture.callbacks)
        fixture.controller.create()

        whenn()
        Logger.installCapturing(FakeLogSink(), remoteLog)
        try {
            fixture.controller.start()
        } finally {
            Logger.install(AndroidLogSink(), null)
        }

        then()
        assertEquals(listOf("start" to fixture.activity), fixture.callbacks.events)
        assertEquals(listOf("onActivityStarted(): "), remoteLog.errors.map { it.first })
    }

    @Test
    fun `should pass only the events an adapter asks for`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val seen = mutableListOf<String>()
        val all = RetenoLifecycleCallBacksAdapter(
            onPause = { seen += "pause" },
            onResume = { seen += "resume" },
            onStart = { seen += "start" },
            onStop = { seen += "stop" }
        )
        val none = RetenoLifecycleCallBacksAdapter()

        whenn()
        listOf(all, none).forEach {
            it.start(fixture.activity)
            it.resume(fixture.activity)
            it.pause(fixture.activity)
            it.stop(fixture.activity)
        }

        then()
        assertEquals(listOf("start", "resume", "pause", "stop"), seen)
    }

    private companion object {
        const val KEY = "screen-watcher"
    }
}
