package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTextTest {
    @Test
    fun splitsReportsAndJoinsContinuationLines() {
        val text = "LOWW 081720Z 29012KT 9999 FEW030 12/07 Q1018\nLOWG 081720Z 18005KT CAVOK 14/06 Q1019\n"
        assertEquals(2, WeatherText.splitReports(text).size)
        val taf = "TAF LOWW 081700Z 0818/0924 29012KT 9999 FEW030\n      TEMPO 0818/0822 4000 SHRA\n      BECMG 0900/0902 VRB03KT\n"
        val reports = WeatherText.splitReports(taf)
        assertEquals(1, reports.size)
        assertEquals("TAF LOWW 081700Z 0818/0924 29012KT 9999 FEW030 TEMPO 0818/0822 4000 SHRA BECMG 0900/0902 VRB03KT", reports[0])
    }

    @Test
    fun tafLinesBreakAtChangeGroups() {
        val raw = "TAF LOWW 081700Z 0818/0924 29012KT 9999 FEW030 PROB30 TEMPO 0818/0822 4000 TSRA " +
            "BECMG 0900/0902 VRB03KT FM091200 31015KT CAVOK="
        assertEquals(
            listOf(
                "TAF LOWW 081700Z 0818/0924 29012KT 9999 FEW030",
                "PROB30 TEMPO 0818/0822 4000 TSRA",
                "BECMG 0900/0902 VRB03KT",
                "FM091200 31015KT CAVOK",
            ),
            WeatherText.tafLines(raw),
        )
    }
}
