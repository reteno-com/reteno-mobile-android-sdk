package com.reteno.core.internal.action.iam

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppHtmlPreloadTest {

    private class SutFixture(scope: TestScope) {
        val preload = mockk<PreloadIamHtml>().also {
            coEvery { it() } returns Result.success(Unit)
        }
        val sut = InAppHtmlPreload(preload = preload, scope = scope.workScope())
    }

    @Test
    fun `should preload base html in background when requested`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.request()
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.preload() }
    }
}
