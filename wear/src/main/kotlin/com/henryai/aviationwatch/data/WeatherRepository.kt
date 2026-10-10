package com.henryai.aviationwatch.data

import com.henryai.aviationwatch.core.Metar
import com.henryai.aviationwatch.core.MetarDecoder
import com.henryai.aviationwatch.core.WeatherText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * METAR and TAF from the free aviationweather.gov Data API (NOAA/NWS, no API key).
 * Uses the plain-text `format=raw` responses and decodes them in core, so the app does
 * not depend on the JSON schema. Results are cached for a few minutes to keep requests low.
 */
object WeatherRepository {
    private const val BASE_URL = "https://aviationweather.gov/api/data/"
    private const val USER_AGENT = "AviationWatch/0.1 (+https://github.com/HenryAI-stack/aviation-watch)"
    private const val TTL_MS = 5 * 60_000L
    private const val TIMEOUT_MS = 15_000
    private val ICAO = Regex("^[A-Z0-9]{4}$")

    private class Cached<T>(val fetchedAt: Long, val value: T)

    private val metarCache = ConcurrentHashMap<String, Cached<Metar?>>()
    private val tafCache = ConcurrentHashMap<String, Cached<List<String>>>()

    /** Only 4-character ICAO-style idents can have reports. */
    fun hasReports(ident: String): Boolean = ICAO.matches(ident)

    /** Latest METAR per station; stations without a current report are absent. */
    suspend fun metars(idents: Collection<String>, forceRefresh: Boolean = false): Map<String, Metar> =
        withContext(Dispatchers.IO) {
            val wanted = idents.filter(::hasReports).distinct()
            val now = System.currentTimeMillis()
            val stale = wanted.filter { forceRefresh || isStale(metarCache[it], now) }
            if (stale.isNotEmpty()) {
                val text = get("metar?ids=${stale.joinToString(",")}&format=raw")
                val decoded = WeatherText.splitReports(text)
                    .mapNotNull(MetarDecoder::decode)
                    .associateBy { it.station }
                stale.forEach { metarCache[it] = Cached(now, decoded[it]) }
            }
            wanted.mapNotNull { id -> metarCache[id]?.value?.let { id to it } }.toMap()
        }

    /** Latest TAF for [ident] split into display lines, empty if none is issued. */
    suspend fun taf(ident: String, forceRefresh: Boolean = false): List<String> =
        withContext(Dispatchers.IO) {
            if (!hasReports(ident)) return@withContext emptyList()
            val now = System.currentTimeMillis()
            val cached = tafCache[ident]
            if (!forceRefresh && cached != null && !isStale(cached, now)) return@withContext cached.value
            val report = WeatherText.splitReports(get("taf?ids=$ident&format=raw")).firstOrNull()
            val lines = report?.let(WeatherText::tafLines).orEmpty()
            tafCache[ident] = Cached(now, lines)
            lines
        }

    private fun isStale(cached: Cached<*>?, now: Long) = cached == null || now - cached.fetchedAt > TTL_MS

    private fun get(path: String): String {
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NO_CONTENT) return ""
            if (code !in 200..299) throw IOException("aviationweather.gov returned HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
