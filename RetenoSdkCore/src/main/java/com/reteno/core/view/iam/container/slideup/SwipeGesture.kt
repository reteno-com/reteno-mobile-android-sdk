package com.reteno.core.view.iam.container.slideup

import kotlin.math.abs
import kotlin.math.roundToInt

internal class SwipeGesture(private val edge: Edge) {

    enum class Edge { TOP, BOTTOM }

    class Release(val target: Int, val dismiss: Boolean)

    fun clampLeft(origin: Int, left: Int, width: Int): Int = left.coerceIn(origin - width, origin + width)

    fun clampTop(origin: Int, top: Int, height: Int): Int =
        if (edge == Edge.TOP) top.coerceIn(origin - height, origin) else top.coerceIn(origin, origin + height)

    fun releaseHorizontally(origin: Int, left: Int, width: Int, velocity: Float): Release {
        val offset = left - origin
        if (velocity == 0f && !pastThreshold(offset, width)) return Release(origin, dismiss = false)
        val towardEnd = if (velocity == 0f) offset >= 0 else velocity > 0f
        return Release(if (towardEnd) origin + width else origin - width, dismiss = true)
    }

    fun releaseVertically(origin: Int, top: Int, height: Int, velocity: Float): Release {
        val flungTowardEdge = if (edge == Edge.TOP) velocity < 0f else velocity > 0f
        val dismiss = if (velocity == 0f) pastThreshold(top - origin, height) else flungTowardEdge
        if (!dismiss) return Release(origin, dismiss = false)
        return Release(if (edge == Edge.TOP) origin - height else origin + height, dismiss = true)
    }

    fun alpha(offset: Int, size: Int): Float {
        if (offset == 0) return 1f
        return (1f - abs(offset) / (size * FADE_OUT_FRACTION)).coerceIn(0f, 1f)
    }

    private fun pastThreshold(offset: Int, size: Int): Boolean = abs(offset) >= (size * DISMISS_FRACTION).roundToInt()

    private companion object {
        const val DISMISS_FRACTION = 0.5f
        const val FADE_OUT_FRACTION = 0.5f
    }
}
