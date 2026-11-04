package com.reteno.core.view.iam.container.slideup

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SlideInTest {

    @Test
    fun `should start a slide-up just past the edge it comes from`() = runUnitTest {
        given()
        val height = 120

        whenn()
        val starts = listOf(slideInStart(height, fromBottom = false), slideInStart(height, fromBottom = true))

        then()
        assertEquals(listOf(-120f, 120f), starts)
    }
}
