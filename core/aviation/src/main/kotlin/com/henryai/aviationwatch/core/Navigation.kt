package com.henryai.aviationwatch.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** WGS-84 position in decimal degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

/**
 * Great-circle navigation on a spherical earth. Accuracy (~0.5 %) is more than
 * sufficient for situational awareness at watch-display resolution.
 */
object Navigation {
    /** Mean earth radius in meters. */
    const val EARTH_RADIUS_M = 6_371_008.8

    /** Great-circle distance in meters (haversine). */
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun distanceNm(from: GeoPoint, to: GeoPoint): Double =
        Units.metersToNm(distanceMeters(from, to))

    /** Initial true bearing from [from] to [to], degrees in [0, 360). */
    fun initialBearing(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    /**
     * Cross-track distance in meters of [position] from the great circle
     * [start] -> [end]. Positive = right of course, negative = left of course.
     * This drives the CDI/HSI needle on the Direct-To page.
     */
    fun crossTrackMeters(start: GeoPoint, end: GeoPoint, position: GeoPoint): Double {
        val d13 = distanceMeters(start, position) / EARTH_RADIUS_M
        val theta13 = Math.toRadians(initialBearing(start, position))
        val theta12 = Math.toRadians(initialBearing(start, end))
        return asin(sin(d13) * sin(theta13 - theta12)) * EARTH_RADIUS_M
    }

    /** Estimated time enroute in seconds, or null if ground speed is too low to be meaningful. */
    fun eteSeconds(distanceNm: Double, groundSpeedKt: Double): Long? =
        if (groundSpeedKt < 1.0) null else (distanceNm / groundSpeedKt * 3600.0).toLong()

    /**
     * Signed difference [to] - [from] in degrees, in (-180, 180].
     * Positive means [to] is clockwise (to the right) of [from].
     */
    fun signedAngleDifference(from: Double, to: Double): Double {
        val d = normalizeDegrees(to - from)
        return if (d > 180.0) d - 360.0 else d
    }

    /** Normalizes an angle to [0, 360). */
    fun normalizeDegrees(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0
}
