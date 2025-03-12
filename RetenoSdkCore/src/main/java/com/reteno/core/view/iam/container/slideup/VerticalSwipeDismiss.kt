package com.reteno.core.view.iam.container.slideup

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VerticalSwipeDismissBehavior<V : View> @JvmOverloads constructor(context: Context? = null, attrs: AttributeSet? = null) : CoordinatorLayout.Behavior<V>(context, attrs) {

    interface OnDismissListener {
        fun onDismiss(view: View)
    }

    private val defaultListener = object : OnDismissListener {
        override fun onDismiss(view: View) {
            // no op
        }
    }
    var dismissListener: OnDismissListener = defaultListener
    private var viewDragHelper: ViewDragHelper? = null
    private var interceptingEvents = false
    private var sensitivity = 1f
    var dragDismissThreshold = DEFAULT_DRAG_DISMISS_THRESHOLD
    var alphaStartSwipeDistance = DEFAULT_ALPHA_START_DISTANCE
    var alphaEndSwipeDistance = DEFAULT_ALPHA_END_DISTANCE

    private val dragCallback = object : ViewDragHelper.Callback() {
        private val INVALID_POINTER_ID = -1

        private var activePointerId = INVALID_POINTER_ID
        private var originalCapturedViewTop = 0

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return activePointerId == INVALID_POINTER_ID || activePointerId == pointerId
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            this.activePointerId = activePointerId
            originalCapturedViewTop = capturedChild.top
        }

        override fun onViewReleased(child: View, xvel: Float, yvel: Float) {
            activePointerId = INVALID_POINTER_ID

            val targetTop: Int
            var dismiss = false

            if (shouldDismiss(child, xvel, yvel)) {
                targetTop = if (child.top < originalCapturedViewTop) {
                    originalCapturedViewTop - (child.height * dragDismissThreshold).toInt()
                } else if (child.top > originalCapturedViewTop) {
                    originalCapturedViewTop + (child.height * dragDismissThreshold).toInt()
                } else {
                    originalCapturedViewTop
                }
                dismiss = true
            } else {
                targetTop = originalCapturedViewTop
            }

            viewDragHelper?.run {
                if (settleCapturedViewAt(child.left, targetTop)) {
                    ViewCompat.postOnAnimation(child, SettleRunnable(child, dismiss))
                } else if (dismiss) {
                    dismissListener.onDismiss(child)
                }
            }
        }

        private fun shouldDismiss(child: View, xvel: Float, yvel: Float): Boolean {
            return abs(yvel) > abs(xvel)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return child.height
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val min = originalCapturedViewTop - child.height
            val max = originalCapturedViewTop + child.height

            return constrain(min, max, top)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun onViewPositionChanged(child: View, left: Int, top: Int, dx: Int, dy: Int) {
            val diffY = max(top, originalCapturedViewTop) - min(top, originalCapturedViewTop)

            val startAlphaDistance = child.height * alphaStartSwipeDistance
            val endAlphaDistance = child.height * alphaEndSwipeDistance

            when {
                diffY <= startAlphaDistance -> {
                    child.alpha = 1f
                }
                diffY >= endAlphaDistance -> {
                    child.alpha = 0f
                }
                else -> {
                    val distance = normalize(startAlphaDistance, endAlphaDistance, diffY.toFloat())
                    child.alpha = constrain(0f, 1f, 1f - distance)
                }
            }
        }

        private fun constrain(min: Int, max: Int, value: Int): Int {
            return when {
                value < min -> min
                value > max -> max
                else -> value
            }
        }

        private fun constrain(min: Float, max: Float, value: Float): Float {
            return when {
                value < min -> min
                value > max -> max
                else -> value
            }
        }

        private fun normalize(start: Float, end: Float, value: Float): Float {
            return (value - start) / (end - start)
        }
    }

    inner class SettleRunnable(var view: View, var dismiss: Boolean) : Runnable {

        override fun run() {
            if (viewDragHelper?.continueSettling(true) == true) {
                ViewCompat.postOnAnimation(view, this)
            } else {
                if (dismiss) {
                    dismissListener.onDismiss(view)
                }
            }
        }

    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        var dispatchEventHelper = interceptingEvents

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interceptingEvents = parent.isPointInChildBounds(child, event.x.toInt(), event.y.toInt())
                dispatchEventHelper = interceptingEvents
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                interceptingEvents = false
            }
        }

        if (dispatchEventHelper) {
            if (viewDragHelper == null) {
                viewDragHelper = ViewDragHelper.create(parent, sensitivity, dragCallback)
            }
            return viewDragHelper?.shouldInterceptTouchEvent(event) == true
        }
        return false
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (viewDragHelper != null) {
            viewDragHelper?.processTouchEvent(event)
            return true
        }
        return false
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V,
                                     directTargetChild: View, target: View, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int,
                                   dy: Int, consumed: IntArray) {

    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View) {
    }

    companion object {
        const val DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f
        const val DEFAULT_ALPHA_START_DISTANCE = 0f
        const val DEFAULT_ALPHA_END_DISTANCE = DEFAULT_DRAG_DISMISS_THRESHOLD

        const val STATE_IDLE = ViewDragHelper.STATE_IDLE
        const val STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING
        const val STATE_SETTLING = ViewDragHelper.STATE_SETTLING

        fun from(view: View): VerticalSwipeDismissBehavior<out View> {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams
                ?: throw IllegalArgumentException("The view is not a child of CoordinatorLayout")
            return params.behavior as? VerticalSwipeDismissBehavior
                ?: throw java.lang.IllegalArgumentException("he view is not associated with VerticalSwipeDismissBehavior")
        }
    }
}