package com.reteno.core.base.robolectric

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.database.util.*
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.DeeplinkController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.controller.ScreenTrackingController
import com.reteno.core.features.appinbox.AppInbox
import com.reteno.core.features.recommendation.Recommendation
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    application = RetenoTestApp::class,
    packageName = "com.reteno.core",
    shadows = [ShadowLooper::class, ShadowPackageManager::class]
)

abstract class BaseRobolectricTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic("com.reteno.core.util.UtilKt")
            mockkConstructor(ServiceLocator::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic("com.reteno.core.util.UtilKt")
            unmockkConstructor(ServiceLocator::class)
            clearAllMocks(
                answers = true,
                recordedCalls = true,
                childMocks = true,
                regularMocks = true,
                objectMocks = true,
                staticMocks = true,
                constructorMocks = true
            )
        }
    }

    protected val application by lazy {
        ApplicationProvider.getApplicationContext() as RetenoTestApp
    }

    protected val reteno: RetenoInternalImpl
        get() = requireNotNull(application.retenoMock)

    @RelaxedMockK
    protected lateinit var activityHelper: RetenoActivityHelper
    @RelaxedMockK
    internal lateinit var screenTrackingController: ScreenTrackingController
    @RelaxedMockK
    protected lateinit var contactController: ContactController
    @RelaxedMockK
    protected lateinit var scheduleController: ScheduleController
    @RelaxedMockK
    internal lateinit var eventController: EventController
    @RelaxedMockK
    internal lateinit var iamController: IamController
    @RelaxedMockK
    protected lateinit var sessionHandler: RetenoSessionHandler
    @RelaxedMockK
    protected lateinit var appLifecycleController: AppLifecycleController
    @RelaxedMockK
    protected lateinit var interactionController: InteractionController
    @RelaxedMockK
    internal lateinit var databaseManager: RetenoDatabaseManager
    @RelaxedMockK
    protected lateinit var deeplinkController: DeeplinkController
    @RelaxedMockK
    internal lateinit var sharedPrefsManager: SharedPrefsManager
    @RelaxedMockK
    internal lateinit var restConfig: RestConfig
    @RelaxedMockK
    protected lateinit var appInbox: AppInbox
    @RelaxedMockK
    protected lateinit var recommendation: Recommendation
    @RelaxedMockK
    protected lateinit var iamView: IamView

    @Before
    @Throws(Exception::class)
    open fun before() {
        MockKAnnotations.init(this)

        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().retenoSessionHandlerProvider.get() } returns sessionHandler
        every { anyConstructed<ServiceLocator>().interactionControllerProvider.get() } returns interactionController
        every { anyConstructed<ServiceLocator>().retenoDatabaseManagerProvider.get() } returns databaseManager
        every { anyConstructed<ServiceLocator>().deeplinkControllerProvider.get() } returns deeplinkController
        every { anyConstructed<ServiceLocator>().sharedPrefsManagerProvider.get() } returns sharedPrefsManager
        every { anyConstructed<ServiceLocator>().restConfigProvider.get() } returns restConfig
        every { anyConstructed<ServiceLocator>().recommendationProvider.get() } returns recommendation
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
        every { anyConstructed<ServiceLocator>().iamControllerProvider.get() } returns iamController
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns activityHelper
        every { anyConstructed<ServiceLocator>().appInboxProvider.get() } returns appInbox
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns screenTrackingController
        every { anyConstructed<ServiceLocator>().appLifecycleControllerProvider.get() } returns appLifecycleController

    }

    @After
    open fun after() {
        application.retenoMock = mockk()
        // Nothing here yet
    }

    protected fun TestScope.createRetenoAndAdvanceInit(
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    ): RetenoInternalImpl {
        return RetenoInternalImpl(
            application = application,
            mainDispatcher = StandardTestDispatcher(testScheduler),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            appLifecycleOwner = lifecycleOwner
        ).also {
            RetenoInternalImpl.swapInstance(it)
            Reteno.initWithConfig(
                RetenoConfig.Builder()
                    .accessKey("Test access key")
                    .setPlatform("Android")
                    .build()
            )
            application.retenoMock = it
            while (!it.isInitialized) {
                advanceUntilIdle()
            }
        }
    }

    fun runRetenoTest(
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
        test: TestScope.(RetenoInternalImpl) -> Unit
    ) = runTest {
        test(createRetenoAndAdvanceInit(lifecycleOwner))
        RetenoInternalImpl.swapInstance(null)
    }
}