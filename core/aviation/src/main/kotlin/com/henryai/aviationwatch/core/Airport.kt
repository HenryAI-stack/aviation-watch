package com.henryai.aviationwatch.core

/** Minimal airport record, sourced from the public-domain OurAirports dataset. */
data class Airport(
    val ident: String,
    val name: String,
    val type: Type,
    val position: GeoPoint,
    val elevationFt: Int?,
) {
    enum class Type { LARGE, MEDIUM, SMALL, HELIPORT, SEAPLANE_BASE, OTHER }
}

data class NearestResult(
    val airport: Airport,
    val distanceNm: Double,
    val bearingTrue: Double,
)

/** "NRST" function: the closest airports to a position. */
object NearestAirports {
    fun find(
        position: GeoPoint,
        airports: Collection<Airport>,
        limit: Int = 9,
        types: Set<Airport.Type> = setOf(Airport.Type.LARGE, Airport.Type.MEDIUM, Airport.Type.SMALL),
    ): List<NearestResult> =
        airports.asSequence()
            .filter { it.type in types }
            .map { NearestResult(it, Navigation.distanceNm(position, it.position), 0.0) }
            .sortedBy { it.distanceNm }
            .take(limit)
            .map { it.copy(bearingTrue = Navigation.initialBearing(position, it.airport.position)) }
            .toList()
}
