package com.reteno.core.data.remote.api

import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal fun interface HttpConnections {

    fun open(url: String): HttpURLConnection
}

internal object HttpsConnections : HttpConnections {

    override fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        return connection
    }
}
