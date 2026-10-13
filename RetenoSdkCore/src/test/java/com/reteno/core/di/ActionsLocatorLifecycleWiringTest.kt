package com.reteno.core.di

import android.app.Activity
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.internal.action.lifecycle.TrackPermissionDialogAction
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.Constants
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorLifecycleWiringTest {

    private class SutFixture(scope: TestScope) {
        val env = LocatorFixture(scope)

        init {
            every { env.configRepository.getAppVersion() } returns "1.0"
            every { env.configRepository.getAppBuildNumber() } returns 1L
        }
    }

    @Test
    fun `should run app foreground and background through lifecycle watcher in order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.sessionHandler.getForegroundTimeMillis() } returns 5_000L
        fixture.env.sut.lifecycleTracking.start()

        whenn()
        fixture.env.platform.onForeground!!.invoke()
        fixture.env.platform.onBackground!!.invoke()
        runCurrent()

        then()
        assertEquals(listOf("ApplicationOpened", "ApplicationBackgrounded"), fixture.env.insertedEventKeys)
        assertEquals(listOf(Constants.BROADCAST_ACTION_RETENO_APP_RESUME), fixture.env.platform.broadcasts)
        verify(exactly = 1) { fixture.env.iamController.preloadHtml() }
    }

    @Test
    fun `should schedule outdated data cleanup when app comes to foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.sut.lifecycleTracking.start()

        whenn()
        fixture.env.platform.onForeground!!.invoke()
        advanceUntilIdle()

        then()
        verify(exactly = 1) { fixture.env.eventsDb.deleteEventsByTime(any()) }
    }

    @Test
    fun `should log permission dialog shown over paused activity`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.platform.topPackage = TrackPermissionDialogAction.PERMISSION_DIALOG_PACKAGE
        fixture.env.sut.lifecycleTracking.start()

        whenn()
        fixture.env.platform.onActivityPaused!!.invoke(mockk<Activity>())
        runCurrent()

        then()
        assertEquals(listOf("PushNotificationsPermissionAsked"), fixture.env.insertedEventKeys)
    }

    @Test
    fun `should apply lifecycle tracking options to shared filter`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.sut.lifecycleTracking.start()

        whenn()
        fixture.env.sut.setLifecycleEventConfig(LifecycleTrackingOptions.NONE)
        fixture.env.platform.onForeground!!.invoke()
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.env.insertedEventKeys)
    }

    @Test
    fun `should log screen views reported by fragment watcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.env.sut.enableAutoScreenTracking(ScreenTrackingConfig(enable = true))
        fixture.env.platform.onScreen!!.invoke("HomeFragment", ScreenTrackingTrigger.ON_START)
        runCurrent()

        then()
        assertEquals(listOf("screenView"), fixture.env.insertedEventKeys)
    }

    @Test
    fun `should broadcast push permission change`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.env.sut.updatePushPermissionStatus()

        then()
        assertEquals(listOf(Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED), fixture.env.platform.broadcasts)
    }

    @Test
    fun `should log push subscription change with notification state`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.env.sut.updateNotificationsEnabled(true)

        then()
        assertEquals(listOf("PushNotificationsSubscribed"), fixture.env.insertedEventKeys)
    }
}
