package com.reteno.core.view.iam.container.slideup

import android.app.Activity
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class SwipeDismissBehaviorTest {

    private class SutFixture(edge: SwipeGesture.Edge = SwipeGesture.Edge.TOP, accessibilityChosen: Boolean = false) {
        val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var dismissed = 0
        val parent = CoordinatorLayout(activity)
        val message = View(activity).apply {
            isClickable = true
            if (accessibilityChosen) {
                ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
            }
        }
        private var downTime = 0L

        init {
            val params = CoordinatorLayout.LayoutParams(WIDTH, HEIGHT).apply {
                behavior = SwipeDismissBehavior<View>(SwipeGesture(edge)) { dismissed++ }
                gravity = Gravity.TOP or Gravity.START
                topMargin = TOP
            }
            parent.addView(message, params)
            activity.setContentView(parent)
            ShadowLooper.idleMainLooper()
        }

        fun drag(fromX: Float, fromY: Float, toX: Float, toY: Float, holdBeforeRelease: Boolean = true) {
            downTime = now()
            send(MotionEvent.ACTION_DOWN, fromX, fromY)
            val steps = 10
            for (step in 1..steps) {
                advance(10)
                val x = fromX + (toX - fromX) * step / steps
                val y = fromY + (toY - fromY) * step / steps
                send(MotionEvent.ACTION_MOVE, x, y)
            }
            if (holdBeforeRelease) {
                advance(300)
                send(MotionEvent.ACTION_MOVE, toX, toY)
                advance(300)
            } else {
                advance(10)
            }
            send(MotionEvent.ACTION_UP, toX, toY)
            settle()
        }

        fun send(action: Int, x: Float, y: Float) {
            val event = MotionEvent.obtain(downTime, now(), action, x, y, 0)
            parent.dispatchTouchEvent(event)
            event.recycle()
        }

        fun sendPointer(action: Int, xs: List<Float>, y: Float) {
            val properties = xs.indices.map { MotionEvent.PointerProperties().apply { id = it } }.toTypedArray()
            val coords = xs.map { x -> MotionEvent.PointerCoords().apply { this.x = x; this.y = y } }.toTypedArray()
            val event = MotionEvent.obtain(
                downTime,
                now(),
                action,
                xs.size,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0
            )
            parent.dispatchTouchEvent(event)
            event.recycle()
        }

        fun startTouch() {
            downTime = now()
        }

        fun advance(millis: Long) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(millis))
        }

        fun settle() {
            repeat(SETTLE_FRAMES) { ShadowLooper.idleMainLooper(FRAME_MILLIS, TimeUnit.MILLISECONDS) }
        }

        private fun now() = android.os.SystemClock.uptimeMillis()
    }

    @Test
    fun `should dismiss a message dragged sideways past half its width`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 20f, fromY = 60f, toX = 180f, toY = 60f)

        then()
        assertEquals(1, fixture.dismissed)
        assertEquals(WIDTH, fixture.message.left)
        assertEquals(0f, fixture.message.alpha)
    }

    @Test
    fun `should put back a message dragged sideways a little`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 20f, fromY = 60f, toX = 60f, toY = 60f)

        then()
        assertEquals(0, fixture.dismissed)
        assertEquals(listOf(0, TOP), listOf(fixture.message.left, fixture.message.top))
        assertEquals(1f, fixture.message.alpha)
    }

    @Test
    fun `should dismiss a message flung sideways`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 150f, fromY = 60f, toX = 100f, toY = 60f, holdBeforeRelease = false)

        then()
        assertEquals(1, fixture.dismissed)
        assertEquals(-WIDTH, fixture.message.left)
    }

    @Test
    fun `should dismiss a top message dragged up past half its height`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 100f, fromY = 105f, toX = 110f, toY = -20f)

        then()
        assertEquals(1, fixture.dismissed)
        assertEquals(listOf(0, TOP - HEIGHT), listOf(fixture.message.left, fixture.message.top))
    }

    @Test
    fun `should keep a top message from being dragged down`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 100f, fromY = 60f, toX = 100f, toY = 160f)

        then()
        assertEquals(0, fixture.dismissed)
        assertEquals(TOP, fixture.message.top)
    }

    @Test
    fun `should ignore a drag that starts outside the message`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.drag(fromX = 100f, fromY = 400f, toX = 300f, toY = 400f)

        then()
        assertEquals(0, fixture.dismissed)
        assertEquals(0, fixture.message.left)
    }

    @Test
    fun `should keep the drag of the first finger when a second finger touches the message`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.startTouch()
        fixture.send(MotionEvent.ACTION_DOWN, 20f, 60f)
        (1..10).forEach {
            fixture.advance(10)
            fixture.send(MotionEvent.ACTION_MOVE, 20f + it * 16f, 60f)
        }

        whenn()
        fixture.advance(10)
        fixture.sendPointer(SECOND_FINGER_DOWN, listOf(180f, 190f), 60f)
        fixture.advance(10)
        fixture.sendPointer(SECOND_FINGER_UP, listOf(180f, 190f), 60f)
        fixture.advance(300)
        fixture.send(MotionEvent.ACTION_MOVE, 180f, 60f)
        fixture.advance(300)
        fixture.send(MotionEvent.ACTION_UP, 180f, 60f)
        fixture.settle()

        then()
        assertEquals(1, fixture.dismissed)
    }

    @Test
    fun `should leave a message alone when a touch on it never becomes a drag`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.startTouch()
        fixture.send(MotionEvent.ACTION_DOWN, 100f, 60f)
        fixture.send(MotionEvent.ACTION_UP, 100f, 60f)
        fixture.startTouch()
        fixture.send(MotionEvent.ACTION_DOWN, 100f, 60f)
        fixture.send(MotionEvent.ACTION_CANCEL, 100f, 60f)
        fixture.startTouch()
        fixture.send(MotionEvent.ACTION_DOWN, 100f, 60f)
        fixture.sendPointer(SECOND_FINGER_DOWN, listOf(100f, 150f), 60f)
        fixture.sendPointer(SECOND_FINGER_UP, listOf(100f, 150f), 60f)
        fixture.send(MotionEvent.ACTION_UP, 100f, 60f)
        fixture.settle()

        then()
        assertEquals(0, fixture.dismissed)
        assertEquals(listOf(0, TOP, 1f), listOf(fixture.message.left, fixture.message.top, fixture.message.alpha))
    }

    @Test
    fun `should finish settling a message taken off the screen mid-swipe`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.startTouch()
        fixture.send(MotionEvent.ACTION_DOWN, 20f, 60f)
        (1..10).forEach {
            fixture.advance(10)
            fixture.send(MotionEvent.ACTION_MOVE, 20f + it * 16f, 60f)
        }
        fixture.advance(300)
        fixture.send(MotionEvent.ACTION_MOVE, 180f, 60f)
        fixture.advance(300)
        fixture.send(MotionEvent.ACTION_UP, 180f, 60f)

        whenn()
        fixture.parent.removeView(fixture.message)
        val settled = runCatching { fixture.settle() }

        then()
        assertTrue(settled.isSuccess) { "${settled.exceptionOrNull()}" }
    }

    @Test
    fun `should let accessibility services dismiss the message`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val performed = ViewCompat.performAccessibilityAction(fixture.message, DISMISS, null)

        then()
        assertTrue(performed)
        assertEquals(1, fixture.dismissed)
        assertEquals(listOf(WIDTH, 0f), listOf(fixture.message.left, fixture.message.alpha))
    }

    @Test
    fun `should keep the accessibility setting the app chose for the message`() = runUnitTest {
        given()
        val fixture = SutFixture(accessibilityChosen = true)

        whenn()
        val performed = ViewCompat.performAccessibilityAction(fixture.message, DISMISS, null)

        then()
        assertFalse(performed)
        assertEquals(0, fixture.dismissed)
    }

    companion object {
        private const val WIDTH = 200
        private const val HEIGHT = 100
        private const val TOP = 10
        private const val SETTLE_FRAMES = 120
        private const val FRAME_MILLIS = 16L
        private val DISMISS = AccessibilityActionCompat.ACTION_DISMISS.id
        private const val SECOND_POINTER = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
        private const val SECOND_FINGER_DOWN = MotionEvent.ACTION_POINTER_DOWN or SECOND_POINTER
        private const val SECOND_FINGER_UP = MotionEvent.ACTION_POINTER_UP or SECOND_POINTER
    }
}
