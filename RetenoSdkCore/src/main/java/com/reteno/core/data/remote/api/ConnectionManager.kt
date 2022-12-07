package com.reteno.core.data.remote.api

import java.net.HttpURLConnection
import java.net.URL

internal object ConnectionManager {

    fun openConnection(url: String): HttpURLConnection {
        val urlPath = URL(url)
        return urlPath.openConnection() as HttpURLConnection
    }

}