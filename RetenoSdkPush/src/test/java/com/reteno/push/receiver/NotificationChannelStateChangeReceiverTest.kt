package com.reteno.push.receiver

import android.app.NotificationManager.*
import android.content.ContextWrapper
import android.content.Intent
import com.reteno.core.util.Constants
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertNotNull
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.shadows.ShadowLooper


class NotificationChannelStateChangeReceiverTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectNotificationsEnabledManager()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectNotificationsEnabledManager()
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var contextWrapper: ContextWrapper? = null
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT: NotificationChannelStateChangeReceiver? = null


    override fun before() {
        super.before()
        SUT = NotificationChannelStateChangeReceiver()

        contextWrapper = ContextWrapper(application)
        assertNotNull(contextWrapper)

        justRun { NotificationsEnabledManager.onCheckState(any()) }
    }

    override fun after() {
        super.after()
        contextWrapper = null
    }

    @Test
    fun whenBroadcastAppBlockStateChangedSent_thenNotificationsEnabledManagerOnCheckStateCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(ACTION_APP_BLOCK_STATE_CHANGED))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 1) { NotificationsEnabledManager.onCheckState(any()) }
    }

    @Test
    fun whenBroadcastChannelBlockStateChangedSent_thenNotificationsEnabledManagerOnCheckStateCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 1) { NotificationsEnabledManager.onCheckState(any()) }
    }

    @Test
    fun whenBroadcastChannelGroupBlockStateChangedSent_thenNotificationsEnabledManagerOnCheckStateCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(ACTION_NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 1) { NotificationsEnabledManager.onCheckState(any()) }
    }

    @Test
    fun whenBroadcastAppPauseSent_thenNotificationsEnabledManagerOnCheckStateNotCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(Constants.BROADCAST_ACTION_RETENO_APP_PAUSE))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 0) { NotificationsEnabledManager.onCheckState(any()) }
    }
}