package com.reteno.core.internal.platform

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.reteno.core.fixtures.onAndroidSdk
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class TopActivityTest {

    private fun activityWithTasks(tasks: List<ActivityManager.AppTask>): Activity {
        val manager = mockk<ActivityManager>()
        every { manager.appTasks } returns tasks
        return mockk<Activity>().also { every { it.getSystemService(Context.ACTIVITY_SERVICE) } returns manager }
    }

    private fun task(topPackage: String?): ActivityManager.AppTask {
        val info = ActivityManager.RecentTaskInfo().apply {
            topActivity = topPackage?.let { ComponentName(it, "$it.Top") }
        }
        return mockk<ActivityManager.AppTask>().also { every { it.taskInfo } returns info }
    }

    @Test
    fun `should return package of activity on top of the first task`() = runUnitTest {
        given()
        val activity = activityWithTasks(listOf(task("com.google.android.permissioncontroller")))

        whenn()
        val topPackage = TopActivity.packageOf(activity)

        then()
        assertEquals("com.google.android.permissioncontroller", topPackage)
    }

    @Test
    fun `should return null when there are no tasks`() = runUnitTest {
        given()
        val activity = activityWithTasks(emptyList())

        whenn()
        val topPackage = TopActivity.packageOf(activity)

        then()
        assertNull(topPackage)
    }

    @Test
    fun `should return null when task has no top activity`() = runUnitTest {
        given()
        val activity = activityWithTasks(listOf(task(null)))

        whenn()
        val topPackage = TopActivity.packageOf(activity)

        then()
        assertNull(topPackage)
    }

    @Test
    fun `should read top activity from running tasks before android m`() = runUnitTest {
        given()
        val manager = mockk<ActivityManager>()
        @Suppress("DEPRECATION")
        val running = ActivityManager.RunningTaskInfo().apply {
            topActivity = ComponentName("com.example.legacy", "com.example.legacy.Top")
        }
        @Suppress("DEPRECATION")
        every { manager.getRunningTasks(1) } returns listOf(running)
        val activity = mockk<Activity>().also {
            every { it.getSystemService(Context.ACTIVITY_SERVICE) } returns manager
        }

        whenn()
        val topPackage = onAndroidSdk(Build.VERSION_CODES.LOLLIPOP_MR1) { TopActivity.packageOf(activity) }

        then()
        assertEquals("com.example.legacy", topPackage)
    }

    @Test
    fun `should return null when activity manager is unavailable`() = runUnitTest {
        given()
        val activity = mockk<Activity>().also {
            every { it.getSystemService(Context.ACTIVITY_SERVICE) } throws SecurityException("denied")
        }

        whenn()
        val topPackage = TopActivity.packageOf(activity)

        then()
        assertNull(topPackage)
    }
}
