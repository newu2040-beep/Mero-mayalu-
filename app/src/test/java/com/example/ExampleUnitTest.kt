package com.example

import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.util.BikramSambatConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
    @Test
    fun bikramSambatRoundTrip_isAccurate() {
        val bs2080NewYear = BikramSambatConverter.toLocalDate(2080, 1, 1)
        assertEquals(LocalDate.of(2023, 4, 14), bs2080NewYear)

        val parsed = BikramSambatConverter.parseBsToLocalDate("2057-02-01")
        assertNotNull(parsed)
        val roundTripBs = BikramSambatConverter.fromLocalDate(parsed!!)
        assertNotNull(roundTripBs)
        assertEquals("2057-02-01", roundTripBs!!.formatNumeric())
    }

    @Test
    fun upcomingFestivalCountdown_returnsSortedList() {
        val list = BikramSambatConverter.upcomingFestivalsWithCountdown(LocalDate.of(2026, 4, 15))
        assertTrue(list.isNotEmpty())
        assertTrue(list.zipWithNext().all { (a, b) -> a.daysRemaining <= b.daysRemaining })
    }

    @Test
    fun themeAndFontCatalogs_haveExpectedCounts() {
        assertEquals(10, MeroThemeCatalog.entries.size)
        assertEquals(4, MeroFontCatalog.entries.size)
    }
}
