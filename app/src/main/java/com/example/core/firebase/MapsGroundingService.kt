package com.example.core.firebase

import android.util.Log
import com.example.core.domain.AvoidPlaceInfo
import com.example.core.domain.HangoutItinerary
import com.example.core.domain.HangoutStop
import com.example.core.domain.HeritagePlacesResult
import com.example.core.domain.MapsGroundedPlace
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.generationConfig
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private fun encodeQueryParam(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private const val TAG = "MapsGroundingService"
private const val FAST_MODEL_NAME = "gemini-3.5-flash-lite"
private const val AI_FAST_TIMEOUT_MS = 1_600L
private const val CIRCUIT_BREAKER_COOLDOWN_MS = 5 * 60_000L

data class VerifiedNepalSpot(
    val city: String,
    val titleEn: String,
    val titleNe: String,
    val categoryEn: String,
    val categoryNe: String,
    val openingHours: String,
    val avgCostNpr: Int,
    val foodEn: String,
    val foodNe: String,
    val activityEn: String,
    val activityNe: String,
    val backupEn: String,
    val backupNe: String,
    val lat: Double,
    val lng: Double,
    val activityTags: List<String> = listOf("Heritage Walk", "Chiya & Cafe", "Photography")
) {
    val mapsUri: String
        get() = "https://www.google.com/maps/search/?api=1&query=${encodeQueryParam("$titleEn, $city, Nepal")}"
}

class MapsGroundingService {

    @Volatile
    private var remoteAiDegradedUntilMs: Long = 0L

    private val variationCounter = AtomicInteger(0)
    private val itineraryCache = ConcurrentHashMap<String, HangoutItinerary>()
    private val placesCache = ConcurrentHashMap<String, HeritagePlacesResult>()

    private val itinerarySchema by lazy {
        Schema.obj(
        mapOf(
            "title" to Schema.string("Short warm title of the hangout plan"),
            "startingLocation" to Schema.string("Public starting landmark"),
            "totalEstimatedBudgetNpr" to Schema.integer("Total estimated budget in NPR"),
            "weatherAndTimingNote" to Schema.string("1 short sentence on best time and weather"),
            "stops" to Schema.array(
                Schema.obj(
                    mapOf(
                        "stepNumber" to Schema.integer("1-indexed stop number"),
                        "placeName" to Schema.string("Real place or landmark name in Nepal"),
                        "category" to Schema.string("Heritage, Cafe, Nature, Park, or Viewpoint"),
                        "timeSlot" to Schema.string("Time window e.g. 10:00 AM - 11:30 AM"),
                        "travelTimeFromPrevious" to Schema.string("Walk or ride duration from previous stop"),
                        "estimatedCostNpr" to Schema.integer("Estimated cost in NPR"),
                        "openingHours" to Schema.string("Opening hours"),
                        "recommendedActivity" to Schema.string("1 concise sentence on what to do"),
                        "foodSuggestion" to Schema.string("Local food or chiya suggestion"),
                        "backupAlternative" to Schema.string("Nearby backup spot")
                    )
                )
            ),
            "placesToAvoid" to Schema.array(
                Schema.obj(
                    mapOf(
                        "placeOrArea" to Schema.string("Specific road, area, or peak hour to avoid"),
                        "reason" to Schema.string("Practical reason e.g. traffic, dust, or unlit after dusk")
                    )
                )
            ),
            "safetyPrecautions" to Schema.array(Schema.string("Short safety tip")),
            "packingAndTravelTips" to Schema.array(Schema.string("Short travel or payment tip"))
        )
    )
    }

    private val structuredPlannerModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.35f
                maxOutputTokens = 680
                responseMimeType = "application/json"
                responseSchema = itinerarySchema
            }
        )
    }

    private fun isCircuitOpen(): Boolean =
        System.currentTimeMillis() < remoteAiDegradedUntilMs

    private fun tripCircuitBreaker(error: Throwable) {
        Log.w(TAG, "Fast fallback activated for Mayalu AI (${error.message})")
        remoteAiDegradedUntilMs = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS
    }

    private fun itineraryCacheKey(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        isNepali: Boolean
    ): String {
        val normActs = preferredActivities.map { it.trim().lowercase() }.sorted().joinToString(",")
        return "${city.trim().lowercase()}|$budgetNpr|$groupSize|${durationLabel.lowercase()}|$normActs|$isNepali"
    }

    fun buildInstantHangoutItinerary(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        isNepali: Boolean = true,
        forceFreshVariation: Boolean = false
    ): HangoutItinerary {
        val cleanCity = city.trim().ifBlank { "Kathmandu" }
        val safeBudget = budgetNpr.coerceIn(300, 50_000)
        val safeGroup = groupSize.coerceIn(1, 10)
        val activitiesList = preferredActivities.ifEmpty {
            listOf("Heritage Walk", "Chiya & Cafe", "Momo & Local Food")
        }
        val key = itineraryCacheKey(cleanCity, safeBudget, safeGroup, durationLabel, activitiesList, isNepali)
        if (!forceFreshVariation) {
            itineraryCache[key]?.let { cached -> return cached }
        }
        val seed = variationCounter.getAndIncrement()
        return buildCuratedHangoutItinerary(
            city = cleanCity,
            budgetNpr = safeBudget,
            groupSize = safeGroup,
            durationLabel = durationLabel,
            preferredActivities = activitiesList,
            isNepali = isNepali,
            variationSeed = seed
        ).also { built ->
            if (!forceFreshVariation) {
                itineraryCache[key] = built
            }
        }
    }

    suspend fun enhanceItineraryWithGemini(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        latitude: Double = 27.7172,
        longitude: Double = 85.3240,
        isNepali: Boolean = true
    ): HangoutItinerary? = withContext(Dispatchers.IO) {
        if (isCircuitOpen()) return@withContext null
        val cleanCity = city.trim().ifBlank { "Kathmandu" }
        val safeBudget = budgetNpr.coerceIn(300, 50_000)
        val safeGroup = groupSize.coerceIn(1, 10)
        val activitiesList = preferredActivities.ifEmpty {
            listOf("Heritage Walk", "Chiya & Cafe", "Momo & Local Food")
        }
        val key = itineraryCacheKey(cleanCity, safeBudget, safeGroup, durationLabel, activitiesList, isNepali)
        val desiredStopCount = when {
            durationLabel.contains("2", ignoreCase = true) -> 2
            durationLabel.contains("Full", ignoreCase = true) || durationLabel.contains("6", ignoreCase = true) -> 4
            else -> 3
        }
        try {
            val langInstruction = if (isNepali) {
                "Use natural Nepali (Devanagari). Keep every description 1 short sentence."
            } else {
                "Use warm English. Keep every description 1 short sentence."
            }
            val prompt = """
                Plan a $durationLabel Nepal hangout in $cleanCity (lat=$latitude, lng=$longitude).
                Budget: NPR $safeBudget total for $safeGroup people.
                Activities: ${activitiesList.joinToString(", ")}.
                Include $desiredStopCount real public stops in order, 2 placesToAvoid with practical traffic/lighting reasons, 2 safetyPrecautions, and 2 packingAndTravelTips.
                $langInstruction
            """.trimIndent()

            val response = withTimeout(AI_FAST_TIMEOUT_MS) {
                structuredPlannerModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            val parsed = parseItineraryJson(
                rawJson = rawJson,
                city = cleanCity,
                durationLabel = durationLabel,
                groupSize = safeGroup,
                budgetNpr = safeBudget,
                isNepali = isNepali
            )
            if (parsed != null && parsed.stops.isNotEmpty()) {
                itineraryCache[key] = parsed
                parsed
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            tripCircuitBreaker(e)
            null
        }
    }

    suspend fun discoverNepaliPlaces(
        query: String,
        cityHint: String,
        latitude: Double = 27.7172,
        longitude: Double = 85.3240,
        isNepali: Boolean = true
    ): Result<HeritagePlacesResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().ifBlank { cityHint }
        val cacheKey = "${cleanQuery.lowercase()}|${cityHint.lowercase()}|$isNepali"
        placesCache[cacheKey]?.let { return@withContext Result.success(it) }
        val seed = variationCounter.getAndIncrement()

        if (isCircuitOpen()) {
            val (curatedSummary, curatedList) = curatedPlacesForQuery(cleanQuery, cityHint, isNepali, seed)
            val result = HeritagePlacesResult(summaryText = curatedSummary, places = curatedList)
            placesCache[cacheKey] = result
            return@withContext Result.success(result)
        }

        try {
            val retrievalConfig = RetrievalConfig.builder()
                .setLatLng(LatLng(latitude = latitude, longitude = longitude))
                .setLanguageCode(if (isNepali) "ne-NP" else "en-US")
                .build()
            val toolConfig = ToolConfig(retrievalConfig = retrievalConfig)
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = FAST_MODEL_NAME,
                generationConfig = generationConfig {
                    temperature = 0.35f
                    maxOutputTokens = 360
                },
                toolConfig = toolConfig,
                tools = listOf(Tool.googleMaps())
            )
            val languageInstruction = if (isNepali) {
                "Respond concisely in natural Nepali (Devanagari) with opening hours and approximate NPR cost."
            } else {
                "Respond concisely in warm English with opening hours and approximate NPR cost."
            }
            val prompt = "List 4 real, safe public heritage sites, cafes, or parks in Nepal near $cityHint for \"$cleanQuery\". $languageInstruction Keep it brief."

            val response = withTimeout(AI_FAST_TIMEOUT_MS) {
                model.generateContent(prompt)
            }

            val summaryText = response.text?.trim().orEmpty()
            val groundingChunks = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
            val groundedPlaces = groundingChunks.orEmpty().mapNotNull { chunk ->
                val maps = chunk.maps
                val title = maps?.title
                val uri = maps?.uri
                if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                    MapsGroundedPlace(
                        title = title,
                        uri = uri,
                        placeId = maps.placeId
                    )
                } else null
            }.distinctBy { it.uri }

            val (curatedSummary, curatedPlaces) = curatedPlacesForQuery(cleanQuery, cityHint, isNepali, seed)
            val finalPlaces = groundedPlaces.ifEmpty { curatedPlaces }
            val finalSummary = summaryText.ifBlank { curatedSummary }
            val result = HeritagePlacesResult(summaryText = finalSummary, places = finalPlaces)
            placesCache[cacheKey] = result
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            tripCircuitBreaker(error)
            val (curatedSummary, curatedList) = curatedPlacesForQuery(cleanQuery, cityHint, isNepali, seed)
            val result = HeritagePlacesResult(summaryText = curatedSummary, places = curatedList)
            placesCache[cacheKey] = result
            Result.success(result)
        }
    }

    suspend fun generateSmartHangoutItinerary(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        latitude: Double = 27.7172,
        longitude: Double = 85.3240,
        isNepali: Boolean = true
    ): Result<HangoutItinerary> = withContext(Dispatchers.IO) {
        val instant = buildInstantHangoutItinerary(
            city = city,
            budgetNpr = budgetNpr,
            groupSize = groupSize,
            durationLabel = durationLabel,
            preferredActivities = preferredActivities,
            isNepali = isNepali
        )
        val ai = enhanceItineraryWithGemini(
            city = city,
            budgetNpr = budgetNpr,
            groupSize = groupSize,
            durationLabel = durationLabel,
            preferredActivities = preferredActivities,
            latitude = latitude,
            longitude = longitude,
            isNepali = isNepali
        )
        Result.success(ai ?: instant)
    }

    private fun parseItineraryJson(
        rawJson: String,
        city: String,
        durationLabel: String,
        groupSize: Int,
        budgetNpr: Int,
        isNepali: Boolean
    ): HangoutItinerary? = runCatching {
        if (rawJson.isBlank()) return null
        val root = JSONObject(rawJson)
        val title = root.optString("title").ifBlank {
            if (isNepali) "$city मायालु भेटघाट योजना" else "$city Mayalu Hangout Plan"
        }
        val startingLocation = root.optString("startingLocation").ifBlank { "$city Center" }
        val totalBudget = root.optInt("totalEstimatedBudgetNpr", budgetNpr).coerceAtLeast(200)
        val weatherNote = root.optString("weatherAndTimingNote").ifBlank {
            if (isNepali) "दिउँसो ३ देखि ६ बजेको समय पैदल यात्रा र चिया गफका लागि उत्तम हुन्छ।"
            else "Late morning or golden hour (3:00 PM – 6:00 PM) offers the most pleasant weather and safe visibility."
        }

        val stopsArr = root.optJSONArray("stops")
        val stops = mutableListOf<HangoutStop>()
        if (stopsArr != null) {
            for (i in 0 until stopsArr.length()) {
                val obj = stopsArr.optJSONObject(i) ?: continue
                val placeName = obj.optString("placeName").trim()
                if (placeName.isBlank()) continue
                stops.add(
                    HangoutStop(
                        stepNumber = obj.optInt("stepNumber", i + 1),
                        placeName = placeName,
                        category = obj.optString("category", "Heritage & Cafe"),
                        timeSlot = obj.optString("timeSlot", "${10 + i * 2}:00 - ${11 + i * 2}:30"),
                        travelTimeFromPrevious = obj.optString("travelTimeFromPrevious", if (i == 0) "Start" else "12 min walk"),
                        estimatedCostNpr = obj.optInt("estimatedCostNpr", totalBudget / 3),
                        openingHours = obj.optString("openingHours", "8:00 AM - 7:30 PM"),
                        recommendedActivity = obj.optString("recommendedActivity", ""),
                        foodSuggestion = obj.optString("foodSuggestion", "Matka Chiya & Momo"),
                        mapsUri = "https://www.google.com/maps/search/?api=1&query=${encodeQueryParam("$placeName, $city, Nepal")}",
                        backupAlternative = obj.optString("backupAlternative", "Nearby Heritage Courtyard Cafe")
                    )
                )
            }
        }

        val avoidArr = root.optJSONArray("placesToAvoid")
        val avoidList = mutableListOf<AvoidPlaceInfo>()
        if (avoidArr != null) {
            for (i in 0 until avoidArr.length()) {
                val obj = avoidArr.optJSONObject(i) ?: continue
                val area = obj.optString("placeOrArea").trim()
                val reason = obj.optString("reason").trim()
                if (area.isNotBlank() && reason.isNotBlank()) {
                    avoidList.add(AvoidPlaceInfo(area, reason))
                }
            }
        }

        val safetyArr = root.optJSONArray("safetyPrecautions")
        val safetyList = mutableListOf<String>()
        if (safetyArr != null) {
            for (i in 0 until safetyArr.length()) {
                val item = safetyArr.optString(i).trim()
                if (item.isNotBlank()) safetyList.add(item)
            }
        }

        val tipsArr = root.optJSONArray("packingAndTravelTips")
        val tipsList = mutableListOf<String>()
        if (tipsArr != null) {
            for (i in 0 until tipsArr.length()) {
                val item = tipsArr.optString(i).trim()
                if (item.isNotBlank()) tipsList.add(item)
            }
        }

        HangoutItinerary(
            title = title,
            city = city,
            startingLocation = startingLocation,
            durationLabel = durationLabel,
            groupSize = groupSize,
            totalEstimatedBudgetNpr = totalBudget,
            weatherAndTimingNote = weatherNote,
            stops = stops,
            placesToAvoid = avoidList.ifEmpty { defaultAvoidList(city, isNepali) },
            safetyPrecautions = safetyList.ifEmpty { defaultSafetyList(isNepali) },
            packingAndTravelTips = tipsList.ifEmpty { defaultTipsList(isNepali) }
        )
    }.getOrNull()

    private fun curatedPlacesForQuery(
        query: String,
        cityHint: String,
        isNepali: Boolean,
        seed: Int = 0
    ): Pair<String, List<MapsGroundedPlace>> {
        val qLower = "$query $cityHint".lowercase()
        val normalizedCity = normalizeCityKey(if (query.isNotBlank()) "$query $cityHint" else cityHint)

        val matchedCitySpots = VERIFIED_NEPAL_SPOTS.filter { spot ->
            spot.city.equals(normalizedCity, ignoreCase = true) ||
                qLower.contains(spot.city.lowercase()) ||
                spot.titleEn.lowercase().contains(query.lowercase()) ||
                spot.categoryEn.lowercase().contains(query.lowercase()) ||
                spot.activityTags.any { tag -> query.contains(tag.split(" ").first(), ignoreCase = true) }
        }.ifEmpty {
            VERIFIED_NEPAL_SPOTS.filter { it.city.equals(normalizedCity, ignoreCase = true) }
        }.ifEmpty {
            VERIFIED_NEPAL_SPOTS
        }

        val rotatedSpots = if (matchedCitySpots.size > 4 && seed > 0) {
            val shift = seed % matchedCitySpots.size
            (matchedCitySpots.drop(shift) + matchedCitySpots.take(shift)).take(4)
        } else {
            matchedCitySpots.take(4)
        }

        val displayArea = rotatedSpots.firstOrNull()?.city ?: cityHint
        val summary = if (isNepali) {
            buildString {
                append("📍 $displayArea आसपासका प्रमाणित र सुरक्षित सम्पदा तथा भेटघाट स्थलहरू:\n\n")
                rotatedSpots.forEachIndexed { idx, s ->
                    append("${idx + 1}. ${s.titleNe} (${s.categoryNe})\n")
                    append("   • समय: ${s.openingHours} | अनुमानित खर्च: रु. ${s.avgCostNpr}\n")
                    append("   • गतिविधि: ${s.activityNe} (${s.foodNe})\n\n")
                }
                append("यी सार्वजनिक स्थानहरू पहिलो भेटघाट, चिया गफ र सांस्कृतिक अवलोकनका लागि सुरक्षित र लोकप्रिय छन्।")
            }
        } else {
            buildString {
                append("📍 Verified & Safe Heritage and Meetup Spots around $displayArea:\n\n")
                rotatedSpots.forEachIndexed { idx, s ->
                    append("${idx + 1}. ${s.titleEn} (${s.categoryEn})\n")
                    append("   • Hours: ${s.openingHours} | Est. Cost: NPR ${s.avgCostNpr}\n")
                    append("   • Highlight: ${s.activityEn} (${s.foodEn})\n\n")
                }
                append("All recommended spots are well-lit, public landmarks ideal for respectful cultural walks and conversations.")
            }
        }

        val groundedList = rotatedSpots.map { spot ->
            MapsGroundedPlace(
                title = if (isNepali) "${spot.titleNe} (${spot.titleEn}) · ${spot.city}" else "${spot.titleEn} · ${spot.city}",
                uri = spot.mapsUri,
                placeId = spot.titleEn.lowercase().replace(" ", "_")
            )
        }
        return summary to groundedList
    }

    private fun buildCuratedHangoutItinerary(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        isNepali: Boolean,
        variationSeed: Int = 0
    ): HangoutItinerary {
        val cityKey = normalizeCityKey(city)
        val displayCity = city.trim().ifBlank { cityKey }
        val desiredStopCount = when {
            durationLabel.contains("2", ignoreCase = true) -> 2
            durationLabel.contains("Full", ignoreCase = true) || durationLabel.contains("6", ignoreCase = true) -> 4
            else -> 3
        }

        // Filter spots by city, then score by how many preferredActivities they match
        val candidatesForCity = VERIFIED_NEPAL_SPOTS.filter {
            it.city.equals(cityKey, ignoreCase = true)
        }.ifEmpty {
            VERIFIED_NEPAL_SPOTS.take(6)
        }

        val scoredCandidates = candidatesForCity.sortedByDescending { spot ->
            preferredActivities.count { pref ->
                spot.activityTags.any { tag -> tag.equals(pref, ignoreCase = true) }
            }
        }

        val shiftedCandidates = if (scoredCandidates.size > 1 && variationSeed > 0) {
            val offset = variationSeed % scoredCandidates.size
            scoredCandidates.drop(offset) + scoredCandidates.take(offset)
        } else {
            scoredCandidates
        }

        val selectedSpots = buildList {
            addAll(shiftedCandidates.take(desiredStopCount))
            // If a full-day 4-stop plan needs more stops than the city has, supplement from nearby valley/scenic spots
            if (size < desiredStopCount) {
                val extra = VERIFIED_NEPAL_SPOTS.filterNot { it in this }.take(desiredStopCount - size)
                addAll(extra)
            }
        }

        val timeWindows = when (desiredStopCount) {
            2 -> listOf("2:00 PM – 3:00 PM", "3:15 PM – 4:15 PM")
            4 -> listOf("10:00 AM – 11:30 AM", "12:00 PM – 1:45 PM", "2:15 PM – 4:00 PM", "4:15 PM – 6:00 PM")
            else -> listOf("11:00 AM – 12:30 PM", "12:45 PM – 2:30 PM", "3:00 PM – 4:45 PM")
        }

        val travelTimes = listOf(
            if (isNepali) "सुरुवात बिन्दु" else "Starting Point",
            if (isNepali) "१०–१२ मिनेट पैदल यात्रा" else "10–12 min scenic walk",
            if (isNepali) "१२ मिनेट पैदल / छोटो सवारी" else "12 min walk / short ride",
            if (isNepali) "१५ मिनेट पैदल यात्रा" else "15 min walk"
        )

        val weights = when (desiredStopCount) {
            2 -> listOf(0.45, 0.55)
            4 -> listOf(0.20, 0.35, 0.25, 0.20)
            else -> listOf(0.28, 0.44, 0.28)
        }

        val standardKeys = setOf(
            "Heritage Walk", "Chiya & Cafe", "Momo & Local Food",
            "Nature & Park", "Temple & Peace", "Photography"
        )
        val customActivities = preferredActivities.filter { act ->
            standardKeys.none { it.equals(act, ignoreCase = true) }
        }

        val stops = selectedSpots.mapIndexed { idx, spot ->
            val weight = weights.getOrElse(idx) { 1.0 / desiredStopCount }
            val rawStopCost = if (idx == selectedSpots.lastIndex) {
                (budgetNpr - selectedSpots.take(idx).sumOf { i ->
                    val w = weights.getOrElse(selectedSpots.indexOf(i)) { 1.0 / desiredStopCount }
                    (budgetNpr * w).toInt().coerceAtLeast(80)
                }).coerceAtLeast(80)
            } else {
                (budgetNpr * weight).toInt().coerceAtLeast(80)
            }
            val foodNote = if (budgetNpr >= 2500) {
                if (isNepali) "${spot.foodNe} + विशेष थकाली/नेवारी सेट" else "${spot.foodEn} + Special Platter"
            } else {
                if (isNepali) spot.foodNe else spot.foodEn
            }
            val baseActivity = if (isNepali) spot.activityNe else spot.activityEn
            val customAct = customActivities.getOrNull(idx)
            val tailoredActivity = if (!customAct.isNullOrBlank()) {
                if (isNepali) "$baseActivity · विशेष रुचि: $customAct"
                else "$baseActivity · Focus: $customAct"
            } else {
                baseActivity
            }
            HangoutStop(
                stepNumber = idx + 1,
                placeName = if (isNepali) "${spot.titleNe} (${spot.titleEn})" else spot.titleEn,
                category = if (isNepali) spot.categoryNe else spot.categoryEn,
                timeSlot = timeWindows.getOrElse(idx) { "4:30 PM – 6:00 PM" },
                travelTimeFromPrevious = travelTimes.getOrElse(idx) { "10 min walk" },
                estimatedCostNpr = rawStopCost,
                openingHours = spot.openingHours,
                recommendedActivity = tailoredActivity,
                foodSuggestion = foodNote,
                mapsUri = spot.mapsUri,
                backupAlternative = if (isNepali) spot.backupNe else spot.backupEn
            )
        }

        val totalCost = stops.sumOf { it.estimatedCostNpr }
        val actFocus = preferredActivities.take(2).joinToString(" & ").ifBlank {
            if (isNepali) "सम्पदा र चिया" else "Heritage & Chiya"
        }

        return HangoutItinerary(
            title = if (isNepali) {
                "$displayCity: $actFocus भेटघाट योजना (${stops.size} स्थान)"
            } else {
                "$displayCity: $actFocus Hangout Plan (${stops.size} Stops)"
            },
            city = displayCity,
            startingLocation = stops.firstOrNull()?.placeName ?: "$displayCity Main Gate",
            durationLabel = durationLabel,
            groupSize = groupSize,
            totalEstimatedBudgetNpr = totalCost,
            weatherAndTimingNote = if (isNepali) {
                "बिहान १०:३० देखि साँझ ५:३० सम्मको उज्यालो समय मौसम, फोटोग्राफी र सुरक्षित यातायातका लागि सबैभन्दा उपयुक्त हुन्छ।"
            } else {
                "Daylight hours (10:30 AM – 5:30 PM) provide comfortable weather, open heritage courtyards, and easy return transport."
            },
            stops = stops,
            placesToAvoid = defaultAvoidList(displayCity, isNepali),
            safetyPrecautions = defaultSafetyList(isNepali),
            packingAndTravelTips = defaultTipsList(isNepali)
        )
    }

    private fun normalizeCityKey(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("patan") || lower.contains("lalitpur") || lower.contains("jham") || lower.contains("godawari") || lower.contains("ललितपुर") || lower.contains("पाटन") -> "Patan"
            lower.contains("bhaktapur") || lower.contains("nagarkot") || lower.contains("thimi") || lower.contains("भक्तपुर") -> "Bhaktapur"
            lower.contains("pokhara") || lower.contains("kaski") || lower.contains("lakeside") || lower.contains("phewa") || lower.contains("पोखरा") -> "Pokhara"
            lower.contains("chitwan") || lower.contains("bharatpur") || lower.contains("sauraha") || lower.contains("चितवन") -> "Chitwan"
            lower.contains("itahari") || lower.contains("इटहरी") || lower.contains("tarahara") || lower.contains("taltalaiya") -> "Itahari"
            lower.contains("dharan") || lower.contains("sunsari") || lower.contains("bhedetar") || lower.contains("biratnagar") || lower.contains("धरान") -> "Dharan"
            lower.contains("lumbini") || lower.contains("butwal") || lower.contains("rupandehi") || lower.contains("bhairahawa") || lower.contains("लुम्बिनी") || lower.contains("बुटवल") -> "Lumbini"
            lower.contains("ilam") || lower.contains("kanyam") || lower.contains("antu") || lower.contains("इलाम") -> "Ilam"
            lower.contains("janakpur") || lower.contains("dhanusha") || lower.contains("mithila") || lower.contains("जनकपुर") -> "Janakpur"
            else -> "Kathmandu"
        }
    }

    private fun defaultAvoidList(city: String, isNepali: Boolean): List<AvoidPlaceInfo> = if (isNepali) {
        listOf(
            AvoidPlaceInfo(
                placeOrArea = "$city को मुख्य चक्रपथ र व्यस्त राजमार्ग (साँझ ५–७ बजे)",
                reason = "कार्यालय छुट्ने समयमा अत्यधिक सवारी चाप, धुलो र ध्वनि प्रदूषणका कारण शान्त कुराकानी गर्न कठिन हुन्छ।"
            ),
            AvoidPlaceInfo(
                placeOrArea = "साँझ ६:३० पछि एकान्त वन वा उज्यालो नभएका पदमार्ग",
                reason = "सूर्यास्तपछि सार्वजनिक यातायात कम हुने र सुरक्षाका दृष्टिले सार्वजनिक उज्यालो स्थान मात्र उपयुक्त हुने।"
            )
        )
    } else {
        listOf(
            AvoidPlaceInfo(
                placeOrArea = "Peak-hour Ring Road & highway junctions in $city (5:00 PM – 7:00 PM)",
                reason = "Heavy commuter traffic, dust, and noise make walking and conversation uncomfortable."
            ),
            AvoidPlaceInfo(
                placeOrArea = "Unlit hillside trails or isolated parks after 6:30 PM",
                reason = "Limited street lighting and fewer public transport options after dusk; stick to active heritage squares and cafes."
            )
        )
    }

    private fun defaultSafetyList(isNepali: Boolean): List<String> = if (isNepali) {
        listOf(
            "सधैं मानिसहरूको चहलपहल हुने सार्वजनिक सम्पदा स्थल, पार्क वा क्याफेमा मात्र भेटघाट गर्नुहोस्।",
            "आफ्नो भेटघाट तालिका र स्थान परिवार वा नजिकको साथीलाई जानकारी गराउनुहोस्।",
            "आफ्नै सवारी वा प्रमाणित राइड-शेयरिङ (Pathao / InDrive / सार्वजनिक यातायात) प्रयोग गर्नुहोस्।"
        )
    } else {
        listOf(
            "Always meet in open, well-populated public heritage squares, gardens, or cafes.",
            "Share your hangout itinerary and live check-in time with a trusted friend or family member.",
            "Use your own transport or verified ride-sharing apps so you can leave comfortably anytime."
        )
    }

    private fun defaultTipsList(isNepali: Boolean): List<String> = if (isNepali) {
        listOf(
            "मन्दिर र सम्पदा क्षेत्रमा प्रवेश टिकट तथा साना चिया पसलका लागि रु. २००–५०० नगद साथमा राख्नुहोस् (अन्यत्र Fonepay/QR चल्छ)।",
            "सम्पदा क्षेत्रको इँटा र ढुङ्गाको बाटोमा हिँड्न सजिलो हुने जुत्ता लगाउनुहोस्।",
            "दिउँसोको घाम वा अचानकको मौसम परिवर्तनका लागि पानीको बोतल र छाता/सनग्लास बोक्नुहोस्।"
        )
    } else {
        listOf(
            "Keep NPR 300–500 in small cash notes for heritage entry tickets and local matka chiya stalls alongside QR/Fonepay.",
            "Wear comfortable walking shoes suitable for brick-paved Durbar Square courtyards.",
            "Carry a reusable water bottle and light layer for late-afternoon valley breezes."
        )
    }

    companion object {
        val VERIFIED_NEPAL_SPOTS = listOf(
            // Patan / Lalitpur (4 stops)
            VerifiedNepalSpot(
                city = "Patan",
                titleEn = "Patan Durbar Square & Museum Courtyard",
                titleNe = "पाटन दरबार स्क्वायर र संग्रहालय परिसर",
                categoryEn = "UNESCO Heritage & Garden Cafe",
                categoryNe = "सम्पदा र बगैंचा क्याफे",
                openingHours = "9:00 AM – 6:30 PM",
                avgCostNpr = 350,
                foodEn = "Spiced Matka Chiya & Newari Bara",
                foodNe = "मट्का चिया र नेवारी बारा",
                activityEn = "Explore Malla-era bronze galleries and relax in the quiet museum garden courtyard",
                activityNe = "पाटन संग्रहालयको कलात्मक आँगन र शान्त बगैंचामा सम्पदा अवलोकन तथा संवाद",
                backupEn = "Swotha Square Traditional Courtyard Cafes (2 min walk)",
                backupNe = "स्वठ स्क्वायरका परम्परागत क्याफेहरू (२ मिनेट पैदल)",
                lat = 27.6727,
                lng = 85.3253,
                activityTags = listOf("Heritage Walk", "Chiya & Cafe", "Photography", "Temple & Peace")
            ),
            VerifiedNepalSpot(
                city = "Patan",
                titleEn = "Pimbahal Pond (Pim Bahal Pokhari)",
                titleNe = "पिम्बहाल पोखरी, पाटन",
                categoryEn = "Historic Lakeside Pavilion",
                categoryNe = "ऐतिहासिक पोखरी र चिया चौतारी",
                openingHours = "7:00 AM – 7:00 PM",
                avgCostNpr = 300,
                foodEn = "Yomari, Chatamari & Herbal Tea",
                foodNe = "योमरी, चतामरी र हर्बल चिया",
                activityEn = "Evening walk around the 14th-century pond with views of Chandeswori Temple",
                activityNe = "ऐतिहासिक पोखरी वरिपरि साँझपखको शान्त पैदल यात्रा र परम्परागत खाजा",
                backupEn = "Jhamsikhel Book & Bakery Cafes",
                backupNe = "झम्सिखेलका पुस्तक तथा बेकरी क्याफेहरू",
                lat = 27.6756,
                lng = 85.3211,
                activityTags = listOf("Momo & Local Food", "Chiya & Cafe", "Heritage Walk", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Patan",
                titleEn = "Godawari Botanical Garden",
                titleNe = "गोदावरी वनस्पति उद्यान",
                categoryEn = "Nature & Botanical Park",
                categoryNe = "प्रकृति र वनस्पति उद्यान",
                openingHours = "9:00 AM – 5:00 PM",
                avgCostNpr = 400,
                foodEn = "Fresh Trout Lunch or Picnic Snacks & Hot Coffee",
                foodNe = "स्थानीय खाजा र तातो कफी",
                activityEn = "Peaceful stroll across glasshouses, fern gardens, and mountain spring streams",
                activityNe = "हरियाली बगैंचा, फूलबारी र शान्त वातावरणमा प्रकृति अवलोकन",
                backupEn = "Central Zoo & Jawalakhel Lakeside",
                backupNe = "जावलाखेल चिडियाखाना परिसर",
                lat = 27.5967,
                lng = 85.3890,
                activityTags = listOf("Nature & Park", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Patan",
                titleEn = "Hiranya Varna Mahavihar (Golden Temple) & Nagbahal",
                titleNe = "हिरण्यवर्ण महाविहार (गोल्डेन टेम्पल) र नागबहाल",
                categoryEn = "Sacred Buddhist Courtyard",
                categoryNe = "ऐतिहासिक बौद्ध महाविहार",
                openingHours = "7:00 AM – 6:30 PM",
                avgCostNpr = 250,
                foodEn = "Artisanal Iced Coffee & Cinnamon Roll at Nagbahal",
                foodNe = "नागबहालको स्थानीय कफी र बेकरी",
                activityEn = "Admire 12th-century gilded copper repoussé art and tranquil Nagbahal courtyard",
                activityNe = "गोल्डेन टेम्पलको धातुकला अवलोकन र नागबहालमा शान्त चिया गफ",
                backupEn = "Patan Museum Cafe",
                backupNe = "पाटन संग्रहालय क्याफे",
                lat = 27.6746,
                lng = 85.3242,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Chiya & Cafe")
            ),

            // Kathmandu (4 stops)
            VerifiedNepalSpot(
                city = "Kathmandu",
                titleEn = "Garden of Dreams (Swapna Bagaicha)",
                titleNe = "स्वप्न बगैंचा (Garden of Dreams), केशर महल",
                categoryEn = "Neo-Classical Heritage Garden",
                categoryNe = "ऐतिहासिक शान्त बगैंचा",
                openingHours = "9:00 AM – 9:00 PM",
                avgCostNpr = 550,
                foodEn = "Iced Lemon Tea, Pastries & Momo Platter",
                foodNe = "लेमन चिया, बेकरी र मःमः",
                activityEn = "Relax by lotus ponds, pergolas, and quiet reading lawns away from street noise",
                activityNe = "कमलपोखरी र शान्त हरियालीमा पुस्तक तथा आत्मीय कुराकानी",
                backupEn = "Kaiser Library Courtyard & Durbar Marg Cafes",
                backupNe = "दरबारमार्गका शान्त कफी पसलहरू",
                lat = 27.7142,
                lng = 85.3145,
                activityTags = listOf("Nature & Park", "Chiya & Cafe", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Kathmandu",
                titleEn = "Boudhanath Stupa Rooftop Courtyards",
                titleNe = "बौद्धनाथ स्तूप र रूफटप क्याफे",
                categoryEn = "UNESCO Spiritual Heritage",
                categoryNe = "विश्व सम्पदा र आध्यात्मिक स्थल",
                openingHours = "6:00 AM – 8:00 PM",
                avgCostNpr = 450,
                foodEn = "Butter Tea, Kothey Momo & Tingmo",
                foodNe = "कोथे मःमः, तिङमो र हिमाली चिया",
                activityEn = "Clockwise kora walk around the stupa followed by golden-hour rooftop conversation",
                activityNe = "बौद्ध स्तूप परिक्रमा र छतबाट साँझको दीप प्रज्वलन अवलोकन",
                backupEn = "Taragaon Museum & Contemporary Art Gallery (5 min walk)",
                backupNe = "तारागाउँ संग्रहालय परिसर (५ मिनेट पैदल)",
                lat = 27.7215,
                lng = 85.3620,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Momo & Local Food", "Chiya & Cafe", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Kathmandu",
                titleEn = "Swayambhunath Foothill & Manjushree Park",
                titleNe = "स्वयम्भू मञ्जुश्री पार्क र शान्ति पोखरी",
                categoryEn = "Hilltop Heritage & Valley Viewpoint",
                categoryNe = "सम्पदा र उपत्यका भ्यू-पोइन्ट",
                openingHours = "7:00 AM – 6:30 PM",
                avgCostNpr = 300,
                foodEn = "Local Masala Chiya & Sel Roti",
                foodNe = "मसला चिया र सेलरोटी",
                activityEn = "Panoramic view of Kathmandu Valley and peaceful walk through Manjushree Park",
                activityNe = "काठमाडौं उपत्यकाको मनोरम दृश्य र मञ्जुश्री पार्क भ्रमण",
                backupEn = "Basantapur Durbar Square Heritage Walk",
                backupNe = "बसन्तपुर दरबार स्क्वायर सम्पदा पदयात्रा",
                lat = 27.7149,
                lng = 85.2903,
                activityTags = listOf("Temple & Peace", "Nature & Park", "Photography", "Heritage Walk")
            ),
            VerifiedNepalSpot(
                city = "Kathmandu",
                titleEn = "Basantapur Durbar Square & Freak Street Courtyards",
                titleNe = "बसन्तपुर दरबार स्क्वायर र झोंछेँ क्याफे",
                categoryEn = "Living Royal Heritage Square",
                categoryNe = "ऐतिहासिक दरबार र कलात्मक चोक",
                openingHours = "8:00 AM – 7:30 PM",
                avgCostNpr = 400,
                foodEn = "Newari Jhol Momo,Aloo Tareko & Spiced Chiya",
                foodNe = "झोल मःमः, आलु तारेको र मसला चिया",
                activityEn = "Walk past Kumari Ghar, Hanuman Dhoka museums, and historic woodcarved temples",
                activityNe = "कुमारी घर, हनुमानढोका संग्रहालय र काष्ठकला अवलोकन",
                backupEn = "Dharahara Sundhara Heritage Plaza",
                backupNe = "धरहरा सुन्धारा परिसर",
                lat = 27.7042,
                lng = 85.3075,
                activityTags = listOf("Heritage Walk", "Momo & Local Food", "Chiya & Cafe", "Photography")
            ),

            // Bhaktapur (4 stops)
            VerifiedNepalSpot(
                city = "Bhaktapur",
                titleEn = "Bhaktapur Durbar Square & Pottery Square",
                titleNe = "भक्तपुर दरबार र बोलाछें पोर्टरी स्क्वायर",
                categoryEn = "Living Newa Heritage & Craft",
                categoryNe = "नेवाः सम्पदा र माटोको कला",
                openingHours = "8:00 AM – 6:30 PM",
                avgCostNpr = 350,
                foodEn = "Authentic King Curd (Juju Dhau) & Jhol Momo",
                foodNe = "भक्तपुरको प्रसिद्ध जुजु धौ र झोल मःमः",
                activityEn = "Try hands-on clay spinning at Pottery Square and admire 55-Window Palace",
                activityNe = "माटोको भाँडा बनाउने अनुभव र ५५ झ्याले दरबार अवलोकन",
                backupEn = "Nyatapola Temple Rooftop Cafes at Taumadhi Square",
                backupNe = "टौमढी स्क्वायर (न्यातापोल मन्दिर) का क्याफेहरू",
                lat = 27.6710,
                lng = 85.4298,
                activityTags = listOf("Heritage Walk", "Momo & Local Food", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Bhaktapur",
                titleEn = "Siddha Pokhari & Dattatreya Square",
                titleNe = "सिद्धपोखरी र दत्तात्रय स्क्वायर, भक्तपुर",
                categoryEn = "Historic Pond & Woodcarving Square",
                categoryNe = "ऐतिहासिक पोखरी र काष्ठकला",
                openingHours = "7:00 AM – 6:30 PM",
                avgCostNpr = 250,
                foodEn = "Samay Baji & Warm Ginger Tea",
                foodNe = "समयबजी र अदुवा चिया",
                activityEn = "Feed fish at Siddha Pokhari and see the famous Peacock Window at Pujari Math",
                activityNe = "सिद्धपोखरी परिक्रमा र मयूर झ्यालको काष्ठकला अवलोकन",
                backupEn = "Rani Kot or Gundu Scenic Meadow",
                backupNe = "गुण्डु वा रानीकोट भ्यू-पोइन्ट",
                lat = 27.6718,
                lng = 85.4192,
                activityTags = listOf("Heritage Walk", "Temple & Peace", "Chiya & Cafe")
            ),
            VerifiedNepalSpot(
                city = "Bhaktapur",
                titleEn = "Taumadhi Square & Nyatapola Rooftop Cafe",
                titleNe = "टौमढी स्क्वायर र न्यातापोल रूफटप क्याफे",
                categoryEn = "Five-Tiered Pagoda & View Cafe",
                categoryNe = "पाँचतले मन्दिर र रूफटप चिया",
                openingHours = "8:00 AM – 7:00 PM",
                avgCostNpr = 400,
                foodEn = "Juju Dhau Lassi, Bara &Himalayan Coffee",
                foodNe = "जुजु धौ लस्सी, बारा र नेपाली कफी",
                activityEn = "Enjoy rooftop tea overlooking Nepal's tallest pagoda temple and bustling brick courtyard",
                activityNe = "न्यातापोल मन्दिरको दृश्य हेर्दै छतमा बसेर कफी र आत्मीय संवाद",
                backupEn = "Kamal Binayak Pond Park",
                backupNe = "कमलविनायक पोखरी उद्यान",
                lat = 27.6714,
                lng = 85.4293,
                activityTags = listOf("Chiya & Cafe", "Temple & Peace", "Photography", "Momo & Local Food")
            ),

            // Pokhara (4 stops)
            VerifiedNepalSpot(
                city = "Pokhara",
                titleEn = "Phewa Lakeside Footpath & Barahi Ghat",
                titleNe = "फेवा ताल किनार फुटपाथ र वाराही घाट, पोखरा",
                categoryEn = "Lakeside Promenade & Boating",
                categoryNe = "ताल किनार पदमार्ग र डुङ्गा सयर",
                openingHours = "6:30 AM – 8:00 PM",
                avgCostNpr = 500,
                foodEn = "Himalayan Arabica Coffee & Fresh Lake Fish/Thakali Set",
                foodNe = "नेपाली कफी र थकाली खाना/खाजा",
                activityEn = "Scenic lakeside walk along Pame/Baidam footpath with Machhapuchhre views",
                activityNe = "माछापुच्छ्रे हिमालको छायाँ हेर्दै फेवा किनारमा शान्त पदयात्रा",
                backupEn = "International Mountain Museum, Gharipatan",
                backupNe = "अन्तर्राष्ट्रिय पर्वतीय संग्रहालय, घारीपाटन",
                lat = 28.2096,
                lng = 83.9595,
                activityTags = listOf("Nature & Park", "Chiya & Cafe", "Photography", "Momo & Local Food")
            ),
            VerifiedNepalSpot(
                city = "Pokhara",
                titleEn = "World Peace Pagoda (Shanti Stupa)",
                titleNe = "विश्व शान्ति स्तूप, पोखरा",
                categoryEn = "Mountain & Lake Viewpoint",
                categoryNe = "हिमाल र फेवा ताल भ्यू-पोइन्ट",
                openingHours = "7:00 AM – 6:00 PM",
                avgCostNpr = 400,
                foodEn = "Fresh Fruit Juice, Sel Roti & Chiya at Stupa Cafe",
                foodNe = "ताजा जुस, सेलरोटी र चिया",
                activityEn = "Enjoy panoramic Annapurna range views and serene stupa gardens",
                activityNe = "अन्नपूर्ण हिमश्रृंखला र फेवा तालको दृश्यावलोकन",
                backupEn = "Begnas Lake Quiet Pier",
                backupNe = "बेगनास तालको शान्त किनार",
                lat = 28.2011,
                lng = 83.9452,
                activityTags = listOf("Temple & Peace", "Photography", "Nature & Park")
            ),
            VerifiedNepalSpot(
                city = "Pokhara",
                titleEn = "Begnas Lake & Majhikuna Promenade",
                titleNe = "बेगनास ताल र माझीकुना पदमार्ग, पोखरा",
                categoryEn = "Serene Alpine Lake & Cafe",
                categoryNe = "शान्त ताल र प्रकृति चौतारी",
                openingHours = "7:00 AM – 6:30 PM",
                avgCostNpr = 450,
                foodEn = "Steamed Momo, Roasted Corn & Local Organic Coffee",
                foodNe = "तातो मःमः, पोलेको मकै र अर्गानिक कफी",
                activityEn = "Relax by quiet crystal waters away from main city crowds with views of Lamjung Himal",
                activityNe = "शान्त बेगनास ताल किनारमा प्रकृति अवलोकन र चिया गफ",
                backupEn = "Bindhyabasini Temple Courtyard Park",
                backupNe = "विन्ध्यवासिनी मन्दिर उद्यान",
                lat = 28.1736,
                lng = 84.0958,
                activityTags = listOf("Nature & Park", "Chiya & Cafe", "Momo & Local Food")
            ),

            // Chitwan (2 stops)
            VerifiedNepalSpot(
                city = "Chitwan",
                titleEn = "Sauraha Rapti River Sunset Deck",
                titleNe = "सौराहा राप्ती नदी सूर्यास्त डेक, चितवन",
                categoryEn = "Riverside Nature & Tharu Culture",
                categoryNe = "नदी किनार प्रकृति र थारु संस्कृति",
                openingHours = "8:00 AM – 7:00 PM",
                avgCostNpr = 450,
                foodEn = "Tharu Dhikri, Masala Tea & Fresh Pakora",
                foodNe = "थारु ढिक्री, पकोडा र मसला चिया",
                activityEn = "Watch the golden sunset over Rapti River and visit the Tharu Cultural Museum",
                activityNe = "राप्ती नदी किनारमा सूर्यास्त अवलोकन र थारु सांस्कृतिक संग्रहालय भ्रमण",
                backupEn = "Narayani River Bank Promenade, Bharatpur",
                backupNe = "नारायणी नदी किनार, भरतपुर",
                lat = 27.5750,
                lng = 84.4944,
                activityTags = listOf("Nature & Park", "Heritage Walk", "Photography", "Momo & Local Food")
            ),
            VerifiedNepalSpot(
                city = "Chitwan",
                titleEn = "Narayani Riverfront & Devghat Suspension Bridge",
                titleNe = "नारायणी नदी किनार र देवघाट झोलुङ्गे पुल",
                categoryEn = "River Promenade & Spiritual Confluence",
                categoryNe = "नदी किनार पदमार्ग र धार्मिक संगम",
                openingHours = "6:30 AM – 6:30 PM",
                avgCostNpr = 300,
                foodEn = "Matka Chiya, Samosa Chaat & Kulfi",
                foodNe = "मट्का चिया, समोसा चाट र कुल्फी",
                activityEn = "Breezy walk along Narayani riverfront and peaceful visit to Devghat Dham",
                activityNe = "नारायणी किनारमा साँझको शीतल पैदल यात्रा र देवघाट दर्शन",
                backupEn = "Bishhazari Tal (20,000 Lakes) Park",
                backupNe = "बीसहजारी ताल उद्यान",
                lat = 27.7026,
                lng = 84.4285,
                activityTags = listOf("Temple & Peace", "Chiya & Cafe", "Nature & Park")
            ),

            // Dharan (2 stops)
            VerifiedNepalSpot(
                city = "Dharan",
                titleEn = "Budhasubba Temple & Vijayapur Hill",
                titleNe = "बुढासुब्बा मन्दिर र विजयपुर डाँडा, धरान",
                categoryEn = "Eastern Heritage & Hilltop Breeze",
                categoryNe = "पूर्वको प्रसिद्ध सम्पदा र डाँडा",
                openingHours = "7:00 AM – 6:00 PM",
                avgCostNpr = 300,
                foodEn = "Dharan Special Veg/Buff Momo, Alu Nimki & Chiya",
                foodNe = "धरानको प्रसिद्ध आलु निम्की, मःमः र चिया",
                activityEn = "Walk through tip-less bamboo groves at Budhasubba and enjoy Vijayapur hill breeze",
                activityNe = "बुढासुब्बाको बाँसघारी अवलोकन र विजयपुरको शीतल हावा",
                backupEn = "Bhedetar Charles Point Viewtower",
                backupNe = "भेडेटार भ्यू-पोइन्ट",
                lat = 26.8225,
                lng = 87.2951,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Momo & Local Food")
            ),
            VerifiedNepalSpot(
                city = "Dharan",
                titleEn = "Bhedetar Hill Station & Namaste Jharana Trail",
                titleNe = "भेडेटार डाँडा र नमस्ते झरना पदमार्ग",
                categoryEn = "Misty Hill Viewpoint & Cafe",
                categoryNe = "पहाडी भ्यू-पोइन्ट र चिया चौतारी",
                openingHours = "7:00 AM – 6:00 PM",
                avgCostNpr = 400,
                foodEn = "Hot Thukpa, Steamed Momo & Churpi",
                foodNe = "तातो थुक्पा, मःमः र छुर्पी",
                activityEn = "Enjoy cool mountain mist at Bhedetar Skywalk and panoramic eastern plains view",
                activityNe = "भेडेटारको चिसो मौसममा हिमाल र तराईको एकैसाथ दृश्यावलोकन",
                backupEn = "Dharan Clock Tower (Ghantaghar) Park",
                backupNe = "धरान घण्टाघर उद्यान",
                lat = 26.8617,
                lng = 87.3245,
                activityTags = listOf("Nature & Park", "Photography", "Chiya & Cafe")
            ),

            // Itahari (3 stops)
            VerifiedNepalSpot(
                city = "Itahari",
                titleEn = "Taltalaiya Wetland & Botanical Picnic Park",
                titleNe = "तालतलैया सिमसार तथा पर्यटकीय उद्यान, इटहरी",
                categoryEn = "Eco-Wetland & Boating Park",
                categoryNe = "सिमसार उद्यान र डुङ्गा सयर",
                openingHours = "7:00 AM – 6:00 PM",
                avgCostNpr = 350,
                foodEn = "Itahari Spiced Chiya, Chatpate & Local Sekuwa",
                foodNe = "इटहरीको मसला चिया, चटपटे र स्थानीय सेकुवा",
                activityEn = "Stroll along shaded lotus lakes, wooden bridges, and lush forest trails",
                activityNe = "कमल ताल, काठे पुल र हरियाली वन क्षेत्रमा शान्त पदयात्रा र डुङ्गा सयर",
                backupEn = "Jhumka Canal Promenade & Gardens",
                backupNe = "झुम्का नहर किनार र उद्यान क्षेत्र",
                lat = 26.7067,
                lng = 87.3198,
                activityTags = listOf("Nature & Park", "Photography", "Chiya & Cafe")
            ),
            VerifiedNepalSpot(
                city = "Itahari",
                titleEn = "Gokul Dham & Budhi Khola Sunset Park",
                titleNe = "गोकुलधाम मन्दिर र बुढी खोला सूर्यास्त पार्क, इटहरी",
                categoryEn = "Peaceful Temple & Riverside Walk",
                categoryNe = "शान्त मन्दिर र नदी किनार पदमार्ग",
                openingHours = "6:00 AM – 7:00 PM",
                avgCostNpr = 250,
                foodEn = "Fresh Lassi, Samosa & Matka Chiya",
                foodNe = "ताजा लस्सी, समोसा र मट्का चिया",
                activityEn = "Admire calm temple courtyards and relax by the breezy eastern riverfront",
                activityNe = "शान्त मन्दिर परिसर दर्शन र साँझको शीतल नदी किनारमा संवाद",
                backupEn = "Tarahara Agricultural Research Green Park",
                backupNe = "तरहरा हरियाली कृषि उद्यान परिसर",
                lat = 26.6788,
                lng = 87.2852,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Itahari",
                titleEn = "Itahari Central Plaza & Koshi Courtyard Cafes",
                titleNe = "इटहरी सेन्ट्रल प्लाजा र कोशी चिया चौतारी",
                categoryEn = "City Hub & Cafe Hangout",
                categoryNe = "सहरी भेटघाट र क्याफे चौतारी",
                openingHours = "8:30 AM – 8:30 PM",
                avgCostNpr = 400,
                foodEn = "Purbeli Steamed Momo, Sekuwa Platter & Iced Coffee",
                foodNe = "पूर्वेली तातो मःमः, सेकुवा र चिसो कफी",
                activityEn = "Enjoy welcoming Koshi hospitality, local bites, and lively evening coffee chats",
                activityNe = "इटहरीको न्यानो आतिथ्यमा स्थानीय खाजा र आत्मीय कफी गफ",
                backupEn = "Ocean Park & Resort Garden Cafe, Itahari",
                backupNe = "ओसियन पार्क बगैंचा क्याफे, इटहरी",
                lat = 26.6646,
                lng = 87.2718,
                activityTags = listOf("Chiya & Cafe", "Momo & Local Food")
            ),

            // Lumbini / Butwal (2 stops)
            VerifiedNepalSpot(
                city = "Lumbini",
                titleEn = "Lumbini Sacred Garden & World Peace Canal",
                titleNe = "लुम्बिनी पवित्र उद्यान र शान्ति नहर",
                categoryEn = "UNESCO Birthplace of Buddha",
                categoryNe = "विश्व सम्पदा शान्ति उद्यान",
                openingHours = "6:00 AM – 6:30 PM",
                avgCostNpr = 350,
                foodEn = "Samosa Chaat, Lassi & Herbal Tea",
                foodNe = "चिसो लस्सी र शाकाहारी खाजा",
                activityEn = "Peaceful walk along the central canal and international monastery zone",
                activityNe = "मायादेवी मन्दिर उद्यान र अन्तर्राष्ट्रिय गुम्बा क्षेत्रको शान्त भ्रमण",
                backupEn = "Manimukunda Sen Park (Phulbari), Butwal",
                backupNe = "मणिमुकुन्द सेन उद्यान (फूलबारी), बुटवल",
                lat = 27.4698,
                lng = 83.2757,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Nature & Park", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Lumbini",
                titleEn = "Manimukunda Sen Park (Phulbari) & Hill Park, Butwal",
                titleNe = "मणिमुकुन्द सेन उद्यान (फूलबारी) र हिलपार्क, बुटवल",
                categoryEn = "Historic Winter Palace Garden",
                categoryNe = "ऐतिहासिक सेनकालीन दरबार र उद्यान",
                openingHours = "8:00 AM – 6:00 PM",
                avgCostNpr = 300,
                foodEn = "Butwal Special Batuk, Chukauni & Masala Tea",
                foodNe = "बुटवलको प्रसिद्ध बटुक, चुकाउनी र मसला चिया",
                activityEn = "Explore Palpa Sen dynasty ruins, botanical gardens, and Butwal panoramic viewpoint",
                activityNe = "फूलबारी उद्यानको हरियाली र बुटवल शहरको दृश्यावलोकन",
                backupEn = "Tilottama Yogikuti Cafe Boulevard",
                backupNe = "तिलोत्तमा योगीकुटी क्याफे क्षेत्र",
                lat = 27.7052,
                lng = 83.4550,
                activityTags = listOf("Nature & Park", "Momo & Local Food", "Chiya & Cafe")
            ),

            // Ilam (2 stops)
            VerifiedNepalSpot(
                city = "Ilam",
                titleEn = "Kanyam & Antu Danda Tea Gardens",
                titleNe = "कन्याम र श्रीअन्तु चिया बगान, इलाम",
                categoryEn = "Himalayan Tea Estate & Viewpoint",
                categoryNe = "चिया बगान र सूर्योदय स्थल",
                openingHours = "6:30 AM – 6:00 PM",
                avgCostNpr = 300,
                foodEn = "First-Flush Orthodox Ilam Tea & Chhurpi",
                foodNe = "इलामको अर्गानिक अर्थोडक्स चिया र छुर्पी",
                activityEn = "Walk along manicured green tea slopes and enjoy misty hill viewpoints",
                activityNe = "हरियाली चिया बगानमा पदयात्रा र फोटो सेसन",
                backupEn = "Ilam Bazar Narayansthan & Tea Factory",
                backupNe = "इलाम बजार चिया कारखाना परिसर",
                lat = 26.8682,
                lng = 88.0819,
                activityTags = listOf("Nature & Park", "Chiya & Cafe", "Photography")
            ),
            VerifiedNepalSpot(
                city = "Ilam",
                titleEn = "Mai Pokhari Botanical Ramsar Wetland",
                titleNe = "माईपोखरी रामसार धार्मिक तथा जैविक उद्यान, इलाम",
                categoryEn = "Sacred Alpine Pond & Forest",
                categoryNe = "धार्मिक पोखरी र गुराँस वन",
                openingHours = "7:00 AM – 5:30 PM",
                avgCostNpr = 250,
                foodEn = "Warm Ginger Tea, Sel Roti & Local Honey Snacks",
                foodNe = "अदुवा चिया, सेलरोटी र स्थानीय खाजा",
                activityEn = "Peaceful clockwise walk around the nine-cornered emerald lake and orchid garden",
                activityNe = "नौ कुना भएको पवित्र माईपोखरी परिक्रमा र शान्त वातावरण",
                backupEn = "Bhaludhunga Ecotourism Viewpoint",
                backupNe = "भालुढुङ्गा भ्यू-पोइन्ट, इलाम",
                lat = 27.0067,
                lng = 87.9297,
                activityTags = listOf("Temple & Peace", "Nature & Park", "Heritage Walk")
            ),

            // Janakpur (2 stops)
            VerifiedNepalSpot(
                city = "Janakpur",
                titleEn = "Janaki Mandir & Ganga Sagar Sarovar",
                titleNe = "जानकी मन्दिर र गंगासागर सरोवर, जनकपुर",
                categoryEn = "Mithila Heritage & Art",
                categoryNe = "मिथिला सम्पदा र कला",
                openingHours = "6:00 AM – 8:00 PM",
                avgCostNpr = 300,
                foodEn = "Mithila Lassi, Peda & Hot Samosa",
                foodNe = "जनकपुरको प्रसिद्ध लस्सी, पेडा र समोसा",
                activityEn = "Admire grand Mughal-Mithila architecture, Janakpur Women's Development Center art, and evening Ganga Aarti",
                activityNe = "जानकी मन्दिर अवलोकन, मिथिला चित्रकला र गंगासागरको सन्ध्या आरती",
                backupEn = "Dhanush Sagar Courtyard Cafes",
                backupNe = "धनुष सागर परिसर",
                lat = 26.7304,
                lng = 85.9254,
                activityTags = listOf("Temple & Peace", "Heritage Walk", "Photography", "Momo & Local Food")
            ),
            VerifiedNepalSpot(
                city = "Janakpur",
                titleEn = "Kuwa Mithila Art Center & Vivaha Mandap Garden",
                titleNe = "कुवा मिथिला कला केन्द्र र विवाह मण्डप उद्यान",
                categoryEn = "Mithila Folk Art & Cultural Park",
                categoryNe = "मिथिला चित्रकला र सांस्कृतिक उद्यान",
                openingHours = "9:00 AM – 6:00 PM",
                avgCostNpr = 250,
                foodEn = "Bagiya, Taruwa & Spiced Kulhad Chiya",
                foodNe = "मिथिलाको परम्परागत बगिया, तरुवा र माटोको कपमा चिया",
                activityEn = "Discover authentic Madhubani/Mithila paintings and stroll through Vivaha Mandap gardens",
                activityNe = "मिथिला लोककला अवलोकन र शान्त उद्यानमा संवाद",
                backupEn = "Maha Ganga Aarti Ghat Pavilion",
                backupNe = "गंगा आरती घाट मण्डप",
                lat = 26.7290,
                lng = 85.9265,
                activityTags = listOf("Heritage Walk", "Chiya & Cafe", "Photography")
            )
        )
    }
}
