package com.example

import com.example.core.audio.MeroRingtoneCatalog
import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.domain.SavedNote
import com.example.core.firebase.MapsGroundingService
import com.example.core.localization.AppStrings
import com.example.core.util.BikramSambatConverter
import com.example.core.util.DataExportHelper
import com.example.core.util.ExportUserData
import com.example.feature.chat.ChatLinkHelper
import com.example.feature.culture.NepaliCultureQuizBank
import com.example.feature.culture.QuizCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(20, MeroThemeCatalog.entries.size)
        assertEquals(22, MeroFontCatalog.entries.size)
        assertTrue(MeroFontCatalog.entries.any { it == MeroFontCatalog.SYSTEM })
        assertTrue(MeroFontCatalog.entries.any { it == MeroFontCatalog.KURALE })
        assertTrue(MeroThemeCatalog.entries.any { it == MeroThemeCatalog.ANNAPURNA_AURORA })
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

    @Test
    fun locationServiceHelper_privacyGridAndCompatibilityWorkCorrectly() {
        val roundedLat = com.example.core.util.LocationServiceHelper.roundToCoarsePrivacyGrid(27.717245)
        val roundedLng = com.example.core.util.LocationServiceHelper.roundToCoarsePrivacyGrid(85.323987)
        assertEquals(27.72, roundedLat, 0.001)
        assertEquals(85.32, roundedLng, 0.001)

        val me = com.example.core.domain.PublicProfile(
            uid = "u1",
            displayName = "Rohan",
            district = "Kathmandu",
            province = "Bagmati",
            interests = listOf("Chiya", "Music", "Travel"),
            favoriteActivities = listOf("chiya_walk", "momo_tasting"),
            currentVibeId = "chiya_guff",
            locationSharingEnabled = true,
            approxLat = 27.72,
            approxLng = 85.32
        )
        val other = com.example.core.domain.PublicProfile(
            uid = "u2",
            displayName = "Srijana",
            district = "Lalitpur",
            province = "Bagmati",
            interests = listOf("Chiya", "Culture", "Music"),
            favoriteActivities = listOf("chiya_walk"),
            currentVibeId = "chiya_guff",
            locationSharingEnabled = true,
            approxLat = 27.66,
            approxLng = 85.32
        )
        val distKm = com.example.core.util.LocationServiceHelper.estimateDistanceKm(me, other)
        assertNotNull(distKm)
        assertTrue(distKm!! in 1..15)

        val comp = com.example.core.util.LocationServiceHelper.computeCompatibility(me, other)
        assertTrue(comp.totalPercent in 75..98)
        assertTrue(comp.matchedTags.contains("Chiya"))
        assertTrue(com.example.core.util.LocationServiceHelper.NEPALI_VIBE_CARDS.isNotEmpty())
        assertTrue(com.example.core.util.LocationServiceHelper.NEPALI_ACTIVITY_OPTIONS.isNotEmpty())
        assertTrue(com.example.core.util.LocationServiceHelper.DAILY_QUESTIONS.isNotEmpty())
        assertTrue(com.example.core.util.LocationServiceHelper.NEPALI_CITIES.any { it.cityEn.equals("Itahari", ignoreCase = true) })

        val onlineUser = me.copy(onlineNow = true)
        val offlineUser = other.copy(onlineNow = false)
        assertTrue(com.example.core.util.LocationServiceHelper.isProfileOnline(onlineUser))
        assertTrue(!com.example.core.util.LocationServiceHelper.isProfileOnline(offlineUser))
        assertEquals("Online", com.example.core.util.LocationServiceHelper.formatPresenceLabel(onlineUser, isNepali = false))
        assertEquals("Offline", com.example.core.util.LocationServiceHelper.formatPresenceLabel(offlineUser, isNepali = false))
    }

    @Test
    fun v6MaxFeatures_ringtonesLinksQuickRepliesAndInstantHangoutWork() {
        assertEquals(6, MeroRingtoneCatalog.options.size)
        assertTrue(MeroRingtoneCatalog.options.any { it.id == MeroRingtoneCatalog.ID_DEVICE_DEFAULT })
        assertTrue(MeroRingtoneCatalog.options.any { it.id == MeroRingtoneCatalog.ID_CUSTOM })

        val sampleMsg = "Check https://meromayalu.np/plan or www.visitnepal.com and email hi@nepal.org or tel:+9779800000000"
        val links = ChatLinkHelper.extractLinks(sampleMsg)
        assertEquals(4, links.size)
        assertEquals("https://meromayalu.np/plan", links[0].normalizedUri)
        assertEquals("https://www.visitnepal.com", links[1].normalizedUri)
        assertEquals("https://nepal.org", links[2].normalizedUri) // verify domain extraction works cleanly
        assertTrue(links.any { it.normalizedUri == "tel:+9779800000000" })

        val maleSuggestionsEn = ChatLinkHelper.quickRepliesFor("Male", isNepali = false)
        val femaleSuggestionsNe = ChatLinkHelper.quickRepliesFor("Female", isNepali = true)
        assertTrue(maleSuggestionsEn.size >= 4)
        assertTrue(femaleSuggestionsNe.size >= 4)
        assertFalse(maleSuggestionsEn == ChatLinkHelper.quickRepliesFor("Female", isNepali = false))

        assertTrue(AppStrings.English.disclaimerBody.isNotBlank())
        assertTrue(AppStrings.Nepali.disclaimerBody.isNotBlank())

        val itinerary = MapsGroundingService().buildInstantHangoutItinerary(
            city = "Itahari",
            budgetNpr = 2000,
            groupSize = 2,
            durationLabel = "Half Day",
            preferredActivities = listOf("Chiya & Cafe", "Nature & Park"),
            isNepali = false,
            forceFreshVariation = false
        )
        assertTrue(itinerary.stops.isNotEmpty())
        assertTrue(itinerary.city.contains("Itahari", ignoreCase = true))
    }
}
