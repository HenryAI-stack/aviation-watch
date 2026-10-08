package com.henryai.aviationwatch.core

/** Helpers for raw METAR/TAF text as served by aviationweather.gov (`format=raw`). */
object WeatherText {
    private val CHANGE_GROUP = Regex("^(FM\\d{6}|BECMG|TEMPO|PROB\\d{2})$")
    private val PROB = Regex("^PROB\\d{2}$")

    /**
     * Splits a raw response into individual reports. A report starts on a line that is
     * not indented; indented lines continue the previous report.
     */
    fun splitReports(text: String): List<String> {
        val reports = mutableListOf<StringBuilder>()
        text.lineSequence().forEach { line ->
            when {
                line.isBlank() -> Unit
                line.first().isWhitespace() && reports.isNotEmpty() -> reports.last().append(' ').append(line.trim())
                else -> reports += StringBuilder(line.trim())
            }
        }
        return reports.map { it.toString() }
    }

    /** Breaks a TAF into lines at each change group (FM, BECMG, TEMPO, PROB) for display. */
    fun tafLines(raw: String): List<String> {
        val lines = mutableListOf<MutableList<String>>(mutableListOf())
        val tokens = raw.trim().removeSuffix("=").split(Regex("\\s+")).filter { it.isNotEmpty() }
        tokens.forEachIndexed { index, token ->
            val previous = tokens.getOrNull(index - 1)
            val startsGroup = CHANGE_GROUP.matches(token) &&
                // "PROB30 TEMPO" stays on one line.
                !(token == "TEMPO" && previous != null && PROB.matches(previous))
            if (startsGroup && lines.last().isNotEmpty()) lines += mutableListOf<String>()
            lines.last() += token
        }
        return lines.filter { it.isNotEmpty() }.map { it.joinToString(" ") }
    }
}
