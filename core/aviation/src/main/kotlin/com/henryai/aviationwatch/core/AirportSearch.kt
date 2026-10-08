package com.henryai.aviationwatch.core

/** Finds airports by ICAO/local ident, IATA code or name, best matches first. */
object AirportSearch {
    private const val MIN_NAME_QUERY = 3

    fun search(query: String, airports: Collection<Airport>, limit: Int = 20): List<Airport> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return emptyList()
        return airports.asSequence()
            .mapNotNull { airport -> rank(q, airport)?.let { it to airport } }
            .sortedWith(compareBy({ it.first }, { it.second.type.ordinal }, { it.second.ident }))
            .take(limit)
            .map { it.second }
            .toList()
    }

    /** Lower is better; null = no match. */
    private fun rank(q: String, airport: Airport): Int? = when {
        airport.ident == q -> 0
        airport.iata == q -> 1
        airport.ident.startsWith(q) -> 2
        q.length >= MIN_NAME_QUERY && airport.name.uppercase().contains(q) -> 3
        else -> null
    }
}
