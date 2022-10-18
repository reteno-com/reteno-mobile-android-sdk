package com.reteno.robolectric.push

import android.os.Bundle
import com.reteno.push.Constants
import com.reteno.push.RetenoNotificationService
import com.reteno.robolectric.AbstractTest
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationHelperTest: AbstractTest() {

    private val pushService by lazy {
        RetenoNotificationService()
    }

    @Throws(Exception::class)
    @Before
    override fun before() {
        super.before()
    }

    @After
    override fun after() {
        super.after()
    }

    @Test
    @Throws(Exception::class)
    fun getNotificationId_emptyBundle_fallbackDefaultNotificationId() {
        val bundle = Bundle()

        val expectedChannelId = RetenoNotificationHelperProxy.NOTIFICATION_ID_DEFAULT
        val channelId = RetenoNotificationHelperProxy.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }

    @Test
    @Throws(Exception::class)
    fun getNotificationId_bundleWithInteractionId_hashCodeOfInteractionId() {
        val interactionId = "1234-5678-1234-0987"
        val expectedChannelId = interactionId.hashCode()

        val bundle = Bundle().apply {
            putString(Constants.KEY_ES_INTERACTION_ID, interactionId)
        }
        val channelId = RetenoNotificationHelperProxy.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }
}