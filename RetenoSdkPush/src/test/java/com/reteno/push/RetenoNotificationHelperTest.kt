package com.reteno.push

import android.os.Bundle
import com.reteno.push.base.robolectric.BaseRobolectricTest
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config

@Config(sdk = [26])
class RetenoNotificationHelperTest: BaseRobolectricTest() {

    @Test
    fun getNotificationId_emptyBundle_fallbackDefaultNotificationId() {
        val bundle = Bundle()

        val expectedChannelId = NOTIFICATION_ID_DEFAULT
        val channelId = RetenoNotificationHelper.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }

    @Test
    fun getNotificationId_bundleWithInteractionId_hashCodeOfInteractionId() {
        val interactionId = "1234-5678-1234-0987"
        val expectedChannelId = interactionId.hashCode()

        val bundle = Bundle().apply {
            putString(Constants.KEY_ES_INTERACTION_ID, interactionId)
        }
        val channelId = RetenoNotificationHelper.getNotificationId(bundle)
        assertEquals(expectedChannelId, channelId)
    }

    companion object {
        private val NOTIFICATION_ID_DEFAULT =
            Whitebox.getField(RetenoNotificationHelper::class.java, "NOTIFICATION_ID_DEFAULT")
                .get(RetenoNotificationHelper::class.java)
    }
}