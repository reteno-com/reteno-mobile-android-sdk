package com.reteno.data.remote.api

import java.net.HttpURLConnection
import java.net.URL

object ConnectionManager {

    fun openConnection(url: String): HttpURLConnection {
        val urlPath = URL(url)
        return urlPath.openConnection() as HttpURLConnection
    }

}