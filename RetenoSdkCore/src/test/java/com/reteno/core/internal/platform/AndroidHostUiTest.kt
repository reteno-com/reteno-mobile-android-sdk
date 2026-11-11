package com.reteno.core.internal.platform

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.view.iam.AndroidInAppScreen
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidHostUiTest {

    class EnglishActivity : Activity() {
        override fun attachBaseContext(newBase: Context) {
            val configuration = Configuration(newBase.resources.configuration)
            configuration.setLocale(Locale.forLanguageTag("en-US"))
            super.attachBaseContext(newBase.createConfigurationContext(configuration))
        }
    }

    private class SutFixture(context: Context? = null) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ManualLifecycleOwner()
        val activityHelper = mockk<RetenoActivityHelper>()
        val sut = AndroidHostUi(context ?: application, owner, activityHelper, AppBroadcaster(application))

        init {
            justRun { activityHelper.registerActivityLifecycleCallbacks(any(), any()) }
        }
    }

    @Test
    fun `should track activities of the application behind the given context`() = runUnitTest {
        given()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val fixture = SutFixture(ContextWrapper(application))
        justRun { fixture.activityHelper.enableLifecycleCallbacks(any()) }

        whenn()
        fixture.sut.trackActivities()

        then()
        verify(exactly = 1) { fixture.activityHelper.enableLifecycleCallbacks(application) }
    }

    @Test
    fun `should register fragment screen and app lifecycle watchers`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val calls = mutableListOf<String>()

        whenn()
        fixture.sut.watchFragmentScreens { name, _ -> calls += name }
        fixture.sut.watchAppLifecycle(
            onForeground = { calls += "foreground" },
            onBackground = {},
            onActivityPaused = {}
        )
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        then()
        assertEquals(listOf("foreground"), calls)
        verify(exactly = 1) {
            fixture.activityHelper.registerActivityLifecycleCallbacks("FragmentScreenWatcher", any())
        }
        verify(exactly = 1) {
            fixture.activityHelper.registerActivityLifecycleCallbacks("AppLifecycleWatcher", any())
        }
    }

    @Test
    fun `should read package on top of activity task`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val taskInfo = ActivityManager.RecentTaskInfo().apply {
            topActivity = ComponentName("com.example.top", "com.example.top.Top")
        }
        val task = mockk<ActivityManager.AppTask>().also { every { it.taskInfo } returns taskInfo }
        val manager = mockk<ActivityManager>().also { every { it.appTasks } returns listOf(task) }
        val activity = mockk<Activity>().also {
            every { it.getSystemService(Context.ACTIVITY_SERVICE) } returns manager
        }

        whenn()
        val topPackage = fixture.sut.packageOnTop(activity)

        then()
        assertEquals("com.example.top", topPackage)
    }

    @Test
    fun `should return no top package when activity manager access is denied`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = mockk<Activity>().also {
            every { it.getSystemService(any()) } throws SecurityException("denied")
        }

        whenn()
        val topPackage = fixture.sut.packageOnTop(activity)

        then()
        assertNull(topPackage)
    }

    @Test
    fun `should show in-apps on android screen`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val screen = fixture.sut.inAppScreen()

        then()
        assertTrue(screen is AndroidInAppScreen)
    }

    @Test
    @Config(qualifiers = "uk-rUA")
    fun `should read the locale the visible activity shows`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = Robolectric.buildActivity(EnglishActivity::class.java).setup().get()
        every { fixture.activityHelper.currentActivity } returns activity

        whenn()
        val locale = fixture.sut.displayedLocale()

        then()
        assertEquals("en-US", locale?.toLanguageTag())
    }

    @Test
    fun `should read no displayed locale without a visible activity`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.activityHelper.currentActivity } returns null

        whenn()
        val locale = fixture.sut.displayedLocale()

        then()
        assertNull(locale)
    }

    @Test
    fun `should report only resumed activities to the resume watcher`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callbacks = slot<RetenoLifecycleCallbacks>()
        justRun { fixture.activityHelper.registerActivityLifecycleCallbacks(any(), capture(callbacks)) }
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val events = mutableListOf<String>()
        fixture.sut.watchResumedActivities { events += "resumed" }

        whenn()
        callbacks.captured.start(activity)
        callbacks.captured.resume(activity)
        callbacks.captured.pause(activity)
        callbacks.captured.stop(activity)

        then()
        assertEquals(listOf("resumed"), events)
    }

    @Test
    fun `should stop the resume watcher it registered`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val key = slot<String>()
        justRun { fixture.activityHelper.registerActivityLifecycleCallbacks(capture(key), any()) }
        justRun { fixture.activityHelper.unregisterActivityLifecycleCallbacks(any()) }
        fixture.sut.watchResumedActivities {}

        whenn()
        fixture.sut.stopWatchingResumedActivities()

        then()
        verify(exactly = 1) { fixture.activityHelper.unregisterActivityLifecycleCallbacks(key.captured) }
    }
}
