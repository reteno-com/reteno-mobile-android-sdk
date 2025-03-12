package com.reteno.core.view.iam.container.slideup;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.customview.widget.ViewDragHelper;


public class SwipeDismissBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int SWIPE_DIRECTION_START_TO_END = 0;
    public static final int SWIPE_DIRECTION_END_TO_START = 1;
    public static final int SWIPE_DIRECTION_HORIZONTAL = 2;
    public static final int SWIPE_DIRECTION_BOTTOM_TO_TOP = 0;
    public static final int SWIPE_DIRECTION_TOP_TO_BOTTOM = 1;
    public static final int SWIPE_DIRECTION_VERTICAL = 2;
    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5F;
    private static final float DEFAULT_ALPHA_START_DISTANCE = 0.0F;
    private static final float DEFAULT_ALPHA_END_DISTANCE = 0.5F;
    ViewDragHelper viewDragHelper;
    OnDismissListener listener;
    private boolean interceptingEvents;
    private float sensitivity = 0.0F;
    private boolean sensitivitySet;
    int horizontalSwipeDirection = 2;
    int verticalSwipeDirection = 2;
    float dragDismissThreshold = 0.5F;
    float alphaStartSwipeDistance = 0.0F;
    float alphaEndSwipeDistance = 0.5F;
    private boolean draggingHorizontally = false;
    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        private static final int INVALID_POINTER_ID = -1;
        private int originalCapturedViewLeft;
        private int originalCapturedViewTop;
        private int activePointerId = -1;

        public boolean tryCaptureView(View child, int pointerId) {
            return (this.activePointerId == -1 || this.activePointerId == pointerId) && SwipeDismissBehavior.this.canSwipeDismissView(child);
        }

        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            this.activePointerId = activePointerId;
            this.originalCapturedViewLeft = capturedChild.getLeft();
            this.originalCapturedViewTop = capturedChild.getTop();
        }

        public void onViewDragStateChanged(int state) {
            if (SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDragStateChanged(state);
            }
        }

        public void onViewReleased(@NonNull View child, float xVelocity, float yVelocity) {
            this.activePointerId = -1;
            int childWidth = child.getWidth();
            int childHeight = child.getHeight();
            boolean dismiss = false;
            int targetLeft;
            int targetTop;

            if (draggingHorizontally) {
                if (this.shouldDismissX(child, xVelocity)) {
                    targetLeft = !(xVelocity < 0.0F) && child.getLeft() >= this.originalCapturedViewLeft ? this.originalCapturedViewLeft + childWidth : this.originalCapturedViewLeft - childWidth;
                    dismiss = true;
                } else {
                    targetLeft = this.originalCapturedViewLeft;
                }
                if (SwipeDismissBehavior.this.viewDragHelper.settleCapturedViewAt(targetLeft, child.getTop())) {
                    ViewCompat.postOnAnimation(child, SwipeDismissBehavior.this.new SettleRunnable(child, dismiss));
                } else if (dismiss && SwipeDismissBehavior.this.listener != null) {
                    SwipeDismissBehavior.this.listener.onDismiss(child);
                }
            } else {
                if (this.shouldDismissY(child, yVelocity)) {
                    targetTop = child.getTop() >= this.originalCapturedViewTop ? this.originalCapturedViewTop + childHeight : this.originalCapturedViewTop - childHeight;
                    dismiss = true;
                } else {
                    targetTop = this.originalCapturedViewTop;
                }
                if (SwipeDismissBehavior.this.viewDragHelper.settleCapturedViewAt(child.getLeft(), targetTop)) {
                    ViewCompat.postOnAnimation(child, SwipeDismissBehavior.this.new SettleRunnable(child, dismiss));
                } else if (dismiss && SwipeDismissBehavior.this.listener != null) {
                    SwipeDismissBehavior.this.listener.onDismiss(child);
                }
            }

        }

        private boolean shouldDismissX(@NonNull View child, float xVelocity) {
            if (xVelocity != 0.0F) {
                boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                if (SwipeDismissBehavior.this.horizontalSwipeDirection == 2) {
                    return true;
                } else if (SwipeDismissBehavior.this.horizontalSwipeDirection == 0) {
                    return isRtl ? xVelocity < 0.0F : xVelocity > 0.0F;
                } else if (SwipeDismissBehavior.this.horizontalSwipeDirection == 1) {
                    return isRtl ? xVelocity > 0.0F : xVelocity < 0.0F;
                } else {
                    return false;
                }
            } else {
                int distance = child.getLeft() - this.originalCapturedViewLeft;
                int thresholdDistance = Math.round((float) child.getWidth() * SwipeDismissBehavior.this.dragDismissThreshold);
                return Math.abs(distance) >= thresholdDistance;
            }
        }

        private boolean shouldDismissY(@NonNull View child, float yVelocity) {
            if (yVelocity != 0.0F) {
                if (SwipeDismissBehavior.this.verticalSwipeDirection == SWIPE_DIRECTION_VERTICAL) {
                    return true;
                } else if (SwipeDismissBehavior.this.verticalSwipeDirection == SWIPE_DIRECTION_BOTTOM_TO_TOP) {
                    return yVelocity < 0.0F;
                } else if (SwipeDismissBehavior.this.verticalSwipeDirection == SWIPE_DIRECTION_TOP_TO_BOTTOM) {
                    return yVelocity > 0.0F;
                } else {
                    return false;
                }
            } else {
                int distance = Math.abs(child.getTop() - this.originalCapturedViewTop);
                int thresholdDistance = Math.round((float) child.getHeight() * SwipeDismissBehavior.this.dragDismissThreshold);
                return Math.abs(distance) >= thresholdDistance;
            }
        }

        public int getViewHorizontalDragRange(@NonNull View child) {
            return draggingHorizontally ? child.getWidth() : 0;
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return !draggingHorizontally ? child.getHeight() : 0;
        }

        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            if (draggingHorizontally) {
                boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                int min;
                int max;
                if (SwipeDismissBehavior.this.horizontalSwipeDirection == 0) {
                    if (isRtl) {
                        min = this.originalCapturedViewLeft - child.getWidth();
                        max = this.originalCapturedViewLeft;
                    } else {
                        min = this.originalCapturedViewLeft;
                        max = this.originalCapturedViewLeft + child.getWidth();
                    }
                } else if (SwipeDismissBehavior.this.horizontalSwipeDirection == 1) {
                    if (isRtl) {
                        min = this.originalCapturedViewLeft;
                        max = this.originalCapturedViewLeft + child.getWidth();
                    } else {
                        min = this.originalCapturedViewLeft - child.getWidth();
                        max = this.originalCapturedViewLeft;
                    }
                } else {
                    min = this.originalCapturedViewLeft - child.getWidth();
                    max = this.originalCapturedViewLeft + child.getWidth();
                }

                return SwipeDismissBehavior.clamp(min, left, max);
            } else {
                return child.getLeft();
            }
        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            if (!draggingHorizontally) {
                int min;
                int max;
                if (SwipeDismissBehavior.this.verticalSwipeDirection == SWIPE_DIRECTION_TOP_TO_BOTTOM) {
                    min = originalCapturedViewTop;
                    max = originalCapturedViewTop + child.getHeight();
                } else if (SwipeDismissBehavior.this.verticalSwipeDirection == SWIPE_DIRECTION_BOTTOM_TO_TOP) {
                    min = originalCapturedViewTop - child.getHeight();
                    max = originalCapturedViewTop;
                } else {
                    min = originalCapturedViewTop - child.getHeight();
                    max = originalCapturedViewTop + child.getHeight();
                }
                return clamp(min, top, max);
            } else {
                return child.getTop();
            }
        }

        public void onViewPositionChanged(@NonNull View child, int left, int top, int dx, int dy) {
            if (draggingHorizontally) {
                float diffX = Math.abs(originalCapturedViewLeft - left);
                float startAlphaDistanceX = child.getWidth() * alphaStartSwipeDistance;
                float endAlphaDistanceX = child.getWidth() * alphaEndSwipeDistance;

                if ((float) diffX <= startAlphaDistanceX) {
                    child.setAlpha(1.0F);
                } else if ((float) diffX >= endAlphaDistanceX) {
                    child.setAlpha(0.0F);
                } else {
                    float distance = SwipeDismissBehavior.fraction(startAlphaDistanceX, endAlphaDistanceX, diffX);
                    child.setAlpha(SwipeDismissBehavior.clamp(0.0F, 1.0F - distance, 1.0F));
                }
            } else {
                float diffY = Math.max(top, originalCapturedViewTop) - Math.min(top, originalCapturedViewTop);
                float startAlphaDistanceY = child.getHeight() * alphaStartSwipeDistance;
                float endAlphaDistanceY = child.getHeight() * alphaEndSwipeDistance;
                if (diffY <= startAlphaDistanceY) {
                    child.setAlpha(1.0F);
                } else if (diffY >= endAlphaDistanceY) {
                    child.setAlpha(0.0F);
                } else {
                    float distance = SwipeDismissBehavior.fraction(startAlphaDistanceY, endAlphaDistanceY, diffY);
                    child.setAlpha(SwipeDismissBehavior.clamp(0.0F, 1.0F - distance, 1.0F));
                }
            }
        }
    };

    public SwipeDismissBehavior() {
    }

    public void setListener(@Nullable OnDismissListener listener) {
        this.listener = listener;
    }

    @VisibleForTesting
    @Nullable
    public OnDismissListener getListener() {
        return this.listener;
    }

    public void setHorizontalSwipeDirection(int direction) {
        this.horizontalSwipeDirection = direction;
    }

    public void setVerticalSwipeDirection(int direction) {
        this.verticalSwipeDirection = direction;
    }

    public void setDragDismissDistance(float distance) {
        this.dragDismissThreshold = clamp(0.0F, distance, 1.0F);
    }

    public void setStartAlphaSwipeDistance(float fraction) {
        this.alphaStartSwipeDistance = clamp(0.0F, fraction, 1.0F);
    }

    public void setEndAlphaSwipeDistance(float fraction) {
        this.alphaEndSwipeDistance = clamp(0.0F, fraction, 1.0F);
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
        this.sensitivitySet = true;
    }

    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        boolean handled = super.onLayoutChild(parent, child, layoutDirection);
        if (ViewCompat.getImportantForAccessibility(child) == 0) {
            ViewCompat.setImportantForAccessibility(child, 1);
            this.updateAccessibilityActions(child);
        }

        return handled;
    }

    public boolean onInterceptTouchEvent(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull MotionEvent event) {
        boolean dispatchEventToHelper = this.interceptingEvents;
        this.ensureViewDragHelper(parent);
        switch (event.getActionMasked()) {
            case 0:
                this.interceptingEvents = parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY());
                dispatchEventToHelper = this.interceptingEvents;
                break;
            case 1:
            case 3:
                this.interceptingEvents = false;
                draggingHorizontally = false;
            case 2:
                draggingHorizontally = viewDragHelper.checkTouchSlop(ViewDragHelper.DIRECTION_HORIZONTAL);
        }

        if (dispatchEventToHelper) {
            return this.viewDragHelper.shouldInterceptTouchEvent(event);
        } else {
            return false;
        }
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (this.viewDragHelper != null && interceptingEvents) {
            this.viewDragHelper.processTouchEvent(event);
            return true;
        } else {
            return false;
        }
    }

    public boolean canSwipeDismissView(@NonNull View view) {
        return true;
    }

    private void ensureViewDragHelper(ViewGroup parent) {
        if (this.viewDragHelper == null) {
            this.viewDragHelper = this.sensitivitySet ? ViewDragHelper.create(parent, this.sensitivity, this.dragCallback) : ViewDragHelper.create(parent, this.dragCallback);
        }

    }

    private void updateAccessibilityActions(View child) {
        ViewCompat.removeAccessibilityAction(child, 1048576);
        if (this.canSwipeDismissView(child)) {
            ViewCompat.replaceAccessibilityAction(child, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_DISMISS, (CharSequence) null, new AccessibilityViewCommand() {
                public boolean perform(@NonNull View view, @Nullable AccessibilityViewCommand.CommandArguments arguments) {
                    if (SwipeDismissBehavior.this.canSwipeDismissView(view)) {
                        boolean isRtl = ViewCompat.getLayoutDirection(view) == 1;
                        boolean dismissToLeft = SwipeDismissBehavior.this.horizontalSwipeDirection == 0 && isRtl || SwipeDismissBehavior.this.horizontalSwipeDirection == 1 && !isRtl;
                        int offset = dismissToLeft ? -view.getWidth() : view.getWidth();
                        ViewCompat.offsetLeftAndRight(view, offset);
                        view.setAlpha(0.0F);
                        if (SwipeDismissBehavior.this.listener != null) {
                            SwipeDismissBehavior.this.listener.onDismiss(view);
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

    }

    static float clamp(float min, float value, float max) {
        return Math.min(Math.max(min, value), max);
    }

    static int clamp(int min, int value, int max) {
        return Math.min(Math.max(min, value), max);
    }

    public int getDragState() {
        return this.viewDragHelper != null ? this.viewDragHelper.getViewDragState() : 0;
    }

    static float fraction(float startValue, float endValue, float value) {
        return (value - startValue) / (endValue - startValue);
    }

    private class SettleRunnable implements Runnable {
        private final View view;
        private final boolean dismiss;

        SettleRunnable(View view, boolean dismiss) {
            this.view = view;
            this.dismiss = dismiss;
        }

        public void run() {
            if (SwipeDismissBehavior.this.viewDragHelper != null && SwipeDismissBehavior.this.viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.view, this);
            } else if (this.dismiss && SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDismiss(this.view);
            }

        }
    }

    public interface OnDismissListener {
        void onDismiss(View var1);

        void onDragStateChanged(int var1);
    }
}
