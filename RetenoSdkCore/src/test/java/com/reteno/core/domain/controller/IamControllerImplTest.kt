package com.reteno.core.domain.controller

import com.google.gson.JsonObject
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRuleValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventValidator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleConditionsGroup
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRuleGroup
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessagesList
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamControllerImpl.Companion.TIMEOUT
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Event.Companion.SCREEN_VIEW_EVENT_TYPE_KEY
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.lifecycle.RetenoSessionHandlerImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalCoroutinesApi::class)
class IamControllerImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
        private const val WIDGET = "widgetContent"
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
    private lateinit var sessionHandler: RetenoSessionHandlerImpl

    @RelaxedMockK
    private lateinit var eventController: EventController

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    private val eventFlow = MutableSharedFlow<Event>()

    private lateinit var SUT: IamControllerImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        Dispatchers.setMain(dispatcher)
        coEvery { eventController.eventFlow } returns eventFlow
        SUT = IamControllerImpl(iamRepository, eventController, sessionHandler)

        coEvery { iamRepository.getBaseHtml() } coAnswers {
            delay(DELAY_BASE_HTML)
            BASE_HTML_CONTENT
        }
        coEvery { iamRepository.getWidgetRemote(any()) } coAnswers {
            delay(DELAY_WIDGET)
            WidgetModel(WIDGET)
        }
        mockkConstructor(FrequencyRuleValidator::class)
        mockkConstructor(ScheduleRuleValidator::class)
        mockkConstructor(RuleEventValidator::class)
    }

    override fun after() {
        super.after()
        Dispatchers.resetMain()
    }

    @Test
    fun given_whenFetchIamFullHtml_thenShouldLoadDataConcurrently() {
        // When
        SUT.fetchIamFullHtml(WIDGET_ID)
        scheduler.advanceUntilIdle()

        // Then
        assertEquals(DELAY_BASE_HTML, scheduler.currentTime)
    }

    @Test
    fun given_whenFetchIamFullHtml_thenFullHtmlFlowChangesToSuccess() {
        // Given
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)

        // Then
        scheduler.advanceTimeBy(DELAY_BASE_HTML)
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        scheduler.runCurrent()
        assertEquals(ResultDomain.Success(FULL_HTML), SUT.fullHtmlStateFlow.value)

        SUT.reset()
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)
    }

    @Test
    fun given_whenFetchIamFullHtmlTimeoutReached_thenFullHtmlFlowChangesToError() {
        coEvery { iamRepository.getBaseHtml() } coAnswers {
            delay(TIMEOUT)
            BASE_HTML_CONTENT
        }

        // Given
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)

        // Then
        scheduler.advanceTimeBy(TIMEOUT)
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        scheduler.runCurrent()
        assert(SUT.fullHtmlStateFlow.value is ResultDomain.Error)
        val result = SUT.fullHtmlStateFlow.value as ResultDomain.Error
        assert(result.errorBody.contains("TIMEOUT"))

        SUT.reset()
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)
    }

    @Test
    fun given_whenReset_thenClearDataSetLoading() {
        // Given
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)
        scheduler.advanceTimeBy(DELAY_BASE_HTML)

        // Then
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        // When
        scheduler.runCurrent()
        // Then
        assertEquals(ResultDomain.Success(FULL_HTML), SUT.fullHtmlStateFlow.value)
        // When
        SUT.reset()
        // Then
        assertEquals(ResultDomain.Idle, SUT.fullHtmlStateFlow.value)
    }

    @Test
    fun givenInAppsWithNoContent_whenGetInAppMessages_thenMessagesWithoutContentShouldBeFetchedFromBackend() {
        // Given
        val flow = MutableSharedFlow<Event>()
        val inApps = frequencyInApps(content = null)
        val inAppList = InAppMessagesList(messages = inApps)

        coEvery { eventController.eventFlow } returns flow
        coEvery { iamRepository.getInAppMessages() } returns inAppList
        coEvery { iamRepository.getInAppMessagesContent(any()) } returns buildList {
            repeat(inApps.size) {
                add(
                    InAppMessageContent(
                        messageInstanceId = 1,
                        layoutType = "",
                        model = JsonObject()
                    )
                )
            }
        }

        // When
        SUT.pauseInAppMessages(false)
        SUT.getInAppMessages()
        scheduler.advanceUntilIdle()

        // Then
        coVerify { iamRepository.getInAppMessagesContent(inApps.map { it.messageInstanceId }) }
        coVerify { iamRepository.saveInAppMessages(eq(inAppList)) }
    }

    @Test
    fun givenInAppsWithContent_whenGetInAppMessagesThatMathesRules_thenShowMostRecentMessage() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                )
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { iamRepository.getInAppMessages() } returns inAppList
            every {
                anyConstructed<FrequencyRuleValidator>().checkInAppMatchesFrequencyRules(
                    any(),
                    any(),
                    any()
                )
            } returns true
            every { anyConstructed<ScheduleRuleValidator>().checkInAppMatchesScheduleRules(any()) } returns true

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }
            // When
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                results.first(),
                inApps.maxBy { it.messageId }
            )
            job.cancel()
        }

    @Test
    fun givenInAppsWithContent_whenInAppsDisabled_thenDoNotShowInApps() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                )
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { iamRepository.getInAppMessages() } returns inAppList
            every {
                anyConstructed<FrequencyRuleValidator>().checkInAppMatchesFrequencyRules(
                    any(),
                    any(),
                    any()
                )
            } returns true
            every { anyConstructed<ScheduleRuleValidator>().checkInAppMatchesScheduleRules(any()) } returns true

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }
            // When
            SUT.pauseInAppMessages(true)
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                results,
                emptyList<InAppMessage>()
            )
            job.cancel()
        }

    @Test
    fun givenInAppsWithContent_whenInAppsDisabledWithSkipBehavior_thenDoNotShowInAppsWhenInAppsEnabled() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                )
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { iamRepository.getInAppMessages() } returns inAppList

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }
            // When
            every {
                anyConstructed<FrequencyRuleValidator>().checkInAppMatchesFrequencyRules(
                    any(),
                    any(),
                    any()
                )
            } returns true
            every { anyConstructed<ScheduleRuleValidator>().checkInAppMatchesScheduleRules(any()) } returns true
            SUT.setPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
            SUT.pauseInAppMessages(true)
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()
            SUT.pauseInAppMessages(false)
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                results,
                emptyList<InAppMessage>()
            )
            job.cancel()
        }

    @Test
    fun givenInAppsWithContent_whenInAppsDisabledWithPostponeFirstBehavior_thenShowFirstInAppWhenInAppsEnabled() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                )
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { iamRepository.getInAppMessages() } returns inAppList

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }
            // When
            every {
                anyConstructed<FrequencyRuleValidator>().checkInAppMatchesFrequencyRules(
                    any(),
                    any(),
                    any()
                )
            } returns true
            every { anyConstructed<ScheduleRuleValidator>().checkInAppMatchesScheduleRules(any()) } returns true
            SUT.setPauseBehaviour(InAppPauseBehaviour.POSTPONE_IN_APPS)
            SUT.pauseInAppMessages(true)
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()
            SUT.pauseInAppMessages(false)
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                results,
                listOf(inApps.maxByOrNull { it.messageId }!!)
            )
            job.cancel()
        }

    @Test
    fun givenInAppsWithContent_whenInAppsDisabledWithPostponeFirstBehaviorAndOnly4InAppIsAllowed_thenShowThisInAppWhenInAppsEnabled() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                )
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { iamRepository.getInAppMessages() } returns inAppList

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }
            // When
            every {
                anyConstructed<FrequencyRuleValidator>().checkInAppMatchesFrequencyRules(
                    any(),
                    any(),
                    any()
                )
            } answers  {
                firstArg<InAppMessage>().messageId == 4L
            }
            every { anyConstructed<ScheduleRuleValidator>().checkInAppMatchesScheduleRules(any()) } returns true
            SUT.setPauseBehaviour(InAppPauseBehaviour.POSTPONE_IN_APPS)
            SUT.pauseInAppMessages(true)
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()
            SUT.pauseInAppMessages(false)
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                results,
                listOf(inApps.first { it.messageId == 4L })
            )
            job.cancel()
        }

    @Test
    fun givenInAppsWithContent_whenGetInAppMessagesWithEventTargetRule_thenWaitingListIsFilled() =
        runTest {
            // Given
            val flow = MutableSharedFlow<Event>()
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                ),
                targeting = createEventDisplayRule()
            )
            val inAppList = InAppMessagesList(messages = inApps)

            coEvery { eventController.eventFlow } returns flow
            coEvery { iamRepository.getInAppMessages() } returns inAppList

            // When
            SUT.getInAppMessages()
            scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                SUT.inAppsWaitingForEvent?.map { it.inApp },
                inApps
            )
        }

    @Test
    fun givenFilledEventWaitingList_whenEventHappened_thenInAppEmitted() =
        runTest {
            // Given
            val inApps = frequencyInApps(
                content = InAppMessageContent(
                    messageInstanceId = 1,
                    layoutType = "",
                    model = JsonObject()
                ),
                targeting = createEventDisplayRule()
            )
            val inAppList = InAppMessagesList(messages = inApps)
            coEvery { iamRepository.getInAppMessages() } returns inAppList
            every { anyConstructed<RuleEventValidator>().checkEventMatchesRules(any(), any()) } returns true

            val results = mutableListOf<InAppMessage>()
            val job = launch {
                SUT.inAppMessagesFlow.toList(results)
            }

            // When
            SUT.getInAppMessages()

            advanceUntilIdle()

            eventFlow.emit(Event.ScreenView("SomeEvent"))

            advanceUntilIdle()

            // Then
            assertEquals(
                results.first(),
                inApps.last() //InApp with largest id in dataset
            )
            job.cancel()
        }

    //region utils

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