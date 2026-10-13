package com.reteno.core

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RetenoInternalEntryPointTest {

    private class SutFixture {
        private val previousInstance = Reteno.instanceInternal

        fun <T> withoutSdkInstance(block: () -> T): T {
            Reteno.instanceInternal = null
            try {
                return block()
            } finally {
                RetenoInternalEntryPoint.install(null)
                Reteno.instanceInternal = previousInstance
            }
        }
    }

    @Test
    fun `should return installed facade`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val facade = mockk<RetenoInternalFacade>()

        whenn()
        val resolved = fixture.withoutSdkInstance {
            RetenoInternalEntryPoint.install(facade)
            RetenoInternalEntryPoint.facade
        }

        then()
        assertSame(facade, resolved)
    }

    @Test
    fun `should fall back to sdk instance once installed facade is removed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val failure = fixture.withoutSdkInstance {
            RetenoInternalEntryPoint.install(mockk())
            RetenoInternalEntryPoint.install(null)
            runCatching { RetenoInternalEntryPoint.facade }.exceptionOrNull()
        }

        then()
        assertTrue(failure is IllegalArgumentException)
    }
}
