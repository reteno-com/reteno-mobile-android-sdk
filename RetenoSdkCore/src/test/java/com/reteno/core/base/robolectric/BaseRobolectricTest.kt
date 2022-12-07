package com.reteno.core.base.robolectric

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.util.*
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