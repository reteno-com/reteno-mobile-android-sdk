package com.reteno.core.internal.platform

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AppBroadcasterTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val sut = AppBroadcaster(application)

        fun registerReceiver(action: String, receiverName: String?) {
            registerReceiver(Intent(action), receiverName)
        }

        fun registerReceiver(intent: Intent, receiverName: String?) {
            val info = ResolveInfo().apply {
                if (receiverName != null) {
                    activityInfo = ActivityInfo().apply {
                        packageName = application.packageName
                        name = receiverName
                    }
                }
            }
            shadowOf(application.packageManager).addResolveInfoForIntent(intent, info)
        }
    }

    @Test
    fun `should send explicit broadcast to every registered receiver`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.registerReceiver("com.reteno.test-action", "com.example.FirstReceiver")
        fixture.registerReceiver("com.reteno.test-action", "com.example.SecondReceiver")

        whenn()
        val sent = fixture.sut.send("com.reteno.test-action")

        then()
        assertEquals(2, sent)
        val intents = shadowOf(fixture.application).broadcastIntents
        assertEquals(
            listOf("com.example.FirstReceiver", "com.example.SecondReceiver"),
            intents.map { it.component?.className }
        )
        assertEquals(
            Intent.FLAG_INCLUDE_STOPPED_PACKAGES,
            intents.first().flags and Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        )
    }

    @Test
    fun `should skip receivers without component info`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.registerReceiver("com.reteno.other-action", null)

        whenn()
        val sent = fixture.sut.send("com.reteno.other-action")

        then()
        assertEquals(0, sent)
        assertEquals(0, shadowOf(fixture.application).broadcastIntents.size)
    }

    @Test
    fun `should send data only to receivers of the app itself`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val appIntent = Intent("com.reteno.data-action").setPackage(fixture.application.packageName)
        fixture.registerReceiver(appIntent, "com.example.AppReceiver")
        fixture.registerReceiver("com.reteno.data-action", "com.example.AnyAppReceiver")

        whenn()
        val sent = fixture.sut.sendToApp("com.reteno.data-action", mapOf("coupon" to "SALE"))

        then()
        assertEquals(1, sent)
        val intent = shadowOf(fixture.application).broadcastIntents.single()
        assertEquals("com.example.AppReceiver", intent.component?.className)
        assertEquals(fixture.application.packageName, intent.`package`)
        assertEquals("SALE", intent.getStringExtra("coupon"))
    }

    @Test
    fun `should send no data when the app has no receiver`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val sent = fixture.sut.sendToApp("com.reteno.data-action", mapOf("coupon" to "SALE"))

        then()
        assertEquals(0, sent)
        assertEquals(0, shadowOf(fixture.application).broadcastIntents.size)
    }
}
