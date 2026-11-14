package com.example

import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.domain.SavedNote
import com.example.core.util.BikramSambatConverter
import com.example.core.util.DataExportHelper
import com.example.core.util.ExportUserData
import com.example.feature.culture.NepaliCultureQuizBank
import com.example.feature.culture.QuizCategory
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
        assertEquals(17, MeroThemeCatalog.entries.size)
        assertEquals(11, MeroFontCatalog.entries.size)
        assertTrue(MeroFontCatalog.entries.any { it == MeroFontCatalog.SYSTEM })
    }

    @Test
    fun dataExportHelper_formatsJsonCsvAndTxtAccurately() {
        val sample = ExportUserData(
            uid = "u_123",
            displayName = "Aarav Shrestha",
            gender = "Male",
            age = 24,
            dateOfBirthPrivate = "2002-03-11",
            province = "Bagmati",
            district = "Kathmandu",
            municipality = "Patan",
            culturalBadge = "Newar Heritage",
            bio = "Chiya, music, and mountain trails",
            lookingFor = "Meaningful bond",
            interests = listOf("Chiya", "Travel"),
            prompts = listOf("Favorite food::Samay Baji"),
            outgoingRequestsCount = 1,
            incomingRequestsCount = 2,
            activeConversationsCount = 1,
            savedNotes = listOf(
                SavedNote(
                    id = "n1",
                    title = "Patan Walk",
                    noteText = "Morning masala chiya near Durbar Square",
                    sourceAuthorName = "Srijana"
                )
            )
        )
        val json = DataExportHelper.toJson(sample)
        assertTrue(json.contains("\"displayName\": \"Aarav Shrestha\""))
        assertTrue(json.contains("\"title\": \"Patan Walk\""))

        val csv = DataExportHelper.toCsv(sample)
        assertTrue(csv.startsWith("Section,Field,Value"))
        assertTrue(csv.contains("\"Chiya, music, and mountain trails\""))

        val txt = DataExportHelper.toPlainText(sample)
        assertTrue(txt.contains("MERO MAYALU - PERSONAL DATA EXPORT"))
        assertTrue(txt.contains("Patan Walk"))
    }

    @Test
    fun nepaliCultureQuizBank_allQuestionsAreValidAndBilingual() {
        val qs = NepaliCultureQuizBank.questions
        assertTrue(qs.size >= 10)
        qs.forEach { q ->
            assertTrue(q.questionEn.isNotBlank() && q.questionNe.isNotBlank())
            assertEquals(4, q.optionsEn.size)
            assertEquals(4, q.optionsNe.size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.explanationEn.isNotBlank() && q.explanationNe.isNotBlank())
        }
        assertTrue(qs.any { it.category == QuizCategory.LANGUAGES })
        assertTrue(qs.any { it.category == QuizCategory.NORMS })
        assertTrue(qs.any { it.category == QuizCategory.VALUES })
    }
}
