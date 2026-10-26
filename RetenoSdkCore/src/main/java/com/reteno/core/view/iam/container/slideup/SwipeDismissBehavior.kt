package com.reteno.core.view.iam.container.slideup

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.customview.widget.ViewDragHelper

internal class SwipeDismissBehavior<V : View>(
    private val gesture: SwipeGesture,
    private val onDismiss: () -> Unit
) : CoordinatorLayout.Behavior<V>() {

    private var dragHelper: ViewDragHelper? = null
    private var interceptingEvents = false
    private var draggingHorizontally = false

    private val dragCallback = object : ViewDragHelper.Callback() {
        private var originLeft = 0
        private var originTop = 0
        private var activePointerId = NO_POINTER

        override fun tryCaptureView(child: View, pointerId: Int): Boolean = activePointerId == NO_POINTER

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            this.activePointerId = activePointerId
            originLeft = capturedChild.left
            originTop = capturedChild.top
        }

        override fun onViewReleased(child: View, xvel: Float, yvel: Float) {
            activePointerId = NO_POINTER
            val helper = dragHelper(child.parent as ViewGroup)
            val settling = if (draggingHorizontally) {
                val release = gesture.releaseHorizontally(originLeft, child.left, child.width, xvel)
                helper.settleCapturedViewAt(release.target, child.top) to release.dismiss
            } else {
                val release = gesture.releaseVertically(originTop, child.top, child.height, yvel)
                helper.settleCapturedViewAt(child.left, release.target) to release.dismiss
            }
            settle(child, helper, moving = settling.first, dismiss = settling.second)
        }

        override fun getViewHorizontalDragRange(child: View): Int = if (draggingHorizontally) child.width else 0

        override fun getViewVerticalDragRange(child: View): Int = if (draggingHorizontally) 0 else child.height

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int =
            if (draggingHorizontally) gesture.clampLeft(originLeft, left, child.width) else child.left

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int =
            if (draggingHorizontally) child.top else gesture.clampTop(originTop, top, child.height)

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            changedView.alpha = if (draggingHorizontally) {
                gesture.alpha(left - originLeft, changedView.width)
            } else {
                gesture.alpha(top - originTop, changedView.height)
            }
        }
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        val handled = super.onLayoutChild(parent, child, layoutDirection)
        if (ViewCompat.getImportantForAccessibility(child) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
            ViewCompat.replaceAccessibilityAction(child, AccessibilityActionCompat.ACTION_DISMISS, null) { view, _ ->
                ViewCompat.offsetLeftAndRight(view, view.width)
                view.alpha = 0f
                onDismiss()
                true
            }
        }
        return handled
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        val helper = dragHelper(parent)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interceptingEvents = parent.isPointInChildBounds(child, ev.x.toInt(), ev.y.toInt())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> interceptingEvents = false
            MotionEvent.ACTION_MOVE -> draggingHorizontally = helper.checkTouchSlop(ViewDragHelper.DIRECTION_HORIZONTAL)
        }
        return interceptingEvents && helper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        if (!interceptingEvents) return false
        dragHelper(parent).processTouchEvent(ev)
        return true
    }

    private fun settle(child: View, helper: ViewDragHelper, moving: Boolean, dismiss: Boolean) {
        if (moving) {
            ViewCompat.postOnAnimation(child) { settle(child, helper, helper.continueSettling(true), dismiss) }
        } else if (dismiss) {
            onDismiss()
        }
    }

    private fun dragHelper(parent: ViewGroup): ViewDragHelper =
        dragHelper ?: ViewDragHelper.create(parent, dragCallback).also { dragHelper = it }

    private companion object {
        const val NO_POINTER = -1
    }
}
