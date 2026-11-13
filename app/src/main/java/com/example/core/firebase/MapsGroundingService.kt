package com.example.core.firebase

import android.util.Log
import com.example.core.domain.HeritagePlacesResult
import com.example.core.domain.MapsGroundedPlace
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "MapsGroundingService"

class MapsGroundingService {

    suspend fun discoverNepaliPlaces(
        query: String,
        cityHint: String,
        latitude: Double = 27.7172,
        longitude: Double = 85.3240,
        isNepali: Boolean = true
    ): Result<HeritagePlacesResult> {
        return try {
            val retrievalConfig = RetrievalConfig.builder()
                .setLatLng(LatLng(latitude = latitude, longitude = longitude))
                .setLanguageCode(if (isNepali) "ne-NP" else "en-US")
                .build()
            val toolConfig = ToolConfig(retrievalConfig = retrievalConfig)
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.8-flash",
                toolConfig = toolConfig,
                tools = listOf(Tool.googleMaps())
            )
            val languageInstruction = if (isNepali) {
                "Respond concisely in natural Nepali (Devanagari) with warm cultural context for a respectful meetup or heritage walk."
            } else {
                "Respond concisely in warm English with cultural context for a respectful meetup or heritage walk."
            }
            val prompt = "Find real, public cultural heritage sites, temples, cafes, or scenic meetup spots in Nepal near $cityHint matching: \"$query\". $languageInstruction Highlight up to 4 places."

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val summaryText = response.text?.trim().orEmpty()
            val groundingChunks = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
            val places = groundingChunks.orEmpty().mapNotNull { chunk ->
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

            if (summaryText.isBlank() && places.isEmpty()) {
                Result.failure(IllegalStateException("No grounded places returned for this query."))
            } else {
                Result.success(HeritagePlacesResult(summaryText = summaryText, places = places))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Maps grounding timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Maps grounding failed", e)
            Result.failure(e)
        }
    }
}
