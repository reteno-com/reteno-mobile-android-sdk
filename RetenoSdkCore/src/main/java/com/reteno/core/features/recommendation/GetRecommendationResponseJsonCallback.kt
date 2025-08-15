package com.reteno.core.features.recommendation

interface GetRecommendationResponseJsonCallback {

    /**
     * @param response Response casted to a provided type
     */
    fun onSuccess(response: String)
    
    /**
     * @param statusCode Rest status code (500, 400, etc)
     * @param response response message from the server
     * @param throwable exception thrown returned in this parameter if any
     */
    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}