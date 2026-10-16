package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.FakeInAppScreen
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.RecordingInAppLifecycleCallback
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.iam.DisplayOffer
import com.reteno.core.internal.action.iam.LoadInAppHtml
import com.reteno.core.internal.action.iam.LoadPushInAppHtml
import com.reteno.core.internal.action.iam.ReportWidgetFailure
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppPresenterTest {

    private class SutFixture(private val scope: TestScope) {
        val screen = FakeInAppScreen()
        val dispatchers = scope.testDispatchers()
        val callback = RecordingInAppLifecycleCallback(dispatchers.markedMain)
        val loadHtml = mockk<LoadInAppHtml>()
        val loadPushHtml = mockk<LoadPushInAppHtml>()
        val reportFailure = mockk<ReportWidgetFailure>()
        val inAppInteractions = mutableListOf<InAppInteraction>()
        val clicks = mutableListOf<Triple<String, InteractionAction, Boolean>>()
        val shown = mutableListOf<Long>()
        val remoteLog = FakeRemoteLog()
        private var nextId = 0
        val sut = InAppPresenter(
            screen = screen,
            loadHtml = loadHtml,
            loadPushHtml = loadPushHtml,
            reportFailure = reportFailure,
            interactions = object : InteractionSubmitter {
                override fun submitInAppInteraction(interaction: InAppInteraction) {
                    inAppInteractions += interaction
                }

                override fun submitInteractionClick(
                    interactionId: String,
                    action: InteractionAction,
                    forcePush: Boolean
                ) {
                    clicks += Triple(interactionId, action, forcePush)
                }

                override fun submitInAppShown(message: InAppMessage) {
                    shown += message.messageId
                }
            },
            remoteLog = remoteLog,
            scope = scope.workScope(),
            dispatchers = dispatchers,
            newId = { "generated-${++nextId}" }
        )

        init {
            coEvery { loadHtml(any()) } returns Result.success(HTML)
            coEvery { loadPushHtml(any()) } returns Result.success(HTML)
            coEvery { reportFailure(any(), any()) } returns Result.success(Unit)
            sut.setInAppLifecycleCallback(callback)
        }

        fun started() = apply {
            sut.start()
            scope.runCurrent()
        }

        fun showing(message: InAppMessage = MESSAGE) = apply {
            started()
            scope.launch { sut.show(message) }
            scope.runCurrent()
        }

        fun displayed(message: InAppMessage = MESSAGE) = apply {
            showing(message)
            widget(INIT_SUCCESS)
        }

        fun pushShowing(interactionId: String = PUSH_ID) = apply {
            started()
            sut.initialize(interactionId)
            scope.runCurrent()
        }

        fun widget(message: String) {
            screen.postWidgetMessage(message)
            scope.runCurrent()
        }
    }

    @Test
    fun `should hold in-app offered before start until start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.show(MESSAGE)
        runCurrent()
        val createdBeforeStart = fixture.screen.calls.toList()

        whenn()
        fixture.sut.start()
        runCurrent()

        then()
        assertTrue(createdBeforeStart.isEmpty())
        assertEquals(listOf("create html-1"), fixture.screen.calls)
    }

    @Test
    fun `should answer that in-app offered to a free screen is loading`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        val offer = fixture.sut.show(MESSAGE)

        then()
        assertEquals(DisplayOffer.LOADING, offer)
    }

    @Test
    fun `should answer that in-app offered before start waits for the app`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val offer = fixture.sut.show(MESSAGE)

        then()
        assertEquals(DisplayOffer.WAITING_FOR_APP, offer)
    }

    @Test
    fun `should answer that in-app offered while another one is on screen is skipped`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        val offer = fixture.sut.show(stubInAppMessage(messageId = 2))

        then()
        assertEquals(DisplayOffer.SKIPPED, offer)
    }

    @Test
    fun `should announce in-app to host on main before loading it`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.sut.show(MESSAGE)
        runCurrent()

        then()
        assertEquals(listOf("beforeDisplay DISPLAY_RULES 11"), fixture.callback.calls)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
        coVerify(exactly = 1) { fixture.loadHtml(MESSAGE) }
    }

    @Test
    fun `should create in-app screen from loaded html`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.sut.show(MESSAGE)
        runCurrent()

        then()
        assertEquals(listOf("create html-1"), fixture.screen.calls)
    }

    @Test
    fun `should ignore in-app offered while another one is on screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.sut.show(stubInAppMessage(messageId = 2))
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.loadHtml(any()) }
    }

    @Test
    fun `should show in-app given through view api`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.sut.initialize(MESSAGE)
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.loadHtml(MESSAGE) }
    }

    @Test
    fun `should free the screen when html cannot be loaded`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        coEvery { fixture.loadHtml(MESSAGE) } returns Result.failure(IllegalStateException("no file"))
        fixture.sut.show(MESSAGE)
        runCurrent()

        whenn()
        fixture.sut.show(stubInAppMessage(messageId = 2))
        runCurrent()

        then()
        coVerify(exactly = 2) { fixture.loadHtml(any()) }
        assertTrue("destroy" in fixture.screen.calls)
    }

    @Test
    fun `should free the screen when in-app screen cannot be created`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.screen.creates = false

        whenn()
        fixture.sut.show(MESSAGE)
        runCurrent()

        then()
        assertEquals(listOf("create html-1", "destroy"), fixture.screen.calls)
    }

    @Test
    fun `should present in-app once widget is displayed`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertTrue("present" in fixture.screen.calls)
        assertTrue(fixture.sut.isViewShown())
    }

    @Test
    fun `should tell host in-app is displayed on main`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertEquals("onDisplay DISPLAY_RULES 11", fixture.callback.calls.last())
        assertTrue(fixture.callback.calledOnMain.all { it })
    }

    @Test
    fun `should count show once widget is displayed`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertEquals(listOf(1L), fixture.shown)
    }

    @Test
    fun `should not count show of in-app whose widget never displays`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.showing()

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should record opened interaction with a new id once widget is displayed`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        val opened = fixture.inAppInteractions.single()
        assertEquals("generated-1", opened.interactionId)
        assertEquals(11L, opened.messageInstanceId)
        assertEquals(InAppInteractionStatus.OPENED, opened.status)
    }

    @Test
    fun `should size in-app to widget content height`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"WIDGET_INIT_SUCCESS","payload":{"contentHeight":"120px"}}""")

        then()
        assertTrue("resize 120" in fixture.screen.calls)
    }

    @Test
    fun `should ignore content height without digits`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"WIDGET_INIT_SUCCESS","payload":{"contentHeight":"auto"}}""")

        then()
        assertTrue(fixture.screen.calls.none { it.startsWith("resize") })
    }

    @Test
    fun `should wait for activity to be ready before presenting`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.canPresent = false
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.screen.canPresent = true
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS)
        runCurrent()

        then()
        assertEquals(1, fixture.screen.calls.count { it == "present" })
    }

    @Test
    fun `should close in-app that was never presented when activity never becomes ready`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.canPresent = false
        fixture.widget(INIT_SUCCESS)

        whenn()
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS * InAppPresenter.READY_CHECK_ATTEMPTS)
        runCurrent()

        then()
        assertTrue("present" !in fixture.screen.calls)
        assertEquals("destroy", fixture.screen.calls.last())
        assertEquals(
            listOf("beforeClose DISPLAY_RULES 11 DISMISSED", "afterClose DISPLAY_RULES 11 DISMISSED"),
            fixture.callback.calls.takeLast(2)
        )
    }

    @Test
    fun `should keep presented in-app for a later activity when the next one never becomes ready`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()
        fixture.screen.canPresent = false
        fixture.screen.events!!.onActivityStarted()
        runCurrent()

        whenn()
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS * InAppPresenter.READY_CHECK_ATTEMPTS)
        runCurrent()

        then()
        assertTrue("destroy" !in fixture.screen.calls)
        assertTrue(fixture.sut.isViewShown())
    }

    @Test
    fun `should try presenting again when screen fails to present`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.presents = false
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.screen.presents = true
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS)
        runCurrent()

        then()
        assertEquals(2, fixture.screen.calls.count { it == "present" })
        assertTrue(fixture.sut.isViewShown())
    }

    @Test
    fun `should close in-app the screen never manages to present`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.presents = false
        fixture.widget(INIT_SUCCESS)

        whenn()
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS * InAppPresenter.READY_CHECK_ATTEMPTS)
        runCurrent()

        then()
        assertEquals("destroy", fixture.screen.calls.last())
        assertEquals("afterClose DISPLAY_RULES 11 DISMISSED", fixture.callback.calls.last())
    }

    @Test
    fun `should not mark in-app shown when screen cannot present it`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.presents = false

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertFalse(fixture.sut.isViewShown())
    }

    @Test
    fun `should stop waiting for activity when paused`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.screen.canPresent = false
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.sut.pause()
        fixture.screen.canPresent = true
        advanceTimeBy(InAppPresenter.READY_CHECK_DELAY_MILLIS * 2)
        runCurrent()

        then()
        assertTrue("present" !in fixture.screen.calls)
    }

    @Test
    fun `should hide in-app when its activity stops`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.screen.events!!.onActivityStopped()
        runCurrent()

        then()
        assertEquals("hide", fixture.screen.calls.last())
    }

    @Test
    fun `should present shown in-app again when an activity starts`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.screen.events!!.onActivityStarted()
        runCurrent()

        then()
        assertEquals(2, fixture.screen.calls.count { it == "present" })
    }

    @Test
    fun `should not present anything when an activity starts and nothing is shown`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.screen.events!!.onActivityStarted()
        runCurrent()

        then()
        assertTrue(fixture.screen.calls.isEmpty())
    }

    @Test
    fun `should record click on in-app link with opened interaction id`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"OPEN_URL","payload":{"targetComponentId":"btn","url":"https://example.com"}}""")

        then()
        val (interactionId, action, forcePush) = fixture.clicks.single()
        assertEquals("generated-1", interactionId)
        assertEquals(InteractionAction("OPEN_URL", "btn", "https://example.com"), action)
        assertTrue(forcePush)
    }

    @Test
    fun `should open in-app link and free the screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"url":"https://example.com"}}""")

        then()
        assertEquals(listOf("open https://example.com", "destroy"), fixture.screen.calls.takeLast(2))
        assertFalse(fixture.sut.isViewShown())
    }

    @Test
    fun `should not open blank in-app link`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"url":" "}}""")

        then()
        assertTrue(fixture.screen.calls.none { it.startsWith("open") })
    }

    @Test
    fun `should only free the screen for a click without payload`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK"}""")

        then()
        assertEquals("destroy", fixture.screen.calls.last())
        assertTrue(fixture.screen.calls.none { it.startsWith("open") })
    }

    @Test
    fun `should send custom data to host instead of opening link`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget(
            """{"type":"CLICK","payload":{"url":"https://example.com","customData":{"coupon":"SALE"}}}"""
        )

        then()
        val expected = mapOf(
            "url" to "https://example.com",
            "inapp_source" to "DISPLAY_RULES",
            "inapp_id" to "1",
            "coupon" to "SALE"
        )
        assertEquals(expected, fixture.screen.customData.single())
        assertTrue(fixture.screen.calls.none { it.startsWith("open") })
    }

    @Test
    fun `should open link when no host receives custom data`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()
        fixture.screen.deliversCustomData = false

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"url":"https://example.com","customData":{"coupon":"SALE"}}}""")

        then()
        assertTrue("open https://example.com" in fixture.screen.calls)
    }

    @Test
    fun `should send custom data without link`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"customData":{"coupon":"SALE"}}}""")

        then()
        assertEquals(
            mapOf("inapp_source" to "DISPLAY_RULES", "inapp_id" to "1", "coupon" to "SALE"),
            fixture.screen.customData.single()
        )
    }

    @Test
    fun `should tell host about closing with close button`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLOSE_WIDGET"}""")

        then()
        assertEquals(
            listOf("beforeClose DISPLAY_RULES 11 CLOSE_BUTTON", "afterClose DISPLAY_RULES 11 CLOSE_BUTTON"),
            fixture.callback.calls.takeLast(2)
        )
        assertEquals("destroy", fixture.screen.calls.last())
    }

    @Test
    fun `should tell host about in-app dismissed by user`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.screen.dismiss()
        runCurrent()

        then()
        assertEquals(
            listOf("beforeClose DISPLAY_RULES 11 DISMISSED", "afterClose DISPLAY_RULES 11 DISMISSED"),
            fixture.callback.calls.takeLast(2)
        )
    }

    @Test
    fun `should report widget failure and record failed interaction`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"WIDGET_INIT_FAILED","payload":{"reason":"script error"}}""")

        then()
        coVerify(exactly = 1) {
            fixture.reportFailure("11", match<IamJsEvent> { it.payload?.reason == "script error" })
        }
        val failed = fixture.inAppInteractions.single()
        assertEquals(InAppInteractionStatus.FAILED, failed.status)
        assertEquals("script error", failed.statusDescription)
        assertEquals("destroy", fixture.screen.calls.last())
    }

    @Test
    fun `should tell host about widget failure on main`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"WIDGET_RUNTIME_ERROR"}""")

        then()
        assertEquals("onError DISPLAY_RULES 11 Failed to load In-App message.", fixture.callback.calls.last())
        assertTrue(fixture.callback.calledOnMain.all { it })
    }

    @Test
    fun `should ignore widget messages it cannot read`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("{not json")
        fixture.widget("""{"type":"UNKNOWN"}""")
        fixture.screen.postWidgetMessage(null)
        runCurrent()

        then()
        assertEquals(listOf("create html-1"), fixture.screen.calls)
    }

    @Test
    fun `should ignore widget messages when nothing is on screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.widget(INIT_SUCCESS)
        fixture.widget("""{"type":"CLOSE_WIDGET"}""")

        then()
        assertTrue(fixture.screen.calls.isEmpty())
        assertTrue(fixture.callback.calls.isEmpty())
    }

    @Test
    fun `should report widget runtime error under opened interaction id`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"WIDGET_RUNTIME_ERROR"}""")

        then()
        coVerify(exactly = 1) { fixture.reportFailure("generated-1", any()) }
        assertEquals("generated-2", fixture.inAppInteractions.last().interactionId)
    }

    @Test
    fun `should not record click on in-app link before widget is displayed`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"url":"https://example.com"}}""")

        then()
        assertTrue(fixture.clicks.isEmpty())
        assertTrue("open https://example.com" in fixture.screen.calls)
    }

    @Test
    fun `should open link when custom data is empty`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"url":"https://example.com","customData":{}}}""")

        then()
        assertTrue(fixture.screen.customData.isEmpty())
        assertTrue("open https://example.com" in fixture.screen.calls)
    }

    @Test
    fun `should not open link for a click without url`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"targetComponentId":"btn"}}""")

        then()
        assertTrue(fixture.screen.calls.none { it.startsWith("open") })
        assertEquals("destroy", fixture.screen.calls.last())
    }

    @Test
    fun `should not present in-app when an activity starts before its widget is displayed`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.screen.events!!.onActivityStarted()
        runCurrent()

        then()
        assertTrue("present" !in fixture.screen.calls)
    }

    @Test
    fun `should keep size of in-app when widget gives no content height`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()

        whenn()
        fixture.widget("""{"type":"WIDGET_INIT_SUCCESS","payload":{}}""")

        then()
        assertTrue(fixture.screen.calls.none { it.startsWith("resize") })
        assertTrue("present" in fixture.screen.calls)
    }

    @Test
    fun `should wait for start before revealing in-app displayed in background`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.sut.pause()
        runCurrent()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertTrue("present" !in fixture.screen.calls)
        assertTrue(fixture.callback.calls.none { it.startsWith("onDisplay") })
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should reveal in-app displayed in background once started`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.sut.pause()
        runCurrent()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.started()

        then()
        assertTrue("present" in fixture.screen.calls)
        assertEquals(listOf(1L), fixture.shown)
    }

    @Test
    fun `should reveal in-app displayed in background only once`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.sut.pause()
        runCurrent()
        fixture.widget(INIT_SUCCESS)
        fixture.started()

        whenn()
        fixture.sut.pause()
        fixture.started()

        then()
        assertEquals(listOf(1L), fixture.shown)
    }

    @Test
    fun `should not reveal in-app waiting for start after it was replaced`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        fixture.sut.pause()
        runCurrent()
        fixture.widget(INIT_SUCCESS)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        whenn()
        fixture.started()

        then()
        assertTrue(fixture.shown.isEmpty())
        assertTrue(fixture.callback.calls.none { it.startsWith("onDisplay") })
    }

    @Test
    fun `should keep in-app offered in background while a push in-app takes the screen on start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.show(MESSAGE)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.started()

        then()
        coVerify(exactly = 0) { fixture.loadHtml(any()) }
        assertTrue("present" in fixture.screen.calls)
    }

    @Test
    fun `should show in-app offered in background once the push in-app shown on start closes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.show(MESSAGE)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()
        fixture.widget(INIT_SUCCESS)
        fixture.started()

        whenn()
        fixture.widget("""{"type":"CLOSE_WIDGET"}""")

        then()
        coVerify(exactly = 1) { fixture.loadHtml(MESSAGE) }
    }

    @Test
    fun `should keep in-app offered in background while a newer push in-app replaces the one on screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.show(MESSAGE)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()
        fixture.widget(INIT_SUCCESS)
        fixture.started()

        whenn()
        fixture.sut.initialize("push-2")
        runCurrent()

        then()
        coVerify(exactly = 0) { fixture.loadHtml(any()) }
        coVerify(exactly = 1) { fixture.loadPushHtml("push-2") }
    }

    @Test
    fun `should keep in-app offered in background when a push in-app fails before start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.loadPushHtml(PUSH_ID) } returns Result.failure(IllegalStateException("offline"))
        fixture.sut.show(MESSAGE)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()
        val loadedBeforeStart = fixture.screen.calls.count { it.startsWith("create") }

        whenn()
        fixture.started()

        then()
        assertEquals(0, loadedBeforeStart)
        coVerify(exactly = 1) { fixture.loadHtml(MESSAGE) }
    }

    @Test
    fun `should ignore widget messages from an in-app that was replaced`() = runUnitTest {
        given()
        val fixture = SutFixture(this).showing()
        coEvery { fixture.loadPushHtml(PUSH_ID) } returns Result.success(PUSH_HTML)
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        whenn()
        fixture.screen.events!!.onWidgetMessage(HTML.id, INIT_SUCCESS)
        fixture.screen.events!!.onDismissed(HTML.id)
        runCurrent()

        then()
        assertTrue("present" !in fixture.screen.calls)
        assertEquals(listOf("create html-1", "destroy", "create html-2"), fixture.screen.calls)
    }

    @Test
    fun `should load in-app when host fails before display`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.callback.failing += "beforeDisplay"

        whenn()
        fixture.sut.show(MESSAGE)
        runCurrent()

        then()
        assertEquals(listOf("create html-1"), fixture.screen.calls)
        assertEquals(
            listOf("In-app lifecycle callback beforeDisplay failed: java.lang.IllegalStateException"),
            fixture.remoteLog.entries.map { it.first }
        )
    }

    @Test
    fun `should free the screen for the next in-app when host fails while closing`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()
        fixture.callback.failing += setOf("beforeClose", "afterClose")
        fixture.widget("""{"type":"CLOSE_WIDGET"}""")

        whenn()
        fixture.sut.show(stubInAppMessage(messageId = 2))
        runCurrent()

        then()
        coVerify(exactly = 2) { fixture.loadHtml(any()) }
        assertEquals(2, fixture.remoteLog.entries.size)
    }

    @Test
    fun `should load push in-app announced to host`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        assertEquals(listOf("beforeDisplay PUSH_NOTIFICATION push-1"), fixture.callback.calls)
        coVerify(exactly = 1) { fixture.loadPushHtml(PUSH_ID) }
        assertEquals(listOf("create html-1"), fixture.screen.calls)
    }

    @Test
    fun `should replace in-app on screen with push in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        assertEquals(listOf("destroy", "create html-1"), fixture.screen.calls.takeLast(2))
    }

    @Test
    fun `should not count show of push in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this).pushShowing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertTrue(fixture.shown.isEmpty())
        assertTrue(fixture.inAppInteractions.isEmpty())
    }

    @Test
    fun `should hold displayed push in-app while push in-apps are paused`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)
        fixture.pushShowing()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertTrue("present" !in fixture.screen.calls)
    }

    @Test
    fun `should present held push in-app when push in-apps resume`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)
        fixture.pushShowing()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.sut.pauseIncomingPushInApps(false)
        runCurrent()

        then()
        assertTrue("present" in fixture.screen.calls)
    }

    @Test
    fun `should drop push in-app displayed while paused with skip behaviour`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.pushShowing()
        fixture.sut.pauseIncomingPushInApps(true)
        runCurrent()

        whenn()
        fixture.widget(INIT_SUCCESS)

        then()
        assertTrue("present" !in fixture.screen.calls)
        assertEquals("destroy", fixture.screen.calls.last())
    }

    @Test
    fun `should keep held push in-app when postpone behaviour is chosen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)
        fixture.pushShowing()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.POSTPONE_IN_APPS)
        fixture.sut.pauseIncomingPushInApps(false)
        runCurrent()

        then()
        assertTrue("destroy" !in fixture.screen.calls)
        assertTrue("present" in fixture.screen.calls)
    }

    @Test
    fun `should not reveal held push in-app replaced by a newer one when push in-apps resume`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)
        fixture.pushShowing()
        fixture.widget(INIT_SUCCESS)
        fixture.sut.initialize("push-2")
        runCurrent()

        whenn()
        fixture.sut.pauseIncomingPushInApps(false)
        runCurrent()

        then()
        assertTrue(fixture.callback.calls.none { it.startsWith("onDisplay") })
        assertTrue("present" !in fixture.screen.calls)
    }

    @Test
    fun `should do nothing when push in-apps resume without held in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.sut.pauseIncomingPushInApps(true)

        whenn()
        fixture.sut.pauseIncomingPushInApps(false)
        runCurrent()

        then()
        assertTrue(fixture.screen.calls.isEmpty())
    }

    @Test
    fun `should skip push in-apps entirely while paused with skip behaviour`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.sut.pauseIncomingPushInApps(true)

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        coVerify(exactly = 0) { fixture.loadPushHtml(any()) }
    }

    @Test
    fun `should load push in-app while paused with postpone behaviour`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.sut.pauseIncomingPushInApps(true)

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.loadPushHtml(PUSH_ID) }
    }

    @Test
    fun `should load push in-app with skip behaviour while not paused`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.loadPushHtml(PUSH_ID) }
    }

    @Test
    fun `should keep presenting display rule in-apps while push in-apps are paused`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)

        whenn()
        fixture.displayed()

        then()
        assertTrue("present" in fixture.screen.calls)
        assertTrue(fixture.sut.isViewShown())
    }

    @Test
    fun `should drop held push in-app when skip behaviour is chosen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseIncomingPushInApps(true)
        fixture.pushShowing()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        runCurrent()

        then()
        assertEquals("destroy", fixture.screen.calls.last())
    }

    @Test
    fun `should keep screen when skip behaviour is chosen without held in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this).displayed()

        whenn()
        fixture.sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        runCurrent()

        then()
        assertTrue("destroy" !in fixture.screen.calls)
    }

    @Test
    fun `should identify push in-app by interaction id in custom data and clicks`() = runUnitTest {
        given()
        val fixture = SutFixture(this).pushShowing()
        fixture.widget(INIT_SUCCESS)

        whenn()
        fixture.widget("""{"type":"CLICK","payload":{"customData":{"coupon":"SALE"}}}""")

        then()
        assertEquals(PUSH_ID, fixture.clicks.single().first)
        assertEquals(
            mapOf("inapp_source" to "PUSH_NOTIFICATION", "inapp_id" to PUSH_ID, "coupon" to "SALE"),
            fixture.screen.customData.single()
        )
    }

    @Test
    fun `should report push widget failure under interaction id without failed interaction`() = runUnitTest {
        given()
        val fixture = SutFixture(this).pushShowing()

        whenn()
        fixture.widget("""{"type":"WIDGET_INIT_FAILED"}""")

        then()
        coVerify(exactly = 1) { fixture.reportFailure(PUSH_ID, any()) }
        assertTrue(fixture.inAppInteractions.isEmpty())
    }

    @Test
    fun `should free the screen when push html cannot be loaded`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        coEvery { fixture.loadPushHtml(PUSH_ID) } returns Result.failure(IllegalStateException("timeout"))

        whenn()
        fixture.sut.initialize(PUSH_ID)
        runCurrent()

        then()
        assertEquals(listOf("destroy"), fixture.screen.calls)
    }

    @Test
    fun `should stop calling removed lifecycle callback`() = runUnitTest {
        given()
        val fixture = SutFixture(this).started()
        fixture.sut.setInAppLifecycleCallback(null)

        whenn()
        fixture.sut.show(MESSAGE)
        runCurrent()

        then()
        assertTrue(fixture.callback.calls.isEmpty())
    }

    private companion object {
        const val PUSH_ID = "push-1"
        const val INIT_SUCCESS = """{"type":"WIDGET_INIT_SUCCESS"}"""
        val MESSAGE = stubInAppMessage(messageId = 1).copy(messageInstanceId = 11)
        val HTML = IamFetchResult("html-1", "<html/>", InAppLayoutType.FULL, InAppLayoutParams(Position.TOP))
        val PUSH_HTML = HTML.copy(id = "html-2")
    }
}
