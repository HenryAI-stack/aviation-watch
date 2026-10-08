package com.henryai.aviationwatch.core

/**
 * Reads the compact airport CSV produced by `tools/build_airports.py`:
 * `ident,name,type,lat,lon,elevation_ft[,iata]` with a header row. Malformed rows are skipped.
 */
object AirportCsv {
    fun parse(lines: Sequence<String>): List<Airport> =
        lines.drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull(::parseRow)
            .toList()

    private fun parseRow(line: String): Airport? {
        val f = splitLine(line)
        if (f.size < 6) return null
        val lat = f[3].toDoubleOrNull() ?: return null
        val lon = f[4].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return Airport(
            ident = f[0],
            name = f[1],
            type = Airport.Type.entries.firstOrNull { it.name == f[2] } ?: Airport.Type.OTHER,
            position = GeoPoint(lat, lon),
            elevationFt = f[5].toIntOrNull(),
            iata = f.getOrNull(6)?.takeIf { it.isNotBlank() },
        )
    }

    /** RFC 4180 field splitting: quoted fields may contain commas and doubled quotes. */
    internal fun splitLine(line: String): List<String> {
        val fields = ArrayList<String>(6)
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
