package com.reteno.core.internal.action.iam

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal interface LoadInAppHtml {
    suspend operator fun invoke(message: InAppMessage): Result<IamFetchResult>
}

internal class LoadInAppHtmlAction(
    private val files: FileManager,
    private val usersDb: RetenoDatabaseManagerUser,
    private val dispatchers: RetenoDispatchers,
    private val newId: () -> String,
    private val runner: ActionRunner
) : LoadInAppHtml {

    override suspend operator fun invoke(message: InAppMessage): Result<IamFetchResult> = runner("LoadInAppHtml") {
        args("messageId" to message.messageId)
        withContext(dispatchers.io) { load(message) }
    }

    private suspend fun ActionTrace.load(message: InAppMessage): IamFetchResult {
        val content = requireNotNull(message.content) { "in-app message has no content" }
        val baseHtml = step("read base html from file") {
            files.getBaseHtmlContent()
        }
        val model = personalise(modelOf(content))
        return IamFetchResult(
            id = newId(),
            fullHtml = baseHtml.replace(KEY_DOCUMENT_MODEL, model).replace(KEY_PERSONALISATION, EMPTY_PERSONALISATION),
            layoutType = layoutTypeOf(content),
            layoutParams = content.layoutParams ?: InAppLayoutParams(Position.TOP)
        )
    }

    private suspend fun ActionTrace.personalise(model: String): String {
        if (!PLACEHOLDER.containsMatchIn(model)) return model
        val latest = step("load latest user attributes from local database") {
            LatestAttributes.from(usersDb.getUsers())
        }
        return USER_FIELDS.entries.fold(model) { text, (key, field) ->
            text.replace("\${$key}", jsonStringContent(latest.pick(field)))
        }
    }

    private fun modelOf(content: InAppMessageContent): String {
        val model: JsonElement = content.model ?: return EMPTY_MODEL
        return model.toString()
    }

    private fun layoutTypeOf(content: InAppMessageContent): InAppLayoutType {
        val layoutType: InAppLayoutType? = content.layoutType
        return layoutType ?: InAppLayoutType.FULL
    }

    private fun jsonStringContent(value: String): String = GSON.toJson(value).removeSurrounding("\"")

    private class LatestAttributes(
        private val pending: UserAttributesDb?,
        private val synced: UserAttributesDb?
    ) {

        fun pick(field: (UserAttributesDb) -> String?): String = pending?.let(field) ?: synced?.let(field) ?: ""

        companion object {
            fun from(users: List<UserDb>) = LatestAttributes(
                pending = users.filter { it.isSynchronizedWithBackend == BooleanDb.FALSE }
                    .maxByOrNull { it.createdAt }
                    ?.userAttributes,
                synced = users.firstOrNull { it.isSynchronizedWithBackend == BooleanDb.TRUE }?.userAttributes
            )
        }
    }

    internal companion object {
        const val KEY_DOCUMENT_MODEL = "\${documentModel}"
        const val KEY_PERSONALISATION = "\${personalisation}"
        private const val EMPTY_PERSONALISATION = "{}"
        private const val EMPTY_MODEL = "{}"
        private val PLACEHOLDER = Regex("\\$\\{.+\\}")
        private val GSON = Gson()
        private val USER_FIELDS: Map<String, (UserAttributesDb) -> String?> = mapOf(
            "firstName" to { it.firstName },
            "lastName" to { it.lastName },
            "email" to { it.email },
            "phone" to { it.phone },
            "languageCode" to { it.languageCode },
            "timeZone" to { it.timeZone },
            "address" to { it.address?.address },
            "marketId" to { it.marketId }
        )
    }
}
