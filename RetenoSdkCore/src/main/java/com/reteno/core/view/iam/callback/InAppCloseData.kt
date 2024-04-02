package com.reteno.core.view.iam.callback

class InAppCloseData(
    source: InAppSource,
    id: String?,
    val closeAction: InAppCloseAction
) : InAppData(source, id)