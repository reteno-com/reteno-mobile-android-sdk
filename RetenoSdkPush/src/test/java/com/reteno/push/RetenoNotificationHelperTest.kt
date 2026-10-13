package com.reteno.push

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import com.reteno.push.fixtures.FakeNotificationImages
import com.reteno.push.fixtures.STUB_INTERACTION_ID
import com.reteno.push.fixtures.applicationMetaData
import com.reteno.push.fixtures.onAndroidSdk
import com.reteno.push.fixtures.stubBitmap
import com.reteno.push.fixtures.stubPushBundle
import com.reteno.push.fixtures.testApplication
import com.reteno.push.interceptor.click.RetenoNotificationClickedActivity
import com.reteno.push.interceptor.click.RetenoNotificationClickedReceiver
import com.reteno.push.receiver.ReceiverNotificationDeleted
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationHelperTest {

    private class SutFixture(available: Map<String, android.graphics.Bitmap> = emptyMap()) {
        val application = testApplication()
        val images = FakeNotificationImages(available)
        val sut = RetenoNotificationHelper(application, images)

        fun build(bundle: Bundle): Notification = sut.getNotificationBuilderCompat(bundle).build()

        fun putMetaData(key: Int, value: Int) {
            application.applicationMetaData().putInt(application.getString(key), value)
        }

        inline fun <T> withGroupingRule(noinline rule: ((Map<String, String?>) -> String?)?, block: () -> T): T {
            RetenoNotifications.setGroupingRule(rule)
            try {
                return block()
            } finally {
                RetenoNotifications.setGroupingRule(null)
            }
        }
    }

    @Test
    fun `should derive notification id from interaction id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val ids = listOf(stubPushBundle(), Bundle()).map(fixture.sut::getNotificationId)

        then()
        assertEquals(listOf(STUB_INTERACTION_ID.hashCode(), RetenoNotificationHelper.NOTIFICATION_ID_DEFAULT), ids)
    }

    @Test
    fun `should show push title and text`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val notification = fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_CONTENT to "Short text"))

        then()
        assertEquals("Title", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Short text", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("defaultId", notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun `should use app name as title when push has none`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val bundle = Bundle().apply { putString(Constants.KEY_ES_INTERACTION_ID, STUB_INTERACTION_ID) }

        whenn()
        val notification = fixture.build(bundle)

        then()
        val appName = fixture.application.applicationInfo.loadLabel(fixture.application.packageManager).toString()
        assertEquals(appName, notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull(notification.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun `should expand long text without images`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val longText = "x".repeat(101)

        whenn()
        val notification = fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_CONTENT to longText))

        then()
        assertEquals(Notification.BigTextStyle::class.java.name, notification.extras.getString(Notification.EXTRA_TEMPLATE))
        assertEquals(longText, notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
    }

    @Test
    fun `should show big picture with long text as summary`() = runUnitTest {
        given()
        val fixture = SutFixture(mapOf(IMAGE to stubBitmap()))
        val longText = "x".repeat(101)

        whenn()
        val notification = fixture.build(
            stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_CONTENT to longText, Constants.KEY_ES_NOTIFICATION_IMAGE to IMAGE)
        )

        then()
        assertEquals(Notification.BigPictureStyle::class.java.name, notification.extras.getString(Notification.EXTRA_TEMPLATE))
        assertEquals(longText, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(listOf(IMAGE), fixture.images.requested)
    }

    @Test
    fun `should skip big picture that can not be downloaded`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val notification = fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_NOTIFICATION_IMAGE to IMAGE))

        then()
        assertNull(notification.extras.getString(Notification.EXTRA_TEMPLATE))
        assertEquals(listOf(IMAGE), fixture.images.requested)
    }

    @Test
    fun `should not download blank big picture url`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_NOTIFICATION_IMAGE to " "))

        then()
        assertTrue(fixture.images.requested.isEmpty())
    }

    @Test
    fun `should show carousel of downloaded images`() = runUnitTest {
        given()
        val fixture = SutFixture(mapOf(IMAGE to stubBitmap()))
        val carousel = """["$IMAGE", " ", "https://cdn.example/missing.png"]"""

        whenn()
        val builder = fixture.sut.getNotificationBuilderCompat(
            stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_NOTIFICATION_CAROUSEL_IMAGES to carousel, Constants.KEY_ES_CONTENT to "x".repeat(101))
        )

        then()
        assertEquals(listOf(IMAGE, "https://cdn.example/missing.png"), fixture.images.requested)
        assertNotNull(builder.bigContentView)
        assertEquals(Notification.DecoratedCustomViewStyle::class.java.name, builder.build().extras.getString(Notification.EXTRA_TEMPLATE))
    }

    @Test
    fun `should skip carousel without usable image urls`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val bundles = listOf("""[" ", ""]""", "not json").map {
            stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_NOTIFICATION_CAROUSEL_IMAGES to it)
        }

        whenn()
        val builders = bundles.map(fixture.sut::getNotificationBuilderCompat)

        then()
        assertTrue(builders.all { it.bigContentView == null })
        assertTrue(fixture.images.requested.isEmpty())
    }

    @Test
    fun `should show badge count from push`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val numbers = listOf("5", "many").map { fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_BADGE_COUNT to it)).number }

        then()
        assertEquals(listOf(5, 0), numbers)
    }

    @Test
    fun `should use bundled push icon when app sets none`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val notification = fixture.build(stubPushBundle())

        then()
        assertEquals(R.drawable.reteno_default_push_icon, notification.smallIcon.resId)
        assertEquals(0, notification.color)
    }

    @Test
    fun `should use push icon and color from app meta data`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.putMetaData(R.string.notification_icon, android.R.drawable.ic_dialog_email)
        fixture.putMetaData(R.string.notification_icon_color, android.R.color.holo_red_dark)

        whenn()
        val notification = fixture.build(stubPushBundle())

        then()
        assertEquals(android.R.drawable.ic_dialog_email, notification.smallIcon.resId)
        assertEquals(fixture.application.getColor(android.R.color.holo_red_dark), notification.color)
        assertEquals(Notification.DecoratedCustomViewStyle::class.java.name, notification.extras.getString(Notification.EXTRA_TEMPLATE))
    }

    @Test
    fun `should add at most three action buttons carrying their links and custom data`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val buttons = """[
            {"action_id":"b1","label":"Open","link":"https://track.example/b1","link_raw":"https://example.com/b1","custom_data":{"tier":"gold","tags":["a",{"k":1}]}},
            {"label":"Second"},
            {},
            {"label":"Fourth"}
        ]"""

        whenn()
        val notification = fixture.build(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_BUTTONS to buttons))

        then()
        assertEquals(listOf("Open", "Second", null), notification.actions.map { it.title?.toString() })
        val first = shadowOf(notification.actions[0].actionIntent).savedIntent
        assertTrue(first.getBooleanExtra(Constants.KEY_ACTION_BUTTON, false))
        assertEquals("b1", first.getStringExtra(Constants.KEY_BTN_ACTION_ID))
        assertEquals("Open", first.getStringExtra(Constants.KEY_BTN_ACTION_LABEL))
        assertEquals("https://track.example/b1", first.getStringExtra(Constants.KEY_BTN_ACTION_LINK_WRAPPED))
        assertEquals("https://example.com/b1", first.getStringExtra(Constants.KEY_BTN_ACTION_LINK_UNWRAPPED))
        assertEquals(
            hashMapOf("tier" to "gold", "tags" to listOf("a", hashMapOf("k" to 1))),
            first.getSerializableExtra(Constants.KEY_BTN_ACTION_CUSTOM_DATA)
        )
        val second = shadowOf(notification.actions[1].actionIntent).savedIntent
        val absentKeys = listOf(
            Constants.KEY_BTN_ACTION_ID,
            Constants.KEY_BTN_ACTION_LINK_WRAPPED,
            Constants.KEY_BTN_ACTION_LINK_UNWRAPPED,
            Constants.KEY_BTN_ACTION_CUSTOM_DATA
        )
        assertTrue(absentKeys.none(second::hasExtra))
        assertFalse(shadowOf(notification.actions[2].actionIntent).savedIntent.hasExtra(Constants.KEY_BTN_ACTION_LABEL))
    }

    @Test
    fun `should open click receiver when notification trampolines are allowed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val notification = fixture.build(stubPushBundle())

        then()
        val content = shadowOf(notification.contentIntent)
        assertTrue(content.isBroadcastIntent)
        assertEquals(RetenoNotificationClickedReceiver::class.java.name, content.savedIntent.component?.className)
        assertEquals(STUB_INTERACTION_ID, content.savedIntent.getStringExtra(Constants.KEY_ES_INTERACTION_ID))
    }

    @Test
    fun `should open click activity when notification trampolines are blocked`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val builder = onAndroidSdk(Build.VERSION_CODES.S) { fixture.sut.getNotificationBuilderCompat(stubPushBundle()) }

        then()
        val content = shadowOf(builder.build().contentIntent)
        assertTrue(content.isActivityIntent)
        assertEquals(RetenoNotificationClickedActivity::class.java.name, content.savedIntent.component?.className)
    }

    @Test
    fun `should notify host delete receiver with a delete intent per notification`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val first = fixture.build(stubPushBundle("interaction-1"))
        val second = fixture.build(stubPushBundle("interaction-2"))

        then()
        val firstDelete = shadowOf(first.deleteIntent)
        assertEquals(ReceiverNotificationDeleted::class.java.name, firstDelete.savedIntent.component?.className)
        assertEquals("interaction-1".hashCode(), firstDelete.requestCode)
        assertEquals("interaction-2".hashCode(), shadowOf(second.deleteIntent).requestCode)
        assertFalse(firstDelete.isCanceled)
        assertTrue(firstDelete.flags and PendingIntent.FLAG_IMMUTABLE != 0)
    }

    @Test
    fun `should not add delete intent when app has no delete receiver`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.application.applicationMetaData().remove(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_DELETED)

        whenn()
        val notification = fixture.build(stubPushBundle())

        then()
        assertNull(notification.deleteIntent)
    }

    @Test
    fun `should group notification by host grouping rule`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val seen = mutableListOf<Map<String, String?>>()

        whenn()
        val groups = fixture.withGroupingRule({ data -> seen += data; data["category"] }) {
            listOf(
                fixture.build(stubPushBundle(STUB_INTERACTION_ID, "category" to "promo")),
                fixture.build(stubPushBundle())
            ).map { it.group }
        }
        val ungrouped = fixture.build(stubPushBundle()).group

        then()
        assertEquals(listOf("promo", null), groups)
        assertNull(ungrouped)
        assertEquals("promo", seen.first()["category"])
    }

    private companion object {
        const val IMAGE = "https://cdn.example/image.png"
    }
}
