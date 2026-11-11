package com.reteno.push.interceptor.click

import android.app.ActivityOptions
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AppLaunchOptionsTest {

    private class SutFixture {
        val bundle = mockk<Bundle>()
        val options = mockk<ActivityOptions>()

        fun <T> withActivityOptions(block: () -> T): T {
            mockkStatic(ActivityOptions::class)
            try {
                every { ActivityOptions.makeBasic() } returns options
                every { options.setSplashScreenStyle(any()) } returns options
                every { options.toBundle() } returns bundle
                return block()
            } finally {
                unmockkStatic(ActivityOptions::class)
            }
        }
    }

    @Test
    fun `should ask for the icon splash screen on Android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val launchOptions = fixture.withActivityOptions { appLaunchOptions(Build.VERSION_CODES.TIRAMISU) }

        then()
        assertSame(fixture.bundle, launchOptions)
        verify { fixture.options.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON) }
    }

    @Test
    fun `should not pass launch options before Android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val launchOptions = fixture.withActivityOptions { appLaunchOptions(Build.VERSION_CODES.S_V2) }

        then()
        assertNull(launchOptions)
    }
}
