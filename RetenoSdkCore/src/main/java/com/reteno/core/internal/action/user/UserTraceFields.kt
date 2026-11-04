package com.reteno.core.internal.action.user

import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.log.Redact

internal fun User?.traceFields(): String = Redact.fields(
    "userAttributes" to this?.userAttributes,
    "subscriptionKeys" to this?.subscriptionKeys,
    "groupNamesInclude" to this?.groupNamesInclude,
    "groupNamesExclude" to this?.groupNamesExclude
)

internal fun UserAttributesAnonymous.traceFields(): String = Redact.fields(
    "firstName" to firstName,
    "lastName" to lastName,
    "languageCode" to languageCode,
    "timeZone" to timeZone,
    "address" to address,
    "fields" to fields,
    "marketId" to marketId
)
