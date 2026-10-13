package com.reteno.core.internal.platform

import android.app.Application
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AppInfoTest {

    @Test
    fun `should read installed version name and code`() = runUnitTest {
        given()
        val application = ApplicationProvider.getApplicationContext<Application>()
        @Suppress("DEPRECATION")
        val info = PackageInfo().apply {
            packageName = application.packageName
            versionName = "3.1"
            versionCode = 31
        }
        shadowOf(application.packageManager).installPackage(info)
        val sut = AppInfo(application)

        whenn()
        val version = sut.installedVersion()

        then()
        assertEquals(AppVersion("3.1", 31L), version)
    }

    @Test
    fun `should fall back to empty version when package is unknown`() = runUnitTest {
        given()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val unknownPackage = object : ContextWrapper(application) {
            override fun getPackageName(): String = "com.example.not.installed"
        }
        val sut = AppInfo(unknownPackage)

        whenn()
        val version = sut.installedVersion()

        then()
        assertEquals(AppVersion("", 0L), version)
    }

    @Test
    fun `should use empty name when installed version has no name`() = runUnitTest {
        given()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val info = PackageInfo().apply { packageName = application.packageName }
        shadowOf(application.packageManager).installPackage(info)
        val sut = AppInfo(application)

        whenn()
        val version = sut.installedVersion()

        then()
        assertEquals("", version.name)
    }
}
