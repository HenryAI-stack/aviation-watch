package com.henryai.aviationwatch.core

import kotlin.math.pow

/**
 * International Standard Atmosphere (troposphere, below 11 km) helpers for the
 * barometric altimeter. Galaxy Watch4 has a pressure sensor reporting static
 * pressure in hPa.
 */
object Atmosphere {
    const val STANDARD_PRESSURE_HPA = 1013.25
    const val STANDARD_TEMP_K = 288.15
    const val LAPSE_RATE_K_PER_M = 0.0065
    private const val EXPONENT = 0.190263 // R * L / (g * M)

    /**
     * Altitude in meters for a measured static pressure, referenced to [qnhHpa].
     * With qnh = 1013.25 this yields pressure altitude.
     */
    fun altitudeMeters(staticPressureHpa: Double, qnhHpa: Double = STANDARD_PRESSURE_HPA): Double {
        require(staticPressureHpa > 0 && qnhHpa > 0) { "pressures must be positive" }
        return STANDARD_TEMP_K / LAPSE_RATE_K_PER_M *
            (1.0 - (staticPressureHpa / qnhHpa).pow(EXPONENT))
    }

    fun altitudeFeet(staticPressureHpa: Double, qnhHpa: Double = STANDARD_PRESSURE_HPA): Double =
        Units.metersToFeet(altitudeMeters(staticPressureHpa, qnhHpa))

    /** Static pressure in hPa expected at [altitudeMeters] for the given [qnhHpa]. */
    fun pressureAt(altitudeMeters: Double, qnhHpa: Double = STANDARD_PRESSURE_HPA): Double =
        qnhHpa * (1.0 - LAPSE_RATE_K_PER_M * altitudeMeters / STANDARD_TEMP_K).pow(1.0 / EXPONENT)

    /** Pressure altitude in feet of a field at [elevationFt] with the given [qnhHpa]. */
    fun pressureAltitudeFt(elevationFt: Double, qnhHpa: Double): Double =
        altitudeFeet(pressureAt(Units.feetToMeters(elevationFt), qnhHpa))

    /** ISA temperature in °C at a given pressure altitude in feet. */
    fun isaTemperatureC(pressureAltitudeFt: Double): Double =
        15.0 - 1.98 * pressureAltitudeFt / 1000.0

    /** Density altitude in feet (common pilot rule of thumb: 120 ft per °C ISA deviation). */
    fun densityAltitudeFt(pressureAltitudeFt: Double, outsideAirTempC: Double): Double =
        pressureAltitudeFt + 120.0 * (outsideAirTempC - isaTemperatureC(pressureAltitudeFt))
}
