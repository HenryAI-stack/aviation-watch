package com.henryai.aviationwatch.core

import java.util.Locale
import kotlin.math.abs

/**
 * An active Direct-To: a straight (great-circle) course from [origin], the position
 * where Direct-To was activated, to [target].
 */
data class DirectTo(val origin: GeoPoint, val target: Airport) {
    /** Desired track (true): course of the origin -> target line at the origin. */
    val desiredTrackTrue: Double = Navigation.initialBearing(origin, target.position)

    fun solve(position: GeoPoint, groundSpeedKt: Double?, trackTrue: Double?): NavSolution {
        val distanceNm = Navigation.distanceNm(position, target.position)
        return NavSolution(
            bearingTrue = Navigation.initialBearing(position, target.position),
            distanceNm = distanceNm,
            desiredTrackTrue = desiredTrackTrue,
            crossTrackNm = Units.metersToNm(
                Navigation.crossTrackMeters(origin, target.position, position),
            ),
            groundSpeedKt = groundSpeedKt,
            trackTrue = trackTrue,
            eteSeconds = groundSpeedKt?.let { Navigation.eteSeconds(distanceNm, it) },
        )
    }
}

/** Everything the Direct-To page displays, in true degrees / NM / kt. */
data class NavSolution(
    val bearingTrue: Double,
    val distanceNm: Double,
    val desiredTrackTrue: Double,
    /** Positive = aircraft right of course. */
    val crossTrackNm: Double,
    val groundSpeedKt: Double?,
    val trackTrue: Double?,
    val eteSeconds: Long?,
) {
    /** Turn needed to fly straight to the target: positive = turn right. Null without a valid track. */
    val bearingRelativeToTrack: Double?
        get() = trackTrue?.let { Navigation.signedAngleDifference(it, bearingTrue) }
}

/** Course deviation indicator scaling, Garmin-style: full deflection at [fullScaleNm]. */
object Cdi {
    const val ENROUTE_FULL_SCALE_NM = 2.0
    const val TERMINAL_FULL_SCALE_NM = 1.0

    /**
     * Needle deflection in [-1, 1]. Negative = needle left. The needle shows where
     * the course is, so an aircraft right of course sees the needle to the left.
     */
    fun deflection(crossTrackNm: Double, fullScaleNm: Double = ENROUTE_FULL_SCALE_NM): Double =
        (-crossTrackNm / fullScaleNm).coerceIn(-1.0, 1.0)

    /** Terminal sensitivity within 30 NM of the target, as on GPS navigators. */
    fun fullScaleFor(distanceToTargetNm: Double): Double =
        if (distanceToTargetNm <= 30.0) TERMINAL_FULL_SCALE_NM else ENROUTE_FULL_SCALE_NM
}

/** Display formatting shared by screens and tiles. Locale-independent, as on avionics. */
object AviationFormat {
    /** "045°" style three-digit heading; 0° is shown as 360°, as on aircraft instruments. */
    fun heading(degrees: Double): String {
        val rounded = Math.round(Navigation.normalizeDegrees(degrees)).toInt()
        val value = if (rounded == 0 || rounded == 360) 360 else rounded
        return "%03d°".format(Locale.ROOT, value)
    }

    /** ETE as "mm:ss" below one hour, otherwise "h:mm" with an "h" suffix. */
    fun duration(seconds: Long): String {
        val s = abs(seconds)
        return if (s < 3600) {
            "%d:%02d".format(Locale.ROOT, s / 60, s % 60)
        } else {
            "%d:%02dh".format(Locale.ROOT, s / 3600, (s % 3600) / 60)
        }
    }

    /** METAR-style wind as "290°12G25kt", "VRB03kt" or "Calm" (direction true, as reported). */
    fun wind(wind: Wind): String {
        if (wind.isCalm) return "Calm"
        val direction = wind.directionTrue?.let { "%03d°".format(Locale.ROOT, it) } ?: "VRB"
        val gust = wind.gustKt?.let { "G%02d".format(Locale.ROOT, it) } ?: ""
        return "$direction${"%02d".format(Locale.ROOT, wind.speedKt)}${gust}kt"
    }

    /** Distance with one decimal below 10 NM, whole NM above. */
    fun distanceNm(nm: Double): String =
        if (nm < 10.0) "%.1f".format(Locale.ROOT, nm) else "%.0f".format(Locale.ROOT, nm)
}
