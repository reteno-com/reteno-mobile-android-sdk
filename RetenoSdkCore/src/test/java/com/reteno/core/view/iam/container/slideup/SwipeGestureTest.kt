package com.reteno.core.view.iam.container.slideup

import com.reteno.core.view.iam.container.slideup.SwipeGesture.Edge
import com.reteno.core.view.iam.container.slideup.SwipeGesture.Release
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SwipeGestureTest {

    private val Release.outcome: Pair<Int, Boolean>
        get() = target to dismiss

    @Test
    fun `should let a message move sideways by at most its width`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.TOP)

        whenn()
        val lefts = listOf(-500, -100, 150, 500).map { gesture.clampLeft(origin = 0, left = it, width = 200) }

        then()
        assertEquals(listOf(-200, -100, 150, 200), lefts)
    }

    @Test
    fun `should let a top message move only up toward its edge`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.TOP)

        whenn()
        val tops = listOf(-500, -40, 30).map { gesture.clampTop(origin = 10, top = it, height = 100) }

        then()
        assertEquals(listOf(-90, -40, 10), tops)
    }

    @Test
    fun `should let a bottom message move only down toward its edge`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.BOTTOM)

        whenn()
        val tops = listOf(-500, 860, 1500).map { gesture.clampTop(origin = 800, top = it, height = 100) }

        then()
        assertEquals(listOf(800, 860, 900), tops)
    }

    @Test
    fun `should dismiss a message dragged sideways past half its width`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.TOP)

        whenn()
        val releases = listOf(100, -100, 99).map {
            gesture.releaseHorizontally(origin = 0, left = it, width = 200, velocity = 0f).outcome
        }

        then()
        assertEquals(listOf(200 to true, -200 to true, 0 to false), releases)
    }

    @Test
    fun `should dismiss a message flung sideways in the direction of the fling`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.TOP)

        whenn()
        val releases = listOf(500f, -500f).map {
            gesture.releaseHorizontally(origin = 0, left = 10, width = 200, velocity = it).outcome
        }

        then()
        assertEquals(listOf(200 to true, -200 to true), releases)
    }

    @Test
    fun `should dismiss a message dragged toward its edge past half its height`() = runUnitTest {
        given()
        val top = SwipeGesture(Edge.TOP)
        val bottom = SwipeGesture(Edge.BOTTOM)

        whenn()
        val releases = listOf(
            top.releaseVertically(origin = 0, top = -50, height = 100, velocity = 0f).outcome,
            top.releaseVertically(origin = 0, top = -49, height = 100, velocity = 0f).outcome,
            bottom.releaseVertically(origin = 800, top = 850, height = 100, velocity = 0f).outcome
        )

        then()
        assertEquals(listOf(-100 to true, 0 to false, 900 to true), releases)
    }

    @Test
    fun `should dismiss a message flung only toward its edge`() = runUnitTest {
        given()
        val top = SwipeGesture(Edge.TOP)
        val bottom = SwipeGesture(Edge.BOTTOM)

        whenn()
        val releases = listOf(
            top.releaseVertically(origin = 0, top = 0, height = 100, velocity = -300f).outcome,
            top.releaseVertically(origin = 0, top = 0, height = 100, velocity = 300f).outcome,
            bottom.releaseVertically(origin = 800, top = 800, height = 100, velocity = 300f).outcome,
            bottom.releaseVertically(origin = 800, top = 800, height = 100, velocity = -300f).outcome
        )

        then()
        assertEquals(listOf(-100 to true, 0 to false, 900 to true, 800 to false), releases)
    }

    @Test
    fun `should fade a message out over the first half of its size`() = runUnitTest {
        given()
        val gesture = SwipeGesture(Edge.TOP)

        whenn()
        val alphas = listOf(0, 25, -50, 100, 300).map { gesture.alpha(offset = it, size = 200) }

        then()
        assertEquals(listOf(1f, 0.75f, 0.5f, 0f, 0f), alphas)
    }
}
