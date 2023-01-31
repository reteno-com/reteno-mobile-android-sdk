package com.reteno.core.domain.controller

import com.reteno.core.data.repository.InAppMessagesRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal class InAppMessagesControllerImpl(
    private val inAppMessagesRepository: InAppMessagesRepository
) : InAppMessagesController {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<String>> =
        MutableStateFlow(ResultDomain.Loading)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<String>> = _fullHtmlStateFlow

    override fun fetchInAppMessagesFullHtml(widgetId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchInAppMessagesFullHtml(): ", "widgetId = [", widgetId, "]")
        /*@formatter:on*/
        _fullHtmlStateFlow.value = ResultDomain.Loading

        scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = async { inAppMessagesRepository.getBaseHtml() }
                    val widget = async { inAppMessagesRepository.getWidget(widgetId) }

                    val fullHtml = baseHtml.await().replace("\${documentModel}", widget.await())
                    _fullHtmlStateFlow.value = ResultDomain.Success(fullHtml)
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchInAppMessagesFullHtml(): widgetId = [$widgetId] TIMEOUT")
            }
        }
    }

    override fun reset() {
        _fullHtmlStateFlow.value = ResultDomain.Loading
        scope.coroutineContext.cancelChildren()
    }

    companion object {
        private val TAG: String = InAppMessagesControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
    }
}