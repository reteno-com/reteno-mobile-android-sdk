package com.reteno.push

import android.os.Bundle
import com.reteno.push.base.robolectric.BaseRobolectricTest
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationHelperTest: BaseRobolectricTest() {

    @Test
    fun getNotificationId_emptyBundle_fallbackDefaultNotificationId() {
        val bundle = Bundle()

        val expectedChannelId = RetenoNotificationHelperProxy.NOTIFICATION_ID_DEFAULT
        val channelId = RetenoNotificationHelperProxy.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }

    @Test
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