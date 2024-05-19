package com.reteno.core.base.robolectric

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoConfig
import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.util.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    application = RetenoTestApp::class,
    packageName = "com.reteno.core",
    shadows = [ShadowLooper::class, ShadowPackageManager::class]
)

abstract class BaseRobolectricTest {

    protected val application by lazy {
        ApplicationProvider.getApplicationContext() as RetenoTestApp
    }

    protected val reteno: RetenoImpl
        get() = requireNotNull(application.retenoMock)

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
        // Nothing here yet
    }

    protected fun TestScope.createRetenoAndAdvanceInit(): RetenoImpl {
        return RetenoImpl(
            application = application,
            config = RetenoConfig(),
            syncScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            delayInitialization = false
        ).also {
            application.retenoMock = it
            advanceUntilIdle()
        }
    }
}