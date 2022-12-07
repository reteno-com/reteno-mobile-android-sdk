package com.reteno.push.base.robolectric

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.push.Util
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.interceptor.click.IntentHandler
import com.reteno.push.receiver.NotificationsEnabledManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    application = RetenoTestApp::class,
    packageName = "com.reteno.core",
    shadows = [ShadowLooper::class]
)
abstract class BaseRobolectricTest {

    companion object {

        @JvmStatic
        protected fun mockObjectNotificationsEnabledManager() {
            mockkObject(NotificationsEnabledManager)
        }

        @JvmStatic
        protected fun mockObjectRetenoNotificationsChannel() {
            mockkObject(RetenoNotificationChannel)
        }

        @JvmStatic
        protected fun mockObjectUtil() {
            mockkObject(Util)
        }

        @JvmStatic
        protected fun mockObjectAppLaunchIntent() {
            mockkObject(IntentHandler.AppLaunchIntent)
        }

        //------------------------------------------------------------------------------------------

        @JvmStatic
        protected fun unMockObjectNotificationsEnabledManager() {
            unmockkObject(NotificationsEnabledManager)
        }

        @JvmStatic
        protected fun unMockObjectRetenoNotificationsChannel() {
            unmockkObject(RetenoNotificationChannel)
        }

        @JvmStatic
        protected fun unMockObjectUtil() {
            unmockkObject(Util)
        }

        @JvmStatic
        protected fun unMockObjectAppLaunchIntent() {
            unmockkObject(IntentHandler.AppLaunchIntent)
        }
    }

    protected val application by lazy {
        ApplicationProvider.getApplicationContext() as RetenoTestApp
    }
    protected val reteno by lazy {
        (application as RetenoApplication).getRetenoInstance() as RetenoImpl
    }

    @Before
    @Throws(Exception::class)
    open fun before() {
        MockKAnnotations.init(this)

        clearAllMocks(
            answers = false,
            recordedCalls = true,
            childMocks = true,
            regularMocks = true,
            objectMocks = true,
            staticMocks = true,
            constructorMocks = true
        )
    }

    @After
    open fun after() {
        // Nothing here yet
    }
}