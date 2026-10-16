package com.reteno.core.features.appinbox

import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class AppInboxImplTest {

    private class SutFixture {
        val actions = FakeSdkActions()
        val remoteLog = FakeRemoteLog()
        val sut = AppInboxImpl(actions, remoteLog)

        fun callEveryApi() {
            sut.getAppInboxMessages(2, 20, AppInboxStatus.OPENED, RecordingResultCallback<AppInboxMessages>())
            sut.getAppInboxMessagesCount(RecordingResultCallback())
            sut.subscribeOnMessagesCountChanged(RecordingResultCallback())
            sut.unsubscribeMessagesCountChanged(RecordingResultCallback())
            sut.unsubscribeAllMessagesCountChanged()
            sut.markAsOpened("m1")
            sut.markAllMessagesAsOpened(RecordingResultCallback())
        }
    }

    @Test
    fun `should pass every inbox call to sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.callEveryApi()

        then()
        assertEquals(
            listOf(
                "submitInboxMessagesRequest 2 20 OPENED",
                "submitInboxCountRequest",
                "subscribeInboxCount",
                "unsubscribeInboxCount",
                "unsubscribeAllInboxCount",
                "submitInboxMessageOpened m1",
                "submitAllInboxMessagesOpened"
            ),
            fixture.actions.calls
        )
    }

    @Test
    fun `should load inbox messages without paging or status filter when host gives none`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val inbox: AppInbox = fixture.sut

        whenn()
        inbox.getAppInboxMessages(callback = RecordingResultCallback())

        then()
        assertEquals(listOf("submitInboxMessagesRequest null null null"), fixture.actions.calls)
    }

    @Test
    fun `should forward the same callback when loading inbox messages`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingResultCallback<AppInboxMessages>()

        whenn()
        fixture.sut.getAppInboxMessages(1, 10, null, callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same callback when loading inbox messages count`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingResultCallback<Int>()

        whenn()
        fixture.sut.getAppInboxMessagesCount(callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same callback when subscribing to inbox messages count`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingResultCallback<Int>()

        whenn()
        fixture.sut.subscribeOnMessagesCountChanged(callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same callback when unsubscribing from inbox messages count`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingResultCallback<Int>()

        whenn()
        fixture.sut.unsubscribeMessagesCountChanged(callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should forward the same callback when marking all inbox messages as opened`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val callback = RecordingResultCallback<Unit>()

        whenn()
        fixture.sut.markAllMessagesAsOpened(callback)

        then()
        assertSame(callback, fixture.actions.callbacks.single())
    }

    @Test
    fun `should keep host running and report every inbox call that fails in sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.actions.failure = IllegalStateException("sdk broken")

        whenn()
        fixture.callEveryApi()

        then()
        val error = "IllegalStateException: sdk broken"
        assertEquals(
            listOf(
                "AppInbox.getAppInboxMessages failed: $error" to LogLevel.ERROR,
                "AppInbox.getAppInboxMessagesCount failed: $error" to LogLevel.ERROR,
                "AppInbox.subscribeOnMessagesCountChanged failed: $error" to LogLevel.ERROR,
                "AppInbox.unsubscribeMessagesCountChanged failed: $error" to LogLevel.ERROR,
                "AppInbox.unsubscribeAllMessagesCountChanged failed: $error" to LogLevel.ERROR,
                "AppInbox.markAsOpened failed: $error" to LogLevel.ERROR,
                "AppInbox.markAllMessagesAsOpened failed: $error" to LogLevel.ERROR
            ),
            fixture.remoteLog.entries
        )
    }
}
