package com.reteno.core.view.iam.callback

class InAppErrorData(
    source: InAppSource,
    id: String?,
    val errorMessage: String
): InAppData(source, id)