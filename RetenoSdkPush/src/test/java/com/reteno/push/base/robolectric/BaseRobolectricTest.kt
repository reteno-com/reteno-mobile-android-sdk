package com.reteno.push.base.robolectric

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoConfig
import com.reteno.core.RetenoImpl
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    application = RetenoTestApp::class,
    packageName = "com.reteno.core",
    shadows = [ShadowLooper::class]
)
abstract class BaseRobolectricTest {

    protected val application by lazy {
        ApplicationProvider.getApplicationContext() as RetenoTestApp
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
        application.retenoMock = mockk()
    }

    protected fun TestScope.createReteno(): RetenoImpl {
        return RetenoImpl(
            application = application,
            config = RetenoConfig(),
            syncScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            delayInitialization = false
        ).also {
            application.retenoMock = it
        }
    }
}