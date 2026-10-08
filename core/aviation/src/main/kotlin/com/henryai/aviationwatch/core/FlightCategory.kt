package com.henryai.aviationwatch.core

/** FAA flight categories as used by aviationweather.gov and Garmin weather pages. */
enum class FlightCategory {
    VFR, MVFR, IFR, LIFR;

    companion object {
        /**
         * @param ceilingFt lowest BKN/OVC/VV layer in feet AGL, or null if no ceiling.
         * @param visibilitySm prevailing visibility in statute miles, or null if unknown.
         */
        fun from(ceilingFt: Int?, visibilitySm: Double?): FlightCategory {
            val byCeiling = when {
                ceilingFt == null -> VFR
                ceilingFt < 500 -> LIFR
                ceilingFt < 1000 -> IFR
                ceilingFt <= 3000 -> MVFR
                else -> VFR
            }
            val byVisibility = when {
                visibilitySm == null -> VFR
                visibilitySm < 1.0 -> LIFR
                visibilitySm < 3.0 -> IFR
                visibilitySm <= 5.0 -> MVFR
                else -> VFR
            }
            // Higher ordinal = worse conditions; the worse of the two wins.
            return maxOf(byCeiling, byVisibility)
        }
    }
}
