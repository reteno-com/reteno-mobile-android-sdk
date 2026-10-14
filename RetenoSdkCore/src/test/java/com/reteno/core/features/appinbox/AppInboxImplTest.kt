package com.reteno.core.features.appinbox

import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AppInboxImplTest {

    private class SutFixture(osSupported: Boolean = true) {
        val actions = FakeSdkActions()
        val sut = AppInboxImpl(actions) { osSupported }

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
    fun `should ignore inbox calls on unsupported os`() = runUnitTest {
        given()
        val fixture = SutFixture(osSupported = false)

        whenn()
        fixture.callEveryApi()

        then()
        assertTrue(fixture.actions.calls.isEmpty())
    }
}
