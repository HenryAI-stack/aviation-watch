package com.henryai.aviationwatch.core

/** One runway, e.g. 11/29 11811 ft asphalt, lighted. */
data class Runway(
    val designator: String,
    val lengthFt: Int?,
    val widthFt: Int?,
    val surface: String,
    val lighted: Boolean,
)

/** One published frequency, e.g. TWR 119.400 MHz. */
data class Frequency(val type: String, val description: String, val mhz: Double) {
    /** Three decimals, as radios display them: 119.400. */
    val display: String get() = "%.3f".format(java.util.Locale.ROOT, mhz)
}

/**
 * Readers for `runways.csv` and `frequencies.csv` produced by tools/build_airports.py.
 * Both files are sorted by ident, so a lookup stops as soon as it passes the airport.
 */
object AirportDetailsCsv {
    fun runwaysFor(ident: String, lines: Sequence<String>): List<Runway> =
        rowsFor(ident, lines).mapNotNull { f ->
            if (f.size < 6) return@mapNotNull null
            Runway(
                designator = f[1],
                lengthFt = f[2].toIntOrNull(),
                widthFt = f[3].toIntOrNull(),
                surface = f[4],
                lighted = f[5] == "1",
            )
        }

    fun frequenciesFor(ident: String, lines: Sequence<String>): List<Frequency> =
        rowsFor(ident, lines).mapNotNull { f ->
            if (f.size < 4) return@mapNotNull null
            val mhz = f[3].toDoubleOrNull() ?: return@mapNotNull null
            Frequency(type = f[1], description = f[2], mhz = mhz)
        }

    private fun rowsFor(ident: String, lines: Sequence<String>): List<List<String>> =
        lines.drop(1)
            .filter { it.isNotBlank() }
            .map { AirportCsv.splitLine(it) }
            .dropWhile { it[0] < ident }
            .takeWhile { it[0] == ident }
            .toList()
}
