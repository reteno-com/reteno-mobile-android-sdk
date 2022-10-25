package com.reteno.push.base.robolectric

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.push.base.robolectric.Constants.DEVICE_ID_ANDROID
import com.reteno.push.base.robolectric._setup.FakeAndroidKeyStore
import com.reteno.push.base.robolectric._setup.RetenoTestApp
import io.mockk.*
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.security.Provider
import java.security.Security

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [16],
    application = RetenoTestApp::class,
    packageName = "com.reteno.core",
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
abstract class BaseRobolectricTest {
    protected val application by lazy {
        ApplicationProvider.getApplicationContext() as Application
    }
    protected val reteno by lazy {
        ((application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }

    @Before
    @Throws(Exception::class)
    open fun before() {
        TestCase.assertNotNull(application)
        Settings.Secure.putString(application.contentResolver, Settings.Secure.ANDROID_ID, DEVICE_ID_ANDROID)

        val provider = object : Provider("AndroidKeyStore", 1.0, "") {
            init {
                put("KeyStore.AndroidKeyStore", FakeAndroidKeyStore.FakeKeyStore::class.java.name)
            }
        }
        Security.addProvider(provider)

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), Matchers.anyString()) } returns 0
        every { Log.w(any(), Throwable()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), Throwable()) } returns 0

        mockkStatic(Logger::class)
        every { Logger.v(any(), any(), *anyVararg()) } just runs
        every { Logger.d(any(), any(), *anyVararg()) } just runs
        every { Logger.i(any(), any(), *anyVararg()) } just runs
        every { Logger.w(any(), any(), *anyVararg()) } just runs
        every { Logger.e(any(), any())} just runs
        every { Logger.e(any(), any(), any()) } just runs
        every { Logger.captureException(any()) } just runs
        every { Logger.captureEvent(any()) } just runs
    }

    @After
    open fun after() {
        unmockkStatic(Log::class)
        unmockkStatic(Logger::class)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FakeAndroidKeyStore.setup
        }
    }
}