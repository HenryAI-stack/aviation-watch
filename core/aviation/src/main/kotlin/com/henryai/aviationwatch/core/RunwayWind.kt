package com.henryai.aviationwatch.core

import kotlin.math.cos
import kotlin.math.sin

/** Headwind (negative = tailwind) and crosswind (positive = from the right) in knots. */
data class WindComponents(val headwindKt: Double, val crosswindKt: Double)

/** One usable runway end, e.g. "29" with magnetic heading 290°. */
data class RunwayEnd(val name: String, val headingMagnetic: Int)

data class RunwayWindResult(val end: RunwayEnd, val components: WindComponents)

object RunwayWind {
    private val END = Regex("^(\\d{1,2})[LCR]?$")

    fun components(windFromTrue: Double, windSpeedKt: Double, runwayHeadingTrue: Double): WindComponents {
        val angle = Math.toRadians(Navigation.signedAngleDifference(runwayHeadingTrue, windFromTrue))
        return WindComponents(headwindKt = windSpeedKt * cos(angle), crosswindKt = windSpeedKt * sin(angle))
    }

    /** Runway ends with a numeric designator: "08L/26R" -> 08L (080°), 26R (260°). */
    fun ends(runway: Runway): List<RunwayEnd> =
        runway.designator.split("/").mapNotNull { name ->
            val number = END.matchEntire(name.trim())?.groupValues?.get(1)?.toInt() ?: return@mapNotNull null
            if (number !in 1..36) null else RunwayEnd(name.trim(), number * 10)
        }

    /**
     * The runway end with the most headwind for [wind]. METAR wind is true, runway
     * numbers are magnetic: [magneticVariation] (east positive) converts between them.
     * Null for calm or variable wind, or without numbered runways.
     */
    fun best(runways: List<Runway>, wind: Wind, magneticVariation: Double): RunwayWindResult? {
        val direction = wind.directionTrue ?: return null
        if (wind.isCalm) return null
        return runways.flatMap(::ends)
            .distinctBy { it.name }
            .map { end ->
                val headingTrue = Navigation.normalizeDegrees(end.headingMagnetic + magneticVariation)
                RunwayWindResult(end, components(direction.toDouble(), wind.speedKt.toDouble(), headingTrue))
            }
            .maxByOrNull { it.components.headwindKt }
    }
}
