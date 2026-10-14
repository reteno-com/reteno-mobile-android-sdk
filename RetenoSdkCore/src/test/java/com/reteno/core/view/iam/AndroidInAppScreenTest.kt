package com.reteno.core.view.iam

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.internal.iam.InAppScreenEvents
import com.reteno.core.internal.platform.AppBroadcaster
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.util.Constants
import com.reteno.core.view.iam.container.IamContainer
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidInAppScreenTest {

    private class RecordingEvents : InAppScreenEvents {
        val calls = mutableListOf<String>()

        override fun onWidgetMessage(htmlId: String, message: String?) {
            calls += "message $htmlId $message"
        }

        override fun onDismissed(htmlId: String) {
            calls += "dismissed $htmlId"
        }

        override fun onActivityStarted() {
            calls += "started"
        }

        override fun onActivityStopped() {
            calls += "stopped"
        }
    }

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val activityHelper = mockk<RetenoActivityHelper>(relaxed = true)
        val events = RecordingEvents()
        val container = mockk<IamContainer>(relaxed = true)
        val bridge = slot<RetenoAndroidHandler>()
        val dismissListener = slot<IamContainer.IamDismissListener>()
        val lifecycle = slot<RetenoLifecycleCallbacks>()
        val sut = AndroidInAppScreen(application, activityHelper, AppBroadcaster(application))

        init {
            every { activityHelper.registerActivityLifecycleCallbacks(any(), capture(lifecycle)) } returns Unit
            sut.attach(events)
        }

        fun <T> withContainer(block: () -> T): T {
            mockkObject(IamContainer.Companion)
            try {
                every {
                    IamContainer.create(any(), any(), capture(bridge), capture(dismissListener))
                } returns container
                return block()
            } finally {
                unmockkObject(IamContainer.Companion)
            }
        }

        fun created() = apply {
            withContainer { sut.create(HTML) }
        }
    }

    @Test
    fun `should create in-app container for html`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val created = fixture.withContainer { fixture.sut.create(HTML) }

        then()
        assertTrue(created)
    }

    @Test
    fun `should report container that cannot be created`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val created = fixture.withContainer {
            every { IamContainer.create(any(), any(), any(), any()) } throws IllegalStateException("no webview")
            fixture.sut.create(HTML)
        }

        then()
        assertFalse(created)
    }

    @Test
    fun `should pass widget messages to events`() = runUnitTest {
        given()
        val fixture = SutFixture().created()

        whenn()
        fixture.bridge.captured.postMessage("""{"type":"CLOSE_WIDGET"}""")

        then()
        assertEquals(listOf("""message html-1 {"type":"CLOSE_WIDGET"}"""), fixture.events.calls)
    }

    @Test
    fun `should tell widget messages of each container apart`() = runUnitTest {
        given()
        val fixture = SutFixture().created()
        val firstBridge = fixture.bridge.captured
        fixture.withContainer { fixture.sut.create(HTML.copy(id = "html-2")) }

        whenn()
        firstBridge.postMessage("late")
        fixture.bridge.captured.postMessage("current")

        then()
        assertEquals(listOf("message html-1 late", "message html-2 current"), fixture.events.calls)
    }

    @Test
    fun `should pass dismissal to events`() = runUnitTest {
        given()
        val fixture = SutFixture().created()

        whenn()
        fixture.dismissListener.captured.onIamDismissed()

        then()
        assertEquals(listOf("dismissed html-1"), fixture.events.calls)
    }

    @Test
    fun `should pass started activity to events`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.lifecycle.captured.start(mockk())

        then()
        assertEquals(listOf("started"), fixture.events.calls)
    }

    @Test
    fun `should pass only stop of current activity to events`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val current = mockk<Activity>()
        every { fixture.activityHelper.currentActivity } returns current

        whenn()
        fixture.lifecycle.captured.stop(mockk())
        fixture.lifecycle.captured.stop(current)

        then()
        assertEquals(listOf("stopped"), fixture.events.calls)
    }

    @Test
    fun `should present only when activity can show messages and is ready`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.activityHelper.canPresentMessages() } returnsMany listOf(false, true, true)
        every { fixture.activityHelper.isActivityFullyReady() } returnsMany listOf(false, true)

        whenn()
        val answers = listOf(fixture.sut.canPresent(), fixture.sut.canPresent(), fixture.sut.canPresent())

        then()
        assertEquals(listOf(false, false, true), answers)
    }

    @Test
    fun `should show container on current activity`() = runUnitTest {
        given()
        val fixture = SutFixture().created()
        val activity = mockk<Activity>()
        every { fixture.activityHelper.currentActivity } returns activity

        whenn()
        val presented = fixture.sut.present()

        then()
        assertTrue(presented)
        verify(exactly = 1) { fixture.container.show(activity) }
    }

    @Test
    fun `should not present without current activity`() = runUnitTest {
        given()
        val fixture = SutFixture().created()
        every { fixture.activityHelper.currentActivity } returns null

        whenn()
        val presented = fixture.sut.present()

        then()
        assertFalse(presented)
    }

    @Test
    fun `should not present without container`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.activityHelper.currentActivity } returns mockk()

        whenn()
        val presented = fixture.sut.present()

        then()
        assertFalse(presented)
    }

    @Test
    fun `should report container that cannot be shown`() = runUnitTest {
        given()
        val fixture = SutFixture().created()
        every { fixture.activityHelper.currentActivity } returns mockk()
        every { fixture.container.show(any()) } throws IllegalStateException("window gone")

        whenn()
        val presented = fixture.sut.present()

        then()
        assertFalse(presented)
    }

    @Test
    fun `should resize hide and destroy container`() = runUnitTest {
        given()
        val fixture = SutFixture().created()

        whenn()
        fixture.sut.resize(10)
        fixture.sut.hide()
        fixture.sut.destroy()
        fixture.sut.hide()

        then()
        verify(exactly = 1) { fixture.container.onHeightDefined(AndroidInAppScreen.dpToPx(10)) }
        verify(exactly = 1) { fixture.container.dismiss() }
        verify(exactly = 1) { fixture.container.destroy() }
    }

    @Test
    fun `should open link from current activity in new task`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = mockk<Activity>(relaxed = true)
        every { fixture.activityHelper.currentActivity } returns activity
        val opened = slot<Intent>()
        every { activity.startActivity(capture(opened)) } returns Unit

        whenn()
        fixture.sut.openLink("https://example.com/sale")

        then()
        assertEquals(Intent.ACTION_VIEW, opened.captured.action)
        assertEquals("https://example.com/sale", opened.captured.dataString)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, opened.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `should open link from application without current activity`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.activityHelper.currentActivity } returns null

        whenn()
        fixture.sut.openLink("https://example.com/sale")

        then()
        val opened = shadowOf(fixture.application).nextStartedActivity
        assertEquals("https://example.com/sale", opened.dataString)
    }

    @Test
    fun `should ignore link no app can open`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = mockk<Activity>()
        every { fixture.activityHelper.currentActivity } returns activity
        every { activity.startActivity(any()) } throws ActivityNotFoundException()

        whenn()
        fixture.sut.openLink("unknown://link")

        then()
        verify(exactly = 1) { activity.startActivity(any()) }
    }

    @Test
    fun `should ignore link the system refuses to open`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = mockk<Activity>()
        every { fixture.activityHelper.currentActivity } returns activity
        every { activity.startActivity(any()) } throws SecurityException("file uri exposed")

        whenn()
        fixture.sut.openLink("file:///data/secret.txt")

        then()
        verify(exactly = 1) { activity.startActivity(any()) }
    }

    @Test
    fun `should send custom data to receivers of the app`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val intent = Intent(Constants.BROADCAST_ACTION_CUSTOM_INAPP_DATA).setPackage(fixture.application.packageName)
        val receiver = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = fixture.application.packageName
                name = "com.example.InAppDataReceiver"
            }
        }
        shadowOf(fixture.application.packageManager).addResolveInfoForIntent(intent, receiver)

        whenn()
        val sent = fixture.sut.sendCustomData(mapOf("coupon" to "SALE"))

        then()
        assertTrue(sent)
        assertEquals("SALE", shadowOf(fixture.application).broadcastIntents.single().getStringExtra("coupon"))
    }

    @Test
    fun `should report custom data without receivers as not sent`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val sent = fixture.sut.sendCustomData(mapOf("coupon" to "SALE"))

        then()
        assertFalse(sent)
    }

    private companion object {
        val HTML = IamFetchResult("html-1", "<html/>", InAppLayoutType.FULL, InAppLayoutParams(Position.TOP))
    }
}
