package com.reteno.core.internal.coroutine

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class RetenoDispatchersTest {

    @Test
    fun `should expose standard android dispatchers`() = runUnitTest {
        given()
        val sut = AndroidRetenoDispatchers

        whenn()
        val dispatchers = Triple(sut.main, sut.io, sut.default)

        then()
        assertSame(Dispatchers.Main, dispatchers.first)
        assertSame(Dispatchers.IO, dispatchers.second)
        assertSame(Dispatchers.Default, dispatchers.third)
    }
}
