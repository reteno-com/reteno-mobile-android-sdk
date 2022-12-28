package com.reteno.core.data.remote.api

import javax.net.ssl.HttpsURLConnection
import java.net.URL

internal object ConnectionManager {

    fun openConnection(url: String): HttpsURLConnection {
        val urlPath = URL(url)
        return urlPath.openConnection() as HttpsURLConnection
    }

}