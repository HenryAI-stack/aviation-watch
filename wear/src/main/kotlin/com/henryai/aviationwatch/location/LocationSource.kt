package com.henryai.aviationwatch.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.henryai.aviationwatch.core.GeoPoint
import com.henryai.aviationwatch.core.Navigation
import com.henryai.aviationwatch.core.Units
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** One GPS position report, converted to aviation units. */
data class Fix(
    val position: GeoPoint,
    val altitudeM: Double?,
    val groundSpeedKt: Double?,
    /** True track over ground; null when too slow for a meaningful direction. */
    val trackTrue: Double?,
    val accuracyM: Float?,
    val timeMillis: Long,
) {
    /** Magnetic variation (declination) in degrees, east positive, from the WMM model. */
    val magneticVariation: Double by lazy {
        GeomagneticField(
            position.latitude.toFloat(),
            position.longitude.toFloat(),
            (altitudeM ?: 0.0).toFloat(),
            timeMillis,
        ).declination.toDouble()
    }

    /** Converts a true direction to magnetic at this position. */
    fun toMagnetic(trueDegrees: Double): Double =
        Navigation.normalizeDegrees(trueDegrees - magneticVariation)
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Position updates from the watch's own GPS via the platform LocationManager.
 * No Google Play services dependency; the Galaxy Watch4 has a built-in GPS receiver.
 * Updates run only while the flow is collected (i.e. while a screen is visible).
 */
object LocationSource {
    private const val INTERVAL_MS = 1000L
    private const val MIN_SPEED_FOR_TRACK_KT = 5.0

    // Permission is checked at the start of the flow; lint cannot follow that check.
    @SuppressLint("MissingPermission")
    fun fixes(context: Context): Flow<Fix> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = manager?.let(::bestProvider)
        if (manager == null || provider == null || !hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toFix())
            }

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        manager.getLastKnownLocation(provider)?.let { trySend(it.toFix()) }
        manager.requestLocationUpdates(provider, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }

    private fun bestProvider(manager: LocationManager): String? {
        val providers = manager.allProviders
        return when {
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                LocationManager.FUSED_PROVIDER in providers -> LocationManager.FUSED_PROVIDER
            else -> manager.getProviders(true).firstOrNull()
        }
    }

    private fun Location.toFix(): Fix {
        val speedKt = if (hasSpeed()) Units.mpsToKnots(speed.toDouble()) else null
        val moving = speedKt != null && speedKt >= MIN_SPEED_FOR_TRACK_KT
        return Fix(
            position = GeoPoint(latitude, longitude),
            altitudeM = if (hasAltitude()) altitude else null,
            groundSpeedKt = speedKt,
            trackTrue = if (hasBearing() && moving) bearing.toDouble() else null,
            accuracyM = if (hasAccuracy()) accuracy else null,
            timeMillis = time,
        )
    }
}
