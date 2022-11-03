package com.reteno.core.data.repository

import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger

class DeeplinkRepositoryImpl(private val apiClient: ApiClient) :
    DeeplinkRepository {

    override fun triggerWrappedLinkClicked(wrappedLink: String) {
        /*@formatter:off*/ Logger.i(TAG, "triggerWrappedLinkClicked(): ", "wrappedLink = [" , wrappedLink , "]")
        /*@formatter:on*/
        if (wrappedLink.isEmpty()) return

        OperationQueue.addParallelOperation {
            try {
                apiClient.get(ApiContract.Custom(wrappedLink), null,
                    object : ResponseCallback {
                        override fun onSuccess(response: String) {
                            /*@formatter:off*/ Logger.i(TAG, "onSuccess(): linkClicked = [", wrappedLink, "] response = [" , response , "]")
                            /*@formatter:on*/
                        }

                        override fun onFailure(
                            statusCode: Int?,
                            response: String?,
                            throwable: Throwable?
                        ) {
                            /*@formatter:off*/ Logger.i(TAG, "onFailure(): linkClicked = [", wrappedLink, "] statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                            /*@formatter:on*/
                        }
                    })
            } catch (t: Exception) {
                t.printStackTrace()
            }
        }
    }

    companion object {
        val TAG: String = DeeplinkRepositoryImpl::class.java.simpleName
    }
}