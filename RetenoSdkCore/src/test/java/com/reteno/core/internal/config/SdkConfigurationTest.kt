package com.reteno.core.internal.config

import com.reteno.core.RetenoConfig
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.STUB_ACCESS_KEY
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SdkConfigurationTest {

    private class SutFixture {
        val configProvider = mockk<RetenoConfigProvider>(relaxed = true)
        val filter = LifecycleEventFilter(LifecycleTrackingOptions.ALL)
        val sut = SdkConfigurationImpl(configProvider, filter)

        fun config(
            debug: Boolean = false,
            options: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL
        ): RetenoConfig =
            RetenoConfig.Builder()
                .accessKey(STUB_ACCESS_KEY)
                .setDebug(debug)
                .lifecycleTrackingOptions(options)
                .build()
    }

    @Test
    fun `should use user configuration for networking lifecycle tracking and debug mode`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val config = fixture.config(debug = true, options = LifecycleTrackingOptions.NONE)

        whenn()
        fixture.sut.useUserConfig(config)

        then()
        verify(exactly = 1) { fixture.configProvider.setConfig(config) }
        assertFalse(fixture.filter.isEnabled(LifecycleEventType.APP_LIFECYCLE))
        assertTrue(fixture.sut.isDebug)
    }

    @Test
    fun `should use cached configuration until user configuration arrives`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val cached = fixture.config()
        val user = fixture.config()

        whenn()
        val cachedUsedFirst = fixture.sut.useCachedConfig(cached)
        fixture.sut.useUserConfig(user)
        val cachedUsedAfterUser = fixture.sut.useCachedConfig(cached)

        then()
        assertEquals(listOf(true, false), listOf(cachedUsedFirst, cachedUsedAfterUser))
        verify(exactly = 1) { fixture.configProvider.setConfig(cached) }
        verify(exactly = 1) { fixture.configProvider.setConfig(user) }
        assertFalse(fixture.sut.isDebug)
    }

    @Test
    fun `should expose no configuration before one is used`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val config = fixture.sut.config

        then()
        assertNull(config)
    }

    @Test
    fun `should expose configuration in use`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val cached = fixture.config()
        val user = fixture.config()

        whenn()
        fixture.sut.useCachedConfig(cached)
        val afterCached = fixture.sut.config
        fixture.sut.useUserConfig(user)

        then()
        assertSame(cached, afterCached)
        assertSame(user, fixture.sut.config)
    }
}
