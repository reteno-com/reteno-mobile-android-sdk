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
    fun `should combine given main and io dispatchers with default dispatcher`() = runUnitTest {
        given()
        val main = Dispatchers.Unconfined
        val io = Dispatchers.IO

        whenn()
        val sut = retenoDispatchers(main, io)

        then()
        assertSame(main, sut.main)
        assertSame(io, sut.io)
        assertSame(Dispatchers.Default, sut.default)
    }
}
