package com.henryai.aviationwatch.core

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Surface wind. [directionTrue] is null for variable (VRB) wind. Speeds in knots. */
data class Wind(
    val directionTrue: Int?,
    val speedKt: Int,
    val gustKt: Int? = null,
    val variableFrom: Int? = null,
    val variableTo: Int? = null,
) {
    val isCalm: Boolean get() = speedKt == 0 && gustKt == null
}

data class CloudLayer(val cover: Cover, val baseFt: Int?, val type: String? = null) {
    enum class Cover { FEW, SCT, BKN, OVC, VV }

    /** BKN, OVC and vertical visibility form a ceiling. */
    val isCeiling: Boolean get() = cover == Cover.BKN || cover == Cover.OVC || cover == Cover.VV
}

/** A decoded METAR/SPECI. Fields the report does not contain are null. */
data class Metar(
    val raw: String,
    val station: String,
    /** Observation day of month / hour / minute, UTC. */
    val day: Int?,
    val hour: Int?,
    val minute: Int?,
    val wind: Wind?,
    /** Prevailing visibility in meters; 10 km or more is reported as 10000. */
    val visibilityM: Double?,
    val cavok: Boolean,
    val weather: List<String>,
    val clouds: List<CloudLayer>,
    val temperatureC: Int?,
    val dewpointC: Int?,
    val qnhHpa: Double?,
) {
    /** Lowest BKN/OVC/VV layer in feet AGL, null if none. */
    val ceilingFt: Int? get() = clouds.filter { it.isCeiling }.mapNotNull { it.baseFt }.minOrNull()

    val flightCategory: FlightCategory
        get() = FlightCategory.from(ceilingFt, visibilityM?.let { it / METERS_PER_SM })

    /** Observation time, resolved against [now] (reports carry only day/hour/minute). */
    fun observedAt(now: Instant): Instant? {
        val d = day ?: return null
        val h = hour ?: return null
        val m = minute ?: return null
        val nowUtc = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
        // Try this month and the previous one; take the latest that is not in the future.
        return (0L..1L).asSequence()
            .mapNotNull { back ->
                val month = nowUtc.minusMonths(back)
                runCatching {
                    month.withDayOfMonth(d).withHour(h).withMinute(m).withSecond(0).withNano(0)
                }.getOrNull()
            }
            .map { it.toInstant() }
            .firstOrNull { !it.isAfter(now.plusSeconds(FUTURE_TOLERANCE_S)) }
    }

    companion object {
        const val METERS_PER_SM = 1609.344
        private const val FUTURE_TOLERANCE_S = 600L
    }
}

/** Decodes the main body of a METAR (trend and remarks are ignored). */
object MetarDecoder {
    private val STATION = Regex("^[A-Z][A-Z0-9]{3}$")
    private val TIME = Regex("^(\\d{2})(\\d{2})(\\d{2})Z$")
    private val WIND = Regex("^(\\d{3}|VRB)(\\d{2,3})(?:G(\\d{2,3}))?(KT|MPS|KMH)$")
    private val WIND_VARIATION = Regex("^(\\d{3})V(\\d{3})$")
    private val VIS_METERS = Regex("^(\\d{4})(?:N|NE|E|SE|S|SW|W|NW|NDV)?$")
    private val VIS_SM = Regex("^([PM])?(\\d+)(?:/(\\d+))?SM$")
    private val WHOLE_NUMBER = Regex("^\\d$")
    private val CLOUD = Regex("^(FEW|SCT|BKN|OVC|VV)(\\d{3}|///)(CB|TCU|///)?$")
    private val WEATHER = Regex(
        "^(\\+|-|VC)?(MI|PR|BC|DR|BL|SH|TS|FZ)?" +
            "((?:DZ|RA|SN|SG|IC|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PY|PO|SQ|FC|SS|DS)*)$",
    )
    private val TEMPERATURE = Regex("^(M?\\d{2})/(M?\\d{2})?$")
    private val QNH = Regex("^Q(\\d{4})$")
    private val ALTIMETER_INHG = Regex("^A(\\d{4})$")
    private val END_OF_BODY = setOf("RMK", "TEMPO", "BECMG", "NOSIG", "FCST")
    private val NO_CLOUDS = setOf("NSC", "NCD", "SKC", "CLR")

