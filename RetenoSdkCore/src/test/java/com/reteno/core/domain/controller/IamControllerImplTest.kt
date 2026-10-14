package com.reteno.core.domain.controller

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleConditionsGroup
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleGroup
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamControllerImpl.Companion.TIMEOUT
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Event.Companion.SCREEN_VIEW_EVENT_TYPE_KEY
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.event.eventFlow
import com.reteno.core.internal.action.iam.CheckInAppSegments
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalCoroutinesApi::class)
class IamControllerImplTest : BaseRobolectricTest() {

    class TestContent(
        @SerializedName("widgetContent")
        val widgetContent: Int = 1
    )

    class PlaceholderContent(
        @SerializedName("text")
        val text: String
    )

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
        private const val WIDGET = "{\"widgetContent\":1}"
        private const val BASE_HTML_PREFIX = "BaseHtmlContentPrefix"
        private const val BASE_HTML_SUFFIX = "BaseHtmlContentSuffix"
        private const val BASE_HTML_CONTENT = "$BASE_HTML_PREFIX\${documentModel}$BASE_HTML_SUFFIX"
        private const val FULL_HTML = "$BASE_HTML_PREFIX$WIDGET$BASE_HTML_SUFFIX"

        private const val DELAY_BASE_HTML = 1000L
        private const val DELAY_WIDGET = 1000L
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var iamRepository: IamRepository
    @RelaxedMockK
    private lateinit var userDatabase: RetenoDatabaseManagerUser
    @RelaxedMockK
    private lateinit var displayRules: InAppDisplayRules
    @RelaxedMockK
    private lateinit var state: InAppMessagesState
    @RelaxedMockK
    private lateinit var checkSegments: CheckInAppSegments
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        coEvery { iamRepository.getLocalBaseHtml() } coAnswers {
            delay(DELAY_BASE_HTML)
            BASE_HTML_CONTENT
        }
        coEvery { iamRepository.getWidgetRemote(any()) } coAnswers {
            delay(DELAY_WIDGET)
            WidgetModel(
                layoutType = InAppLayoutType.FULL,
                layoutParams = null,
                model = Gson().toJsonTree(TestContent()),
                personalization = null
            )
        }
        every { displayRules.decide(any(), any(), any()) } returns DisplayDecision.SHOW
        every { state.postponeIfPaused(any()) } returns false
        every { state.setPaused(any()) } returns null
        every { state.waitingForEvent() } returns emptyList()
        coEvery { checkSegments(any(), any()) } returns Result.success(Unit)
    }

    override fun after() {
        super.after()
        Dispatchers.resetMain()
    }

    @Test
    fun given_whenFetchIamFullHtml_thenShouldLoadDataConcurrently() = runTest {
        val sut = createController()
        // When
        sut.fetchIamFullHtml(WIDGET_ID)
        advanceUntilIdle()

        // Then
        assertEquals(DELAY_BASE_HTML, currentTime)
        coroutineContext.cancelChildren()
    }

    @Test
    fun given_whenFetchIamFullHtml_thenFullHtmlFlowChangesToSuccess() = runTest {
        val sut = createController()
        // Given
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)

        // When
        sut.fetchIamFullHtml(WIDGET_ID)

        // Then
        advanceTimeBy(DELAY_BASE_HTML)
        assertEquals(ResultDomain.Loading, sut.fullHtmlStateFlow.value)
        runCurrent()
        assertTrue(sut.fullHtmlStateFlow.value is ResultDomain.Success)
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.fullHtml == FULL_HTML)
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.layoutParams == InAppLayoutParams(Position.TOP))
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.layoutType == InAppLayoutType.FULL)

        sut.reset()
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)
        coroutineContext.cancelChildren()
    }

    @Test
    fun given_whenFetchIamFullHtmlTimeoutReached_thenFullHtmlFlowChangesToError() = runTest {
        val sut = createController()
        coEvery { iamRepository.getLocalBaseHtml() } coAnswers {
            delay(TIMEOUT)
            BASE_HTML_CONTENT
        }

        // Given
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)

        // When
        sut.fetchIamFullHtml(WIDGET_ID)

        // Then
        advanceTimeBy(TIMEOUT)
        assertEquals(ResultDomain.Loading, sut.fullHtmlStateFlow.value)
        runCurrent()
        assert(sut.fullHtmlStateFlow.value is ResultDomain.Error)
        val result = sut.fullHtmlStateFlow.value as ResultDomain.Error
        assert(result.errorBody.contains("TIMEOUT"))

        sut.reset()
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)
        coroutineContext.cancelChildren()
    }

    @Test
    fun givenModelHasNoPlaceholders_whenFetchIamFullHtmlWithContent_thenUserDatabaseIsNotQueried() = runTest {
        // Given
        val sut = createController()
        val content = InAppMessageContent(
            messageInstanceId = 1,
            layoutType = InAppLayoutType.FULL,
            model = Gson().toJsonTree(TestContent()),
            layoutParams = null
        )

        // When
        sut.fetchIamFullHtml(content)
        advanceUntilIdle()

        // Then
        assertTrue(sut.fullHtmlStateFlow.value is ResultDomain.Success)
        coVerify(exactly = 0) { userDatabase.getUsers() }
        coroutineContext.cancelChildren()
    }

    @Test
    fun givenOnlySyncedUserInDb_whenFetchIamFullHtmlWithPlaceholders_thenReplacedWithSyncedUserAttributes() = runTest {
        // Given
        val sut = createController()
        val syncedUser = userDb(
            rowId = "1",
            createdAt = 100L,
            isSynchronizedWithBackend = BooleanDb.TRUE,
            firstName = "John",
            lastName = "Doe",
            email = "john@old.com",
            phone = "+380990009900",
            languageCode = "en",
            timeZone = "UTC",
            address = "Old street",
            marketId = "market1"
        )
        every { userDatabase.getUsers() } returns listOf(syncedUser)

        val content = placeholderContent()

        // When
        sut.fetchIamFullHtml(content)
        advanceUntilIdle()

        // Then
        val html = (sut.fullHtmlStateFlow.value as ResultDomain.Success).body.fullHtml
        assertTrue(html.contains("John|Doe|john@old.com|+380990009900|en|UTC|Old street|market1"))
        coroutineContext.cancelChildren()
    }

    @Test
    fun givenPendingPartialUpdateOverSyncedUser_whenFetchIamFullHtmlWithPlaceholders_thenChangedFieldTakenFromPendingAndRestFromSynced() =
        runTest {
            // Given
            val sut = createController()
            // Full snapshot as it was last synced with backend
            val syncedUser = userDb(
                rowId = "1",
                createdAt = 100L,
                isSynchronizedWithBackend = BooleanDb.TRUE,
                firstName = "John",
                lastName = "Doe",
                email = "john@old.com",
                phone = "+380990009900",
                languageCode = "en",
                timeZone = "UTC",
                address = "Old street",
                marketId = "market1"
            )
            // A partial update queued for the next sync - only email changed, everything else null
            val pendingUser = userDb(
                rowId = "2",
                createdAt = 200L,
                isSynchronizedWithBackend = BooleanDb.FALSE,
                email = "john@new.com"
            )
            every { userDatabase.getUsers() } returns listOf(syncedUser, pendingUser)

            val content = placeholderContent()

            // When
            sut.fetchIamFullHtml(content)
            advanceUntilIdle()

            // Then
            val html = (sut.fullHtmlStateFlow.value as ResultDomain.Success).body.fullHtml
            // email comes from the pending partial update, everything else falls back to the synced snapshot
            assertTrue(html.contains("John|Doe|john@new.com|+380990009900|en|UTC|Old street|market1"))
            coroutineContext.cancelChildren()
        }

    @Test
    fun givenNoUsersInDb_whenFetchIamFullHtmlWithPlaceholders_thenReplacedWithEmptyStrings() = runTest {
        // Given
        val sut = createController()
        every { userDatabase.getUsers() } returns emptyList()

        val content = placeholderContent()

        // When
        sut.fetchIamFullHtml(content)
        advanceUntilIdle()

        // Then
        val html = (sut.fullHtmlStateFlow.value as ResultDomain.Success).body.fullHtml
        assertTrue(html.contains("|||||||"))
        coroutineContext.cancelChildren()
    }

    @Test
    fun given_whenReset_thenClearDataSetLoading() = runTest {
        val sut = createController()
        // Given
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)

        // When
        sut.fetchIamFullHtml(WIDGET_ID)
        advanceTimeBy(DELAY_BASE_HTML)

        // Then
        assertEquals(ResultDomain.Loading, sut.fullHtmlStateFlow.value)
        // When
        runCurrent()
        // Then
        assertTrue(sut.fullHtmlStateFlow.value is ResultDomain.Success)
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.fullHtml == FULL_HTML)
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.layoutParams == InAppLayoutParams(Position.TOP))
        assertTrue((sut.fullHtmlStateFlow.value as ResultDomain.Success).body.layoutType == InAppLayoutType.FULL)

        // When
        sut.reset()
        // Then
        assertEquals(ResultDomain.Idle, sut.fullHtmlStateFlow.value)
        coroutineContext.cancelChildren()
    }

    @Test
    fun `should show most recent in-app the display rules allow`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApps = contentInApps()
        val shown = collectShown(sut)

        whenn()
        sut.processInAppMessages(inApps)
        runCurrent()

        then()
        assertEquals(listOf(inApps.maxBy { it.messageId }), shown)
    }

    @Test
    fun `should skip in-apps the display rules reject`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApps = contentInApps()
        every { displayRules.decide(any(), any(), any()) } answers {
            if (firstArg<InAppMessage>().messageId == 2L) DisplayDecision.SHOW else DisplayDecision.FREQUENCY_LIMIT_REACHED
        }
        val shown = collectShown(sut)

        whenn()
        sut.processInAppMessages(inApps)
        runCurrent()

        then()
        assertEquals(listOf(inApps.first { it.messageId == 2L }), shown)
    }

    @Test
    fun `should postpone allowed in-app through state while paused`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApps = contentInApps()
        every { state.postponeIfPaused(any()) } returns true
        val shown = collectShown(sut)

        whenn()
        sut.processInAppMessages(inApps)
        runCurrent()

        then()
        assertTrue(shown.isEmpty())
        verify(exactly = 1) { state.postponeIfPaused(inApps.maxBy { it.messageId }) }
    }

    @Test
    fun `should show in-app handed back by state when resumed`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val postponed = contentInApps().first()
        every { state.setPaused(false) } returns postponed
        val shown = collectShown(sut)

        whenn()
        sut.pauseInAppMessages(false)
        runCurrent()

        then()
        assertEquals(listOf(postponed), shown)
    }

    @Test
    fun `should pass pause behaviour to state`() = runUnitTest {
        given()
        val sut = createWorkingController()

        whenn()
        sut.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)

        then()
        verify(exactly = 1) { state.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS) }
    }

    @Test
    fun `should replace in-apps waiting for event with fetched event in-apps`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApps = contentInApps(targeting = createEventDisplayRule())

        whenn()
        sut.processInAppMessages(inApps)

        then()
        verify(exactly = 1) { state.replaceWaitingForEvent(match { waiting -> waiting.map { it.inApp } == inApps }) }
    }

    @Test
    fun `should clear in-apps waiting for event when fetch has none`() = runUnitTest {
        given()
        val sut = createWorkingController()

        whenn()
        sut.processInAppMessages(contentInApps())

        then()
        verify(exactly = 1) { state.replaceWaitingForEvent(emptyList()) }
    }

    @Test
    fun `should show waiting in-app whose event rule matches`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApp = contentInApps().last()
        val rule = TargetingRule.Event(SCREEN_VIEW_EVENT_TYPE_KEY, RuleRelation.OR)
        every { state.waitingForEvent() } returns listOf(InAppWithEvent(inApp, rule))
        every { displayRules.matchesEvent(rule, any()) } returns true
        val shown = collectShown(sut)
        sut.processInAppMessages(emptyList())
        runCurrent()

        whenn()
        eventFlow.emit(Event.ScreenView("Home"))
        runCurrent()

        then()
        assertEquals(listOf(inApp), shown)
    }

    @Test
    fun `should check and save segments of event in-apps before showing them`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val inApp = contentInApps().last()
        val rule = TargetingRule.Event(SCREEN_VIEW_EVENT_TYPE_KEY, RuleRelation.OR)
        every { state.waitingForEvent() } returns listOf(InAppWithEvent(inApp, rule))
        every { displayRules.matchesEvent(rule, any()) } returns true
        sut.processInAppMessages(emptyList())
        runCurrent()

        whenn()
        eventFlow.emit(Event.ScreenView("Home"))
        runCurrent()

        then()
        coVerify(exactly = 1) { checkSegments(listOf(inApp), true) }
    }

    @Test
    fun `should not check segments of in-apps shown on app start`() = runUnitTest {
        given()
        val sut = createWorkingController()

        whenn()
        sut.processInAppMessages(contentInApps())

        then()
        coVerify(exactly = 0) { checkSegments(any(), any()) }
    }

    @Test
    fun `should not show waiting in-app whose event rule does not match`() = runUnitTest {
        given()
        val sut = createWorkingController()
        val rule = TargetingRule.Event(SCREEN_VIEW_EVENT_TYPE_KEY, RuleRelation.OR)
        every { state.waitingForEvent() } returns listOf(InAppWithEvent(contentInApps().last(), rule))
        every { displayRules.matchesEvent(rule, any()) } returns false
        val shown = collectShown(sut)
        sut.processInAppMessages(emptyList())
        runCurrent()

        whenn()
        eventFlow.emit(Event.ScreenView("Home"))
        runCurrent()

        then()
        assertTrue(shown.isEmpty())
    }

    //region utils

    private fun placeholderContent() = InAppMessageContent(
        messageInstanceId = 1,
        layoutType = InAppLayoutType.FULL,
        model = Gson().toJsonTree(
            PlaceholderContent(
                "\${firstName}|\${lastName}|\${email}|\${phone}|\${languageCode}|\${timeZone}|\${address}|\${marketId}"
            )
        ),
        layoutParams = null
    )

    private fun userDb(
        rowId: String,
        createdAt: Long,
        isSynchronizedWithBackend: BooleanDb,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        languageCode: String? = null,
        timeZone: String? = null,
        address: String? = null,
        marketId: String? = null
    ) = UserDb(
        rowId = rowId,
        createdAt = createdAt,
        deviceId = "deviceId",
        isSynchronizedWithBackend = isSynchronizedWithBackend,
        userAttributes = UserAttributesDb(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            languageCode = languageCode,
            timeZone = timeZone,
            address = address?.let { AddressDb(address = it) },
            marketId = marketId
        )
    )

    private fun TestScope.createController(): IamControllerImpl {
        return IamControllerImpl(iamRepository, sessionHandler, userDatabase, displayRules, state, checkSegments, this)
    }

    private fun TestScope.createWorkingController(): IamControllerImpl {
        return IamControllerImpl(iamRepository, sessionHandler, userDatabase, displayRules, state, checkSegments, workScope())
    }

    private fun TestScope.collectShown(sut: IamControllerImpl): List<InAppMessage> {
        val shown = mutableListOf<InAppMessage>()
        workScope().launch { sut.inAppMessagesFlow.toList(shown) }
        return shown
    }

    private fun contentInApps(targeting: TargetingDisplayRules? = null) = frequencyInApps(
        content = InAppMessageContent(
            messageInstanceId = 1,
            layoutType = InAppLayoutType.FULL,
            model = JsonObject(),
            layoutParams = null
        ),
        targeting = targeting
    )

    private fun createEventDisplayRule() = TargetingDisplayRules(
        include = TargetingRuleGroup(
            relation = RuleRelation.OR,
            groups = listOf(
                TargetingRuleConditionsGroup(
                    relation = RuleRelation.OR,
                    conditions = listOf(
                        TargetingRule.Event(
                            SCREEN_VIEW_EVENT_TYPE_KEY,
                            paramsRelation = RuleRelation.OR,
                            params = listOf(
                                RuleEventParameter(
                                    "name",
                                    StringOperator.CONTAINS,
                                    values = listOf(
                                        "SomeEvent"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),
        exclude = null
    )

    private fun frequencyInApps(
        content: InAppMessageContent? = null,
        targeting: TargetingDisplayRules? = null
    ) = listOf(
        InAppMessage(
            1,
            1,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerApp)
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        ),
        InAppMessage(
            2,
            2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerSession)
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        ),
        InAppMessage(
            3,
            3,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.NoLimit)
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        ),
        InAppMessage(
            4,
            4,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.MinInterval(5000L))
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        ),
        InAppMessage(
            5,
            5,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.TimesPerTimeUnit(timeUnit = TimeUnit.HOURS, 5))
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        ),
        InAppMessage(
            6,
            6,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.TimesPerTimeUnit(timeUnit = TimeUnit.HOURS, 5))
                },
                targeting = targeting,
                schedule = null,
                async = null
            ),
            content = content,
            displayRulesJson = JsonObject()
        )
    )
    //endregion
}