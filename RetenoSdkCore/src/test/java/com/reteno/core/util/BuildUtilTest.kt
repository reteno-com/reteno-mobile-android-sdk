package com.reteno.core.util

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ApplicationInfo
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class BuildUtilTest {

    private class SutFixture(targetSdk: Int) {
        val context = mockk<Context>().also {
            val info = mockk<ApplicationInfo>()
            info.targetSdkVersion = targetSdk
            every { it.applicationInfo } returns info
        }
    }

    @Test
    fun `should disable trampolines only when both device and app are on android 12`() = runUnitTest {
        given()
        val combinations = listOf(31 to 31, 30 to 31, 31 to 30, 34 to 33)

        whenn()
        val disabled = combinations.map { (sdk, target) -> BuildUtil.trampolinesDisabled(sdk, target) }

        then()
        assertEquals(listOf(true, false, false, true), disabled)
    }

    @Test
    fun `should read target sdk of the app every time`() = runUnitTest {
        given()
        val contexts = listOf(SutFixture(targetSdk = 30).context, SutFixture(targetSdk = 34).context)

        whenn()
        val targets = contexts.map { BuildUtil.getTargetSdkVersion(it) }

        then()
        assertEquals(listOf(30, 34), targets)
    }

    @Test
    fun `should keep trampolines on device older than android 12`() = runUnitTest {
        given()
        val fixture = SutFixture(targetSdk = 34)

        whenn()
        val disabled = BuildUtil.shouldDisableTrampolines(fixture.context)

        then()
        assertFalse(disabled)
    }

    @Test
    fun `should make pending intent flags immutable`() = runUnitTest {
        given()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT

        whenn()
        val immutable = BuildUtil.createIntentFlags(flags)

        then()
        assertEquals(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE, immutable)
    }
}
