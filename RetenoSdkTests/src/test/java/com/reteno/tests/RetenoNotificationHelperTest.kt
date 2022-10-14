package com.reteno.tests

import android.os.Bundle
import com.reteno.push.Constants
import com.reteno.push.RetenoNotificationService
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
    fun testGetNotificationId() {
        val bundle = Bundle()

        var expectedChannelId = RetenoNotificationHelperProxy.NOTIFICATION_ID_DEFAULT
        var channelId = RetenoNotificationHelperProxy.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)

        val interactionId = "1234-5678-1234-0987"
        expectedChannelId = interactionId.hashCode()
        bundle.putString(Constants.KEY_ES_INTERACTION_ID, interactionId)
        channelId = RetenoNotificationHelperProxy.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }
}