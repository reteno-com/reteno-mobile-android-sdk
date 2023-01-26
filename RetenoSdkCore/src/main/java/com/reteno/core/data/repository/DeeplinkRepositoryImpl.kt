package com.reteno.core.data.repository

import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import java.time.ZonedDateTime

internal class DeeplinkRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManagerWrappedLink
) : DeeplinkRepository {

    override fun saveWrappedLink(wrappedLink: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveWrappedLink(): ", "wrappedLink = [", wrappedLink, "]")
        /*@formatter:on*/
        if (wrappedLink.isBlank()) return

        OperationQueue.addParallelOperation {
            databaseManager.insertWrappedLink(wrappedLink)
        }
    }

    override fun pushWrappedLink() {
        /*@formatter:off*/ Logger.i(TAG, "pushWrappedLink(): ", "")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val wrappedLink = databaseManager.getWrappedLinks(1).firstOrNull() ?: kotlin.run {
                PushOperationQueue.nextOperation()
                return@addOperation
            }
            /*@formatter:off*/ Logger.i(TAG, "pushWrappedLink(): ", "url = [" , wrappedLink , "]")
            /*@formatter:on*/

            try {
                apiClient.head(ApiContract.Custom(wrappedLink), null,
                    object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "onSuccess(): linkClicked = [", wrappedLink, "] response = [" , response , "]")
                            /*@formatter:on*/
                            databaseManager.deleteWrappedLinks(1)
                            pushWrappedLink()
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "onFailure(): linkClicked = [", wrappedLink, "] statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                            /*@formatter:on*/
                            if (isNonRepeatableError(statusCode)) {
                                databaseManager.deleteWrappedLinks(1)
                                pushWrappedLink()
                            } else {
                                PushOperationQueue.removeAllOperations()
                            }
                        }
                    })
            } catch (t: Exception) {
                t.printStackTrace()
            }
        }
    }

    override fun clearOldWrappedLinks(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldWrappedLinks(): ", "outdatedTime = [", outdatedTime, "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val count = databaseManager.deleteWrappedLinksByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldWrappedLinks(): ", "removedWrappedLinksCount = [" , count , "]")
            /*@formatter:on*/
            if (count > 0) {
                val msg = "$REMOVE_WRAPPED_LINKS - $count"
                val event = SentryEvent().apply {
                    message = Message().apply {
                        message = msg
                    }
                    level = SentryLevel.INFO
                    fingerprints = listOf(RetenoImpl.application.packageName, REMOVE_WRAPPED_LINKS)
                }
                Logger.captureEvent(event)
            }
        }
    }

    companion object {
        private val TAG: String = DeeplinkRepositoryImpl::class.java.simpleName

        private const val REMOVE_WRAPPED_LINKS = "Removed wrapped links"
    }
}