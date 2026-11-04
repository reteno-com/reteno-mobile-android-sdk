package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.network.SuspendApiClient

internal interface ReportWidgetFailure {
    suspend operator fun invoke(tenantId: String, event: IamJsEvent): Result<Unit>
}

internal class ReportWidgetFailureAction(
    private val api: SuspendApiClient,
    private val initGate: InitGate,
    private val runner: ActionRunner
) : ReportWidgetFailure {

    override suspend operator fun invoke(
        tenantId: String,
        event: IamJsEvent
    ): Result<Unit> = runner("ReportWidgetFailure") {
        args("type" to event.type)
        step("wait for sdk initialization") {
            initGate.awaitInit()
        }
        val data = Data(payload = Payload(reason = event.payload?.reason), type = event.type.name)
        step("send widget failure to server") {
            api.request(
                HttpMethod.POST,
                ApiContract.InAppMessages.WidgetInitFailed,
                IamJsWidgetInitiFailed(tenantId = tenantId, data = data.toJson()).toJson()
            )
        }
    }
}
