package com.reteno.robolectric

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.robolectric._setup.FakeAndroidKeyStore
import com.reteno.robolectric._setup.RetenoTestApp
import com.reteno.core.data.local.config.DeviceIdHelperTest
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
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
        Settings.Secure.putString(application.contentResolver, Settings.Secure.ANDROID_ID,
            DeviceIdHelperTest.DEVICE_ID_ANDROID
        )

        val provider = object : Provider("AndroidKeyStore", 1.0, "") {
            init {
                put("KeyStore.AndroidKeyStore", FakeAndroidKeyStore.FakeKeyStore::class.java.name)
            }
        }
        Security.addProvider(provider)
    }

    @After
    open fun after() {

    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FakeAndroidKeyStore.setup
        }
    }
}