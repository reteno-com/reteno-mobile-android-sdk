package com.reteno.push.receiver

import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Procedure
import com.reteno.push.RetenoNotifications
import com.reteno.push.events.EventListener
import com.reteno.push.events.InAppCustomData
import com.reteno.push.fixtures.testApplication
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class HostEventReceiversTest {

    private class SutFixture {
        val application = testApplication()

        inline fun <T : Any> listening(events: EventListener<T>, block: (MutableList<T>) -> Unit) {
            val received = mutableListOf<T>()
            val listener = Procedure<T> { received += it }
            events.addListener(listener)
            try {
                block(received)
            } finally {
                events.removeListener(listener)
            }
        }
    }

    @Test
    fun `should notify bundle listeners of each push event`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val extras = Bundle().apply { putString("key", "value") }
        val cases = listOf(
            RetenoNotifications.received to PushReceivedReceiver(),
            RetenoNotifications.click to ReceiverNotificationClicked(),
            RetenoNotifications.close to ReceiverNotificationDeleted(),
            RetenoNotifications.custom to CustomPushReceiver()
        )
        val delivered = mutableListOf<String?>()

        whenn()
        cases.forEach { (events, receiver) ->
            fixture.listening(events) { received ->
                receiver.onReceive(fixture.application, Intent().putExtras(extras))
                delivered += received.single().getString("key")
            }
        }

        then()
        assertEquals(List(4) { "value" }, delivered)
    }

    @Test
    fun `should notify listeners with empty bundle when push event has no extras`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val receivers = listOf(
            PushReceivedReceiver(),
            ReceiverNotificationClicked(),
            ReceiverNotificationDeleted(),
            CustomPushReceiver()
        )
        val events = listOf(
            RetenoNotifications.received,
            RetenoNotifications.click,
            RetenoNotifications.close,
            RetenoNotifications.custom
        )
        val delivered = mutableListOf<Bundle>()

        whenn()
        receivers.zip(events).forEach { (receiver, listener) ->
            fixture.listening(listener) { received ->
                receiver.onReceive(fixture.application, Intent())
                delivered += received.single()
            }
        }

        then()
        assertTrue(delivered.all { it.isEmpty })
    }

    @Test
    fun `should not notify removed listener`() = runUnitTest {
        given()
        val fixture = SutFixture()
        var removedCalls = 0
        val removed = Procedure<Bundle> { removedCalls++ }
        RetenoNotifications.received.addListener(removed)
        RetenoNotifications.received.removeListener(removed)

        whenn()
        var keptCalls = 0
        fixture.listening(RetenoNotifications.received) { kept ->
            PushReceivedReceiver().onReceive(fixture.application, Intent())
            keptCalls = kept.size
        }

        then()
        assertEquals(1, keptCalls)
        assertEquals(0, removedCalls)
    }

    @Test
    fun `should pass in-app custom data to listeners`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val intent = Intent()
            .putExtra("url", "https://example.com")
            .putExtra("inapp_source", "PUSH_NOTIFICATION")
            .putExtra("inapp_id", "in-app-1")
            .putExtra("promo", "SALE")
            .putExtra("count", 3)

        whenn()
        var received: List<InAppCustomData> = emptyList()
        fixture.listening(RetenoNotifications.inAppCustomDataReceived) {
            ReceiverInAppData().onReceive(fixture.application, intent)
            received = it.toList()
        }

        then()
        val expected = InAppCustomData("https://example.com", "PUSH_NOTIFICATION", "in-app-1", mapOf("promo" to "SALE"))
        assertEquals(listOf(expected), received)
    }

    @Test
    fun `should pass empty in-app custom data when intent has no extras`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        var received: List<InAppCustomData> = emptyList()
        fixture.listening(RetenoNotifications.inAppCustomDataReceived) {
            ReceiverInAppData().onReceive(fixture.application, Intent())
            received = it.toList()
        }

        then()
        assertEquals(listOf(InAppCustomData(null, "", "", emptyMap())), received)
    }
}
