package com.reteno.core.base.robolectric

import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.IamController
import com.reteno.core.features.recommendation.Recommendation
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    internal lateinit var facadeConfigRepository: ConfigRepository
    @RelaxedMockK
    internal lateinit var iamController: IamController
    @RelaxedMockK
    protected lateinit var sessionHandler: RetenoSessionHandler
    @RelaxedMockK
    internal lateinit var databaseManager: RetenoDatabaseManager
    @RelaxedMockK
    internal lateinit var sharedPrefsManager: SharedPrefsManager
    @RelaxedMockK
    internal lateinit var fileManager: FileManager
    @RelaxedMockK
    internal lateinit var restConfig: RestConfig
    @RelaxedMockK
    protected lateinit var recommendation: Recommendation
    @RelaxedMockK
    protected lateinit var iamView: IamView

    @Before
    @Throws(Exception::class)
    open fun before() {
        MockKAnnotations.init(this)

        every { anyConstructed<ServiceLocator>().retenoSessionHandlerProvider.get() } returns sessionHandler
        every { anyConstructed<ServiceLocator>().retenoDatabaseManagerProvider.get() } returns databaseManager
        every { anyConstructed<ServiceLocator>().sharedPrefsManagerProvider.get() } returns sharedPrefsManager
        every { anyConstructed<ServiceLocator>().restConfigProvider.get() } returns restConfig
        every { anyConstructed<ServiceLocator>().recommendationProvider.get() } returns recommendation
        every { anyConstructed<ServiceLocator>().configRepositoryProvider.get() } returns facadeConfigRepository
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().iamControllerProvider.get() } returns iamController
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns activityHelper

    }

    @After
    open fun after() {
        application.retenoMock = mockk()
        // Nothing here yet
    }

}