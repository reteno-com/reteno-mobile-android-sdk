package com.reteno.core.view.iam.container

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.view.iam.AndroidInAppScreen
import com.reteno.core.view.iam.container.slideup.SlideUpIamContainer
import com.reteno.core.view.iam.container.slideup.SwipeDismissBehavior
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class IamContainersTest {

    private class SutFixture(layoutType: InAppLayoutType, position: Position? = Position.TOP) {
        val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val bridge = object : RetenoAndroidHandler() {
            override fun onMessagePosted(event: String?) = Unit
        }
        var dismissed = 0
        val result = IamFetchResult("html-1", HTML, layoutType, InAppLayoutParams(position))
        val sut = AndroidIamContainers.create(activity, result, bridge) { dismissed++ }

        val content: ViewGroup
            get() = activity.findViewById(android.R.id.content)

        fun slideUpLayout(): CoordinatorLayout? = (0 until content.childCount)
            .map { content.getChildAt(it) }
            .filterIsInstance<CoordinatorLayout>()
            .singleOrNull()

        fun slideUpWebView(): WebView = slideUpLayout()!!.getChildAt(0) as WebView

        fun popupWindowViews(): List<View> = Shadow.extract<ShadowWindowManagerImpl>(activity.windowManager).views
            .filter { it.javaClass.name.contains("Popup") }

    }

    @Test
    fun `should show full and pop-up messages in a full screen window`() = runUnitTest {
        given()
        val fixtures = listOf(SutFixture(InAppLayoutType.FULL), SutFixture(InAppLayoutType.POP_UP))

        whenn()
        val shownHtml = fixtures.map { fixture ->
            fixture.sut.show(fixture.activity)
            val webView = (fixture.popupWindowViews().single() as ViewGroup).findWebView()
            fixture.sut.dismiss()
            shadowOf(webView).lastLoadDataWithBaseURL.data
        }

        then()
        assertEquals(listOf(HTML, HTML), shownHtml)
        assertTrue(fixtures.all { it.sut is FullscreenIamContainer })
    }

    @Test
    fun `should close the full screen window and drop its bridge`() = runUnitTest {
        given()
        val fixture = SutFixture(InAppLayoutType.FULL)
        fixture.sut.show(fixture.activity)
        val webView = (fixture.popupWindowViews().single() as ViewGroup).findWebView()

        whenn()
        fixture.sut.onHeightDefined(300)
        fixture.sut.dismiss()
        fixture.sut.show(fixture.activity)
        fixture.sut.destroy()

        then()
        assertEquals(emptyList<View>(), fixture.popupWindowViews())
        assertNull(shadowOf(webView).getJavascriptInterface(AndroidInAppScreen.JS_INTERFACE_NAME))
    }

    @Test
    fun `should show slide-up and bottom bar messages on top of the activity once`() = runUnitTest {
        given()
        val fixtures = listOf(SutFixture(InAppLayoutType.SLIDE_UP), SutFixture(InAppLayoutType.BOTTOM_BAR))

        whenn()
        fixtures.forEach {
            it.sut.show(it.activity)
            it.sut.show(it.activity)
        }

        then()
        fixtures.forEach { fixture ->
            assertTrue(fixture.sut is SlideUpIamContainer)
            assertEquals(HTML, shadowOf(fixture.slideUpWebView()).lastLoadDataWithBaseURL.data)
        }
    }

    @Test
    fun `should pin a slide-up to its edge and let it be swiped away from there`() = runUnitTest {
        given()
        val fixtures = listOf(Position.TOP, null, Position.BOTTOM).map { SutFixture(InAppLayoutType.SLIDE_UP, it) }

        whenn()
        fixtures.forEach { it.sut.show(it.activity) }

        then()
        val gravities = fixtures.map {
            (it.slideUpWebView().layoutParams as CoordinatorLayout.LayoutParams).gravity
        }
        assertEquals(listOf(Gravity.TOP, Gravity.TOP, Gravity.BOTTOM), gravities)
        fixtures.forEach {
            val params = it.slideUpWebView().layoutParams as CoordinatorLayout.LayoutParams
            assertTrue(params.behavior is SwipeDismissBehavior<*>)
        }
    }

    @Test
    fun `should come to rest after sliding in from its edge`() = runUnitTest {
        given()
        val fixtures = listOf(Position.TOP, Position.BOTTOM).map { SutFixture(InAppLayoutType.SLIDE_UP, it) }

        whenn()
        fixtures.forEach {
            it.sut.show(it.activity)
            ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        }

        then()
        assertEquals(listOf(0f, 0f), fixtures.map { it.slideUpWebView().translationY })
    }

    @Test
    fun `should size a slide-up to the height its content asks for`() = runUnitTest {
        given()
        val fixture = SutFixture(InAppLayoutType.SLIDE_UP)
        fixture.sut.show(fixture.activity)

        whenn()
        fixture.sut.onHeightDefined(250)

        then()
        assertEquals(250, fixture.slideUpWebView().layoutParams.height)
    }

    @Test
    fun `should dismiss a slide-up the user swipes away`() = runUnitTest {
        given()
        val fixture = SutFixture(InAppLayoutType.SLIDE_UP)
        fixture.sut.show(fixture.activity)
        fixture.slideUpLayout()!!.run {
            measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
            layout(0, 0, 400, 800)
        }

        whenn()
        val performed = ViewCompat.performAccessibilityAction(
            fixture.slideUpWebView(),
            AccessibilityActionCompat.ACTION_DISMISS.id,
            null
        )

        then()
        assertTrue(performed)
        assertEquals(1, fixture.dismissed)
    }

    @Test
    fun `should take a slide-up off the activity and drop its bridge`() = runUnitTest {
        given()
        val fixture = SutFixture(InAppLayoutType.SLIDE_UP)
        fixture.sut.dismiss()
        fixture.sut.show(fixture.activity)
        val webView = fixture.slideUpWebView()

        whenn()
        fixture.sut.dismiss()
        val afterDismiss = fixture.slideUpLayout()
        fixture.sut.show(fixture.activity)
        fixture.sut.destroy()

        then()
        assertNull(afterDismiss)
        assertNull(fixture.slideUpLayout())
        assertNull(shadowOf(webView).getJavascriptInterface(AndroidInAppScreen.JS_INTERFACE_NAME))
    }

    private fun ViewGroup.findWebView(): WebView {
        val child = getChildAt(0)
        return child as? WebView ?: (child as ViewGroup).findWebView()
    }

    companion object {
        private const val HTML = "<html><body>Sale</body></html>"
    }
}
