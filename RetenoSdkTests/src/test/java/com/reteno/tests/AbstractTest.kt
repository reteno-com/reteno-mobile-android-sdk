package com.reteno.tests

import com.reteno.RetenoApplication
import com.reteno.RetenoImpl
import com.reteno.tests._setup.RetenoTestApp
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [16],
    application = RetenoTestApp::class,
    packageName = "com.reteno.sample",
    shadows = [ShadowLooper::class]
)
@PowerMockIgnore(
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*",
    "javax.net.ssl.*",
    "javax.xml.*",
    "org.xml.sax.*",
    "org.w3c.dom.*",
    "jdk.internal.reflect.*"
)
abstract class AbstractTest {

    protected val application by lazy {
        RuntimeEnvironment.application
    }
    protected val reteno by lazy {
        ((application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }

    @Before
    @Throws(Exception::class)
    open fun before() {

    }

    @After
    open fun after() {

    }
}