package com.reteno.core.internal.action.iam

import com.google.gson.JsonElement
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.fromJsonOrNull
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.internal.action.iam.LoadInAppHtmlAction.Companion.KEY_DOCUMENT_MODEL
import com.reteno.core.internal.action.iam.LoadInAppHtmlAction.Companion.KEY_PERSONALISATION
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.stepOrNull
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.internal.platform.AppPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal interface LoadPushInAppHtml {
    suspend operator fun invoke(interactionId: String): Result<IamFetchResult>
}

internal class LoadPushInAppHtmlAction(
    private val api: SuspendApiClient,
    private val files: FileManager,
    private val platform: AppPlatform,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val newId: () -> String,
    private val runner: ActionRunner
) : LoadPushInAppHtml {

    override suspend operator fun invoke(interactionId: String): Result<IamFetchResult> = runner("LoadPushInAppHtml") {
        args("interactionId" to interactionId)
        step("wait for sdk initialization") {
            initGate.awaitInit()
        }
        withContext(dispatchers.io) { load(interactionId) }
    }

    private suspend fun ActionTrace.load(interactionId: String): IamFetchResult = coroutineScope {
        val baseHtml = async {
            step("read base html from file") {
                files.getBaseHtmlContent()
            }
        }
        val widget = loadWidget(interactionId)
        IamFetchResult(
            id = newId(),
            fullHtml = fillIn(baseHtml.await(), widget),
            layoutType = widget.layoutType ?: InAppLayoutType.FULL,
            layoutParams = widget.layoutParams ?: InAppLayoutParams(Position.TOP)
        )
    }

    private suspend fun ActionTrace.loadWidget(interactionId: String): WidgetModel {
        val response = step("load in-app widget from server") {
            api.request(HttpMethod.GET, ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(interactionId))
        }
        if (response is ApiResult.Success) {
            val fromServer = readWidget(response.body)
            if (fromServer != null) return fromServer
        }
        return step("read bundled widget from app resources") {
            val model = (platform.bundledWidget() ?: EMPTY_MODEL).fromJson<JsonElement>()
            WidgetModel(layoutType = InAppLayoutType.FULL, layoutParams = null, model = model, personalization = null)
        }
    }

    private suspend fun ActionTrace.readWidget(body: String): WidgetModel? =
        stepOrNull("read widget from server response") {
            body.fromJsonOrNull<WidgetModel>()
        }

    private fun fillIn(baseHtml: String, widget: WidgetModel): String {
        var html = baseHtml
        val model = widget.model
        if (model != null) html = html.replace(KEY_DOCUMENT_MODEL, model.toString())
        val personalisation = widget.personalization
        if (personalisation != null) html = html.replace(KEY_PERSONALISATION, personalisation.toString())
        return html
    }

    private companion object {
        const val EMPTY_MODEL = "{}"
    }
}
