package com.henryai.aviationwatch.enginetime

import java.io.IOException
import java.net.URLEncoder

/**
 * Google Drive v3 calls for the hidden app-data folder, exactly as the Enginetime web app
 * uses it (multipart upload with `parents: ["appDataFolder"]`). Files are visible to the
 * web app because both OAuth clients belong to the same Google Cloud project.
 */
internal object DriveClient {
    private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val BOUNDARY = "---AviationWatchBoundary"

    /** The access token was rejected (expired or revoked). */
    class UnauthorizedException : IOException("Drive rejected the access token")

    /** True if a file with exactly this name already exists (protects against duplicate uploads). */
    fun exists(accessToken: String, name: String): Boolean {
        val query = URLEncoder.encode("name = '${name.replace("'", "\\'")}'", "UTF-8")
        val response = Http.request(
            "$FILES_URL?spaces=appDataFolder&q=$query&fields=files(id)",
            "GET",
            mapOf("Authorization" to "Bearer $accessToken"),
        )
        check(response)
        return response.body.contains("\"id\"")
    }

    fun upload(accessToken: String, name: String, json: String) {
        val metadata = """{"name":${quote(name)},"parents":["appDataFolder"],"mimeType":"application/json"}"""
        val body = "--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
            metadata +
            "\r\n--$BOUNDARY\r\nContent-Type: application/json\r\n\r\n" +
            json +
            "\r\n--$BOUNDARY--"
        val response = Http.request(
            UPLOAD_URL,
            "POST",
            mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "multipart/related; boundary=\"$BOUNDARY\"",
            ),
            body,
        )
        check(response)
    }

    private fun check(response: Http.Response) {
        if (response.code == 401) throw UnauthorizedException()
        if (!response.isSuccess) throw IOException("Drive returned HTTP ${response.code}")
    }

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
