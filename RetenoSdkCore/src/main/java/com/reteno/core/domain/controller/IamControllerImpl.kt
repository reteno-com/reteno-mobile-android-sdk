package com.reteno.core.domain.controller

import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
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

internal class IamControllerImpl(
    private val iamRepository: IamRepository
) : IamController {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var widgetId: String? = null
    private val _fullHtmlStateFlow: MutableStateFlow<ResultDomain<String>> = MutableStateFlow(ResultDomain.Loading)
    override val fullHtmlStateFlow: StateFlow<ResultDomain<String>> = _fullHtmlStateFlow

    override fun fetchIamFullHtml(widgetId: String) {
        /*@formatter:off*/ Logger.i(TAG, "fetchIamFullHtml(): ", "widgetId = [", widgetId, "]")
        /*@formatter:on*/
        this.widgetId = widgetId
        _fullHtmlStateFlow.value = ResultDomain.Loading

        scope.launch {
            try {
                withTimeout(TIMEOUT) {
                    val baseHtml = async { iamRepository.getBaseHtml() }
                    val widget = async { iamRepository.getWidget(widgetId) }

                    val fullHtml = baseHtml.await().replace("\${documentModel}", widget.await())
                    _fullHtmlStateFlow.value = ResultDomain.Success(fullHtml)
                }
            } catch (e: TimeoutCancellationException) {
                _fullHtmlStateFlow.value =
                    ResultDomain.Error("fetchIamFullHtml(): widgetId = [$widgetId] TIMEOUT")
            }
        }
    }

    override fun widgetInitFailed(jsEvent: IamJsEvent) {
        /*@formatter:off*/ Logger.i(TAG, "widgetInitFailed(): ", "widgetId = [", widgetId, "], jsEvent = [", jsEvent, "]")
        /*@formatter:on*/
        widgetId?.let {
            iamRepository.widgetInitFailed(it, jsEvent)
        }
    }

    override fun reset() {
        _fullHtmlStateFlow.value = ResultDomain.Loading
        widgetId = null
        scope.coroutineContext.cancelChildren()
    }

    companion object {
        private val TAG: String = IamControllerImpl::class.java.simpleName

        internal const val TIMEOUT = 30_000L
    }
}