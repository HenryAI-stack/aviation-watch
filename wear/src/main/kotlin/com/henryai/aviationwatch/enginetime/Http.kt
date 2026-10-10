package com.henryai.aviationwatch.enginetime

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal HTTPS helper (no extra dependencies): returns status code and body, also for 4xx. */
internal object Http {
    private const val TIMEOUT_MS = 20_000

    data class Response(val code: Int, val body: String) {
        val isSuccess: Boolean get() = code in 200..299
    }

    fun postForm(url: String, params: Map<String, String>): Response {
        val body = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return request(url, "POST", mapOf("Content-Type" to "application/x-www-form-urlencoded"), body)
    }

    fun request(url: String, method: String, headers: Map<String, String>, body: String? = null): Response {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = method
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Response(code, text)
        } finally {
            connection.disconnect()
        }
    }
}
