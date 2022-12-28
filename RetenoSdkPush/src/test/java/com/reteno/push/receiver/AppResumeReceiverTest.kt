package com.reteno.push.receiver

import android.content.ContextWrapper
import android.content.Intent
import com.reteno.core.util.Constants.BROADCAST_ACTION_RETENO_APP_PAUSE
import com.reteno.core.util.Constants.BROADCAST_ACTION_RETENO_APP_RESUME
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.justRun
import io.mockk.verify
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.robolectric.shadows.ShadowLooper


class AppResumeReceiverTest : BaseRobolectricTest() {

    // region helper fields ------------------------------------------------------------------------
    private var contextWrapper: ContextWrapper? = null
    // endregion helper fields ---------------------------------------------------------------------

    private var SUT: AppResumeReceiver? = null


    override fun before() {
        super.before()
        SUT = AppResumeReceiver()

        contextWrapper = ContextWrapper(application)
        assertNotNull(contextWrapper)

        justRun { NotificationsEnabledManager.onCheckState(any()) }
    }

    override fun after() {
        super.after()
        contextWrapper = null
    }

    @Test
    fun whenBroadcastAppResumeSent_thenNotificationsEnabledManagerOnCheckStateCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(BROADCAST_ACTION_RETENO_APP_RESUME))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 1) { NotificationsEnabledManager.onCheckState(any()) }
    }

    @Test
    fun whenBroadcastAppPauseSent_thenNotificationsEnabledManagerOnCheckStateNotCalled() {
        // When
        contextWrapper!!.sendBroadcast(Intent(BROADCAST_ACTION_RETENO_APP_PAUSE))

        // Then
        ShadowLooper.shadowMainLooper().idle()
        verify(exactly = 0) { NotificationsEnabledManager.onCheckState(any()) }
    }
}