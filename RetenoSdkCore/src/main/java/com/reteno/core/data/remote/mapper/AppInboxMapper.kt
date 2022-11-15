package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.inbox.InboxMessageRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.model.appinbox.AppInboxMessage
import com.reteno.core.domain.model.appinbox.AppInboxMessages

fun InboxMessagesRemote.toDomain(): AppInboxMessages {
    return AppInboxMessages(
        messages = messages.map { it.toDomain() },
        totalPages = totalPages ?: 1
    )
}

fun InboxMessageRemote.toDomain(): AppInboxMessage {
    return AppInboxMessage(
        content = content,
        createdDate = createdDate,
        id = id,
        imageUrl = imageUrl,
        linkUrl = linkUrl,
        isNewMessage = isNewMessage,
        title = title
    )
}