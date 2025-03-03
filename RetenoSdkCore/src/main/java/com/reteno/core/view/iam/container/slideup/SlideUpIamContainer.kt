package com.reteno.core.view.iam.container.slideup

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.contains
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.IamViewImpl
import com.reteno.core.view.iam.container.IamContainer
import com.reteno.core.view.iam.container.IamContainer.IamDismissListener

internal class SlideUpIamContainer(
    context: Context,
    jsInterface: RetenoAndroidHandler,
    private val fetchResult: IamFetchResult,
    private val iamDismissListener: IamDismissListener
) : IamContainer {
    private val parentLayout = CoordinatorLayout(context.applicationContext)
    private val webView = createWebView(context.applicationContext, jsInterface)

    init {
        addWebViewToParent()
    }

    private fun addWebViewToParent() {
        /*@formatter:off*/ Logger.i(TAG, "addWebViewToCardView(): ", "")
        /*@formatter:on*/
        val swipe = SwipeDismissBehavior<WebView>().apply {
            setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
        }
        swipe.setListener(object : SwipeDismissBehavior.OnDismissListener {
            override fun onDismiss(var1: View?) {
                iamDismissListener.onIamDismissed()
            }

            override fun onDragStateChanged(var1: Int) {
            }

        })
        parentLayout.addView(
            webView,
            CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                behavior = swipe
                val position = fetchResult.layoutParams.position ?: Position.TOP
                gravity = when (position) {
                    Position.TOP -> Gravity.TOP
                    Position.BOTTOM -> Gravity.BOTTOM
                }
            }
        )
    }

    private fun createWebView(context: Context, jsInterface: RetenoAndroidHandler): WebView {
        /*@formatter:off*/ Logger.i(TAG, "createWebView(): ", "context = [", context, "]")
        /*@formatter:on*/
        val webView = WebView(context)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WEB VIEW TAG",
                    consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                )
                return true
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
        }
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(jsInterface, IamViewImpl.JS_INTERFACE_NAME)
        webView.loadDataWithBaseURL("", fetchResult.fullHtml, "text/html", "UTF-8", "")
        return webView
    }

    override fun dismiss() {
        try {
            val container = parentLayout.parent as ViewGroup
            container.removeView(parentLayout)
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "dismiss(): SlideUpIamContainer.dismiss() ", e)
            /*@formatter:on*/
        }
    }

    override fun destroy() {
        try {
            val container = parentLayout.parent as? ViewGroup
            //To stop propagating unexpected scroll event to Activity convent View
            parentLayout.isVisible = false
            container?.removeView(parentLayout)
            parentLayout.removeAllViews()
            webView.removeJavascriptInterface(IamViewImpl.JS_INTERFACE_NAME)
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "destroy(): SlideUpIamContainer.destroy() ", e)
            /*@formatter:on*/
        }
    }

    override fun show(activity: Activity) {
        val container = activity.findViewById<ViewGroup>(android.R.id.content)
        if (!container.contains(parentLayout)) {
            activity.addContentView(
                parentLayout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            webView.doOnLayout {
                val position = fetchResult.layoutParams.position ?: Position.TOP
                when (position) {
                    Position.TOP -> {
                        webView.translationY = -webView.height.toFloat()
                        webView.animate()
                            .translationY(0f)
                            .setInterpolator(LinearInterpolator())
                            .setDuration(200L)
                            .start()
                    }

                    Position.BOTTOM -> {
                        webView.translationY = webView.height.toFloat()
                        webView.animate()
                            .translationY(0f)
                            .setInterpolator(LinearInterpolator())
                            .setDuration(200L)
                            .start()
                    }
                }
            }
        }
    }

    override fun onHeightDefined(newHeight: Int) {
        webView.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            height = newHeight
        }
    }

    companion object {
        private val TAG = SlideUpIamContainer::class.simpleName.orEmpty()
    }
}