package com.henryai.aviationwatch.core

/** Unit conversions used throughout the app. All internal math is SI unless noted. */
object Units {
    const val METERS_PER_FOOT = 0.3048
    const val METERS_PER_NM = 1852.0
    const val HPA_PER_INHG = 33.8638866667
    const val MPS_PER_KNOT = METERS_PER_NM / 3600.0

    fun feetToMeters(ft: Double): Double = ft * METERS_PER_FOOT
    fun metersToFeet(m: Double): Double = m / METERS_PER_FOOT

    fun metersToNm(m: Double): Double = m / METERS_PER_NM
    fun nmToMeters(nm: Double): Double = nm * METERS_PER_NM

    fun mpsToKnots(mps: Double): Double = mps / MPS_PER_KNOT
    fun knotsToMps(kt: Double): Double = kt * MPS_PER_KNOT

    fun hpaToInHg(hpa: Double): Double = hpa / HPA_PER_INHG
    fun inHgToHpa(inHg: Double): Double = inHg * HPA_PER_INHG

    fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0
    fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0
}
