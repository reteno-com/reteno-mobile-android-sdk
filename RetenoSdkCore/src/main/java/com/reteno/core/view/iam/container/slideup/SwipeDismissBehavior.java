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
    public static final int SWIPE_DIRECTION_ANY = 2;
    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5F;
    private static final float DEFAULT_ALPHA_START_DISTANCE = 0.0F;
    private static final float DEFAULT_ALPHA_END_DISTANCE = 0.5F;
    ViewDragHelper viewDragHelper;
    OnDismissListener listener;
    private boolean interceptingEvents;
    private float sensitivity = 0.0F;
    private boolean sensitivitySet;
    int swipeDirection = 2;
    float dragDismissThreshold = 0.5F;
    float alphaStartSwipeDistance = 0.0F;
    float alphaEndSwipeDistance = 0.5F;
    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        private static final int INVALID_POINTER_ID = -1;
        private int originalCapturedViewLeft;
        private int activePointerId = -1;

        public boolean tryCaptureView(View child, int pointerId) {
            return (this.activePointerId == -1 || this.activePointerId == pointerId) && SwipeDismissBehavior.this.canSwipeDismissView(child);
        }

        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            this.activePointerId = activePointerId;
            this.originalCapturedViewLeft = capturedChild.getLeft();
        }

        public void onViewDragStateChanged(int state) {
            if (SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDragStateChanged(state);
            }

        }

        public void onViewReleased(@NonNull View child, float xVelocity, float yVelocity) {
            this.activePointerId = -1;
            int childWidth = child.getWidth();
            boolean dismiss = false;
            int targetLeft;
            if (this.shouldDismiss(child, xVelocity)) {
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

        }

        private boolean shouldDismiss(@NonNull View child, float xVelocity) {
            if (xVelocity != 0.0F) {
                boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                if (SwipeDismissBehavior.this.swipeDirection == 2) {
                    return true;
                } else if (SwipeDismissBehavior.this.swipeDirection == 0) {
                    return isRtl ? xVelocity < 0.0F : xVelocity > 0.0F;
                } else if (SwipeDismissBehavior.this.swipeDirection == 1) {
                    return isRtl ? xVelocity > 0.0F : xVelocity < 0.0F;
                } else {
                    return false;
                }
            } else {
                int distance = child.getLeft() - this.originalCapturedViewLeft;
                int thresholdDistance = Math.round((float)child.getWidth() * SwipeDismissBehavior.this.dragDismissThreshold);
                return Math.abs(distance) >= thresholdDistance;
            }
        }

        public int getViewHorizontalDragRange(@NonNull View child) {
            return child.getWidth();
        }

        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
            int min;
            int max;
            if (SwipeDismissBehavior.this.swipeDirection == 0) {
                if (isRtl) {
                    min = this.originalCapturedViewLeft - child.getWidth();
                    max = this.originalCapturedViewLeft;
                } else {
                    min = this.originalCapturedViewLeft;
                    max = this.originalCapturedViewLeft + child.getWidth();
                }
            } else if (SwipeDismissBehavior.this.swipeDirection == 1) {
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
        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return child.getTop();
        }

        public void onViewPositionChanged(@NonNull View child, int left, int top, int dx, int dy) {
            float startAlphaDistance = (float)this.originalCapturedViewLeft + (float)child.getWidth() * SwipeDismissBehavior.this.alphaStartSwipeDistance;
            float endAlphaDistance = (float)this.originalCapturedViewLeft + (float)child.getWidth() * SwipeDismissBehavior.this.alphaEndSwipeDistance;
            if ((float)left <= startAlphaDistance) {
                child.setAlpha(1.0F);
            } else if ((float)left >= endAlphaDistance) {
                child.setAlpha(0.0F);
            } else {
                float distance = SwipeDismissBehavior.fraction(startAlphaDistance, endAlphaDistance, (float)left);
                child.setAlpha(SwipeDismissBehavior.clamp(0.0F, 1.0F - distance, 1.0F));
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

    public void setSwipeDirection(int direction) {
        this.swipeDirection = direction;
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
        switch (event.getActionMasked()) {
            case 0:
                this.interceptingEvents = parent.isPointInChildBounds(child, (int)event.getX(), (int)event.getY());
                dispatchEventToHelper = this.interceptingEvents;
                break;
            case 1:
            case 3:
                this.interceptingEvents = false;
            case 2:
        }

        if (dispatchEventToHelper) {
            this.ensureViewDragHelper(parent);
            return this.viewDragHelper.shouldInterceptTouchEvent(event);
        } else {
            return false;
        }
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean isInChildBounds = parent.isPointInChildBounds(child, (int)event.getX(), (int)event.getY());
        if (this.viewDragHelper != null && isInChildBounds) {
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
            ViewCompat.replaceAccessibilityAction(child, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_DISMISS, (CharSequence)null, new AccessibilityViewCommand() {
                public boolean perform(@NonNull View view, @Nullable AccessibilityViewCommand.CommandArguments arguments) {
                    if (SwipeDismissBehavior.this.canSwipeDismissView(view)) {
                        boolean isRtl = ViewCompat.getLayoutDirection(view) == 1;
                        boolean dismissToLeft = SwipeDismissBehavior.this.swipeDirection == 0 && isRtl || SwipeDismissBehavior.this.swipeDirection == 1 && !isRtl;
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
