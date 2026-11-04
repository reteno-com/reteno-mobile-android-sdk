package com.reteno.core.internal.iam

import com.reteno.core.data.local.model.interaction.InAppInteractionDb

internal val InAppInteractionDb.serverAssignedIid: Boolean
    get() = deliveryContext != null && deliveryExpiresAt != null