    fun decode(raw: String): Metar? {
        val tokens = raw.trim().removeSuffix("=").split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it != "METAR" && it != "SPECI" && it != "COR" && it != "AUTO" }
        val station = tokens.firstOrNull()?.takeIf { STATION.matches(it) } ?: return null

        var day: Int? = null
        var hour: Int? = null
        var minute: Int? = null
        var wind: Wind? = null
        var visibilityM: Double? = null
        var cavok = false
        val weather = mutableListOf<String>()
        val clouds = mutableListOf<CloudLayer>()
        var temperature: Int? = null
        var dewpoint: Int? = null
        var qnh: Double? = null

        var i = 1
        while (i < tokens.size) {
            val t = tokens[i]
            if (t in END_OF_BODY) break
            TIME.matchEntire(t)?.let { m ->
                day = m.groupValues[1].toInt()
                hour = m.groupValues[2].toInt()
                minute = m.groupValues[3].toInt()
            } ?: WIND.matchEntire(t)?.let { m ->
                val factor = when (m.groupValues[4]) {
                    "MPS" -> 1.0 / Units.MPS_PER_KNOT
                    "KMH" -> 1000.0 / Units.METERS_PER_NM
                    else -> 1.0
                }
                fun kt(v: String) = Math.round(v.toInt() * factor).toInt()
                wind = Wind(
                    directionTrue = m.groupValues[1].toIntOrNull(),
                    speedKt = kt(m.groupValues[2]),
                    gustKt = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::kt),
                )
            } ?: WIND_VARIATION.matchEntire(t)?.let { m ->
                wind = wind?.copy(variableFrom = m.groupValues[1].toInt(), variableTo = m.groupValues[2].toInt())
            } ?: if (t == "CAVOK") {
                cavok = true
                visibilityM = 10_000.0
            } else {
                null
            } ?: VIS_METERS.matchEntire(t)?.takeIf { visibilityM == null }?.let { m ->
                val meters = m.groupValues[1].toDouble()
                visibilityM = if (meters >= 9999.0) 10_000.0 else meters
            } ?: VIS_SM.matchEntire(t)?.let { m ->
                var miles = m.groupValues[2].toDouble()
                m.groupValues[3].takeIf { it.isNotEmpty() }?.let { miles /= it.toDouble() }
                // "1 1/2SM": the whole number is the previous token.
                val previous = tokens.getOrNull(i - 1)
                if (m.groupValues[3].isNotEmpty() && previous != null && WHOLE_NUMBER.matches(previous)) {
                    miles += previous.toDouble()
                }
                visibilityM = miles * Metar.METERS_PER_SM
            } ?: CLOUD.matchEntire(t)?.let { m ->
                clouds += CloudLayer(
                    cover = CloudLayer.Cover.valueOf(m.groupValues[1]),
                    baseFt = m.groupValues[2].toIntOrNull()?.times(100),
                    type = m.groupValues[3].takeIf { it.isNotEmpty() && it != "///" },
                )
            } ?: TEMPERATURE.matchEntire(t)?.let { m ->
                temperature = parseTemp(m.groupValues[1])
                dewpoint = m.groupValues[2].takeIf { it.isNotEmpty() }?.let(::parseTemp)
            } ?: QNH.matchEntire(t)?.let { m ->
                qnh = m.groupValues[1].toDouble()
            } ?: ALTIMETER_INHG.matchEntire(t)?.let { m ->
                qnh = Units.inHgToHpa(m.groupValues[1].toDouble() / 100.0)
            } ?: WEATHER.matchEntire(t)?.takeIf { isWeather(it) }?.let {
                weather += t
            }
            if (t in NO_CLOUDS) clouds.clear()
            i++
        }
        return Metar(
            raw = raw.trim(),
            station = station,
            day = day,
            hour = hour,
            minute = minute,
            wind = wind,
            visibilityM = visibilityM,
            cavok = cavok,
            weather = weather,
            clouds = clouds,
            temperatureC = temperature,
            dewpointC = dewpoint,
            qnhHpa = qnh,
        )
    }

    private fun isWeather(m: MatchResult): Boolean =
        m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty()

    private fun parseTemp(s: String): Int =
        if (s.startsWith("M")) -s.drop(1).toInt() else s.toInt()
}
