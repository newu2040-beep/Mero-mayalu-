package com.example.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.core.domain.PublicProfile
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class NepaliCityLocation(
    val cityEn: String,
    val cityNe: String,
    val district: String,
    val province: String,
    val lat: Double,
    val lng: Double
) {
    val publicLabelEn: String get() = "$cityEn, Nepal"
    val publicLabelNe: String get() = "$cityNe, नेपाल"
}

data class CoarseDeviceLocation(
    val coarseLat: Double,
    val coarseLng: Double,
    val matchedCity: NepaliCityLocation
)

data class CompatibilityBreakdown(
    val totalPercent: Int,
    val sharedInterestsScore: Int,
    val culturalPromptsScore: Int,
    val locationProximityScore: Int,
    val sharedActivitiesScore: Int,
    val matchedTags: List<String>,
    val summaryEn: String,
    val summaryNe: String
)

data class NepaliVibeCard(
    val id: String,
    val emoji: String,
    val titleEn: String,
    val titleNe: String,
    val subtitleEn: String,
    val subtitleNe: String
)

data class NepaliActivityOption(
    val id: String,
    val emoji: String,
    val labelEn: String,
    val labelNe: String
)

data class MayaluDailyQuestion(
    val id: String,
    val questionEn: String,
    val questionNe: String,
    val sampleAnswersEn: List<String>,
    val sampleAnswersNe: List<String>
)

object LocationServiceHelper {

    val NEPALI_CITIES = listOf(
        NepaliCityLocation("Kathmandu", "काठमाडौं", "Kathmandu", "Bagmati", 27.7172, 85.3240),
        NepaliCityLocation("Patan (Lalitpur)", "पाटन (ललितपुर)", "Lalitpur", "Bagmati", 27.6644, 85.3188),
        NepaliCityLocation("Bhaktapur", "भक्तपुर", "Bhaktapur", "Bagmati", 27.6710, 85.4298),
        NepaliCityLocation("Pokhara", "पोखरा", "Kaski", "Gandaki", 28.2096, 83.9856),
        NepaliCityLocation("Bharatpur (Chitwan)", "भरतपुर (चितवन)", "Chitwan", "Bagmati", 27.6833, 84.4333),
        NepaliCityLocation("Dharan", "धरान", "Sunsari", "Koshi", 26.8125, 87.2836),
        NepaliCityLocation("Itahari", "इटहरी", "Sunsari", "Koshi", 26.6646, 87.2718),
        NepaliCityLocation("Biratnagar", "विराटनगर", "Morang", "Koshi", 26.4525, 87.2718),
        NepaliCityLocation("Butwal", "बुटवल", "Rupandehi", "Lumbini", 27.7006, 83.4484),
        NepaliCityLocation("Lumbini (Bhairahawa)", "लुम्बिनी (भैरहवा)", "Rupandehi", "Lumbini", 27.5000, 83.4500),
        NepaliCityLocation("Ilam", "इलाम", "Ilam", "Koshi", 26.9080, 87.9260),
        NepaliCityLocation("Janakpur", "जनकपुर", "Dhanusha", "Madhesh", 26.7288, 85.9263),
        NepaliCityLocation("Hetauda", "हेटौंडा", "Makwanpur", "Bagmati", 27.4284, 85.0322),
        NepaliCityLocation("Nepalgunj", "नेपालगञ्ज", "Banke", "Lumbini", 28.0500, 81.6167),
        NepaliCityLocation("Dhangadhi", "धनगढी", "Kailali", "Sudurpashchim", 28.6833, 80.6000),
        NepaliCityLocation("Birendranagar (Surkhet)", "वीरेन्द्रनगर (सुर्खेत)", "Surkhet", "Karnali", 28.6000, 81.6333)
    )

    val NEPALI_VIBE_CARDS = listOf(
        NepaliVibeCard(
            id = "chiya_guff",
            emoji = "☕",
            titleEn = "Chiya & Warm Guff",
            titleNe = "मट्का चिया र न्यानो गफ",
            subtitleEn = "Ready for meaningful conversation over spiced tea",
            subtitleNe = "चियाको चुस्कीसँगै आत्मीय कुराकानीको मुडमा"
        ),
        NepaliVibeCard(
            id = "heritage_walk",
            emoji = "🏛️",
            titleEn = "Durbar Square Stroll",
            titleNe = "सम्पदा र डबली पदयात्रा",
            subtitleEn = "Exploring Patan or Bhaktapur courtyards at sunset",
            subtitleNe = "पाटन वा भक्तपुरको ऐतिहासिक आँगनमा साँझको डुलाइ"
        ),
        NepaliVibeCard(
            id = "mountain_peace",
            emoji = "🏔️",
            titleEn = "Himalayan Calm",
            titleNe = "हिमाली शान्ति र प्रकृति",
            subtitleEn = "Peaceful mindset, acoustic Nepali tunes & fresh air",
            subtitleNe = "शान्त मन, मीठो नेपाली धुन र प्रकृतिको आनन्द"
        ),
        NepaliVibeCard(
            id = "momo_date",
            emoji = "🥟",
            titleEn = "Jhol Momo Craving",
            titleNe = "तातो झोल मःमः मुड",
            subtitleEn = "Looking for a foodie partner for authentic momo",
            subtitleNe = "मीठो मःमः र परम्परागत स्वादको खोजीमा"
        ),
        NepaliVibeCard(
            id = "festive_joy",
            emoji = "🪔",
            titleEn = "Festive Cultural Glow",
            titleNe = "चाडपर्व र सांस्कृतिक उमङ्ग",
            subtitleEn = "Celebrating family traditions, music & lights",
            subtitleNe = "नेपाली परम्परा, संगीत र उज्यालो उत्सवको मुड"
        ),
        NepaliVibeCard(
            id = "book_cafe",
            emoji = "📚",
            titleEn = "Quiet Book Cafe",
            titleNe = "शान्त पुस्तक र कफी",
            subtitleEn = "Reading Nepali literature & slow afternoon coffee",
            subtitleNe = "नेपाली साहित्य अध्ययन र शान्त कफी समय"
        )
    )

    val NEPALI_ACTIVITY_OPTIONS = listOf(
        NepaliActivityOption("chiya_walk", "☕", "Chiya & Heritage Walk", "चिया र सम्पदा पदयात्रा"),
        NepaliActivityOption("momo_tasting", "🥟", "Momo & Newari Food Tour", "मःमः र नेवारी खाजा"),
        NepaliActivityOption("morning_temple", "🔔", "Morning Temple Visit", "बिहानी मन्दिर दर्शन"),
        NepaliActivityOption("hiking_nature", "🥾", "Short Hill Hike (Shivapuri/Sarangkot)", "छोटो हाइकिङ र प्रकृति"),
        NepaliActivityOption("book_art", "🎨", "Book Cafe & Art Gallery", "पुस्तक क्याफे र कला प्रदर्शनी"),
        NepaliActivityOption("live_acoustic", "🎸", "Nepali Folk / Acoustic Music", "नेपाली सुगम तथा लोक संगीत"),
        NepaliActivityOption("pottery_craft", "🏺", "Pottery & Traditional Craft", "माटोको कला र हस्तकला"),
        NepaliActivityOption("lakeside_sunset", "🌅", "Lakeside / Riverside Sunset", "ताल वा नदी किनार सूर्यास्त")
    )

    val DAILY_QUESTIONS = listOf(
        MayaluDailyQuestion(
            id = "q_sunday",
            questionEn = "What is your ideal Saturday afternoon hangout in Nepal?",
            questionNe = "नेपालमा शनिबारको दिउँसो बिताउने तपाईंको सबैभन्दा मनपर्ने तरिका के हो?",
            sampleAnswersEn = listOf(
                "Matka chiya at Patan Durbar Square followed by a quiet museum walk",
                "Boating on Phewa Lake and watching the Annapurna sunset",
                "Reading a book at a cozy cafe with hot jhol momo"
            ),
            sampleAnswersNe = listOf(
                "पाटन दरबार स्क्वायरमा मट्का चिया र शान्त सम्पदा अवलोकन",
                "फेवा तालको किनारमा डुङ्गा सयर र हिमालको दृश्यावलोकन",
                "शान्त क्याफेमा पुस्तक पढ्दै तातो झोल मःमः खाने"
            )
        ),
        MayaluDailyQuestion(
            id = "q_values",
            questionEn = "Which Nepali cultural value or tradition means the most to you in a relationship?",
            questionNe = "सम्बन्धमा तपाईंलाई कुन नेपाली सांस्कृतिक मूल्य वा परम्परा सबैभन्दा महत्वपूर्ण लाग्छ?",
            sampleAnswersEn = listOf(
                "Mutual respect for both families and elders (Aadar & Samman)",
                "Kindness, patience, and standing together through every season",
                "Celebrating festivals like Dashain, Tihar, and Lhosar together"
            ),
            sampleAnswersNe = listOf(
                "दुवै परिवार र अग्रजहरूप्रति आदर र सम्मान",
                "धैर्यता, इमानदारी र सुख-दुःखमा सधैं साथ दिने भावना",
                "दशैं, तिहार र ल्होसार जस्ता पर्वहरू सँगै मनाउने रमाइलो"
            )
        ),
        MayaluDailyQuestion(
            id = "q_travel",
            questionEn = "If we planned a 1-day cultural trip together, which destination would you pick?",
            questionNe = "यदि हामीले एक दिनको छोटो सांस्कृतिक यात्रा गर्ने हो भने तपाईं कुन ठाउँ रोज्नुहुन्छ?",
            sampleAnswersEn = listOf(
                "Bhaktapur Pottery Square & Dattatreya heritage walk",
                "Godawari Botanical Garden & Phulchowki foothills",
                "Peace Pagoda hike & lakeside coffee in Pokhara"
            ),
            sampleAnswersNe = listOf(
                "भक्तपुरको पोर्टरी स्क्वायर र दत्तात्रय सम्पदा भ्रमण",
                "गोदावरी वनस्पति उद्यान र हरियाली पदयात्रा",
                "पोखराको विश्व शान्ति स्तूप र ताल किनार कफी"
            )
        )
    )

    val RESPECTFUL_COMPLIMENTS = listOf(
        "kind_energy" to Pair(
            "Your profile radiates genuine warmth, cultural respect, and kindness. 🌸",
            "तपाईंको प्रोफाइलमा आत्मीय न्यानोपन, सांस्कृतिक सम्मान र सकारात्मक ऊर्जा झल्किन्छ। 🌸"
        ),
        "heritage_taste" to Pair(
            "I really admire your love for Nepali heritage, traditions, and meaningful conversations! 🏛️",
            "नेपाली सम्पदा, परम्परा र अर्थपूर्ण संवादप्रतिको तपाईंको रुचि निकै प्रशंसनीय छ! 🏛️"
        ),
        "thoughtful_prompts" to Pair(
            "Your cultural prompt answers are so thoughtful and grounded. Would love to share a cup of chiya someday! ☕",
            "तपाईंका सांस्कृतिक विचारहरू निकै गहिरा र प्रेरणादायी छन्। कुनै दिन चिया गफ गर्न पाए खुशी लाग्नेछ! ☕"
        ),
        "bright_smile" to Pair(
            "Your vibe and interests feel like a peaceful Himalayan morning! 🏔️✨",
            "तपाईंको व्यक्तित्व र रुचिहरू हिमाली बिहानी जस्तै शान्त र उज्यालो लाग्यो! 🏔️✨"
        )
    )

    fun hasLocationPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    /**
     * Rounds coordinates to 2 decimal places (~1.1 km resolution) so exact home/street
     * coordinates are NEVER stored or exposed.
     */
    fun roundToCoarsePrivacyGrid(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCoarseDeviceLocation(context: Context): CoarseDeviceLocation? {
        if (!hasLocationPermission(context)) return null

        val fusedPoint: Pair<Double, Double>? = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    val client = LocationServices.getFusedLocationProviderClient(context)
                    val cts = CancellationTokenSource()
                    cont.invokeOnCancellation { cts.cancel() }
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                cont.resume(loc.latitude to loc.longitude)
                            } else {
                                client.lastLocation
                                    .addOnSuccessListener { last ->
                                        if (cont.isActive) {
                                            cont.resume(if (last != null) last.latitude to last.longitude else null)
                                        }
                                    }
                                    .addOnFailureListener {
                                        if (cont.isActive) cont.resume(null)
                                    }
                            }
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

        val rawCoords = fusedPoint ?: runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (netLoc != null) netLoc.latitude to netLoc.longitude else null
        }.getOrNull() ?: return null

        // Check if device is inside Nepal bounding box (approx lat 26.0..30.5, lng 80.0..88.3)
        // If running on an emulator outside Nepal (e.g., Mountain View default 37.42, -122.08),
        // map gracefully to Kathmandu Valley while keeping coarse privacy grid!
        val isInsideNepal = rawCoords.first in 26.0..30.6 && rawCoords.second in 80.0..88.4
        val effectiveLat = if (isInsideNepal) rawCoords.first else 27.7172
        val effectiveLng = if (isInsideNepal) rawCoords.second else 85.3240

        val coarseLat = roundToCoarsePrivacyGrid(effectiveLat)
        val coarseLng = roundToCoarsePrivacyGrid(effectiveLng)
        val nearestCity = findNearestNepaliCity(coarseLat, coarseLng)

        return CoarseDeviceLocation(
            coarseLat = coarseLat,
            coarseLng = coarseLng,
            matchedCity = nearestCity
        )
    }

    fun findNearestNepaliCity(lat: Double, lng: Double): NepaliCityLocation {
        return NEPALI_CITIES.minByOrNull { city ->
            haversineKm(lat, lng, city.lat, city.lng)
        } ?: NEPALI_CITIES.first()
    }

    fun cityForDistrictOrName(name: String): NepaliCityLocation {
        val clean = name.trim().lowercase()
        return NEPALI_CITIES.firstOrNull {
            it.district.lowercase() == clean ||
                it.cityEn.lowercase().contains(clean) ||
                clean.contains(it.district.lowercase())
        } ?: NEPALI_CITIES.first()
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Computes approximate distance in kilometers between the current user and another profile.
     * Respects privacy: if the other user has `locationSharingEnabled == false`, uses district-level
     * center coordinates or returns null if they hid distance.
     */
    fun estimateDistanceKm(myProfile: PublicProfile?, otherProfile: PublicProfile): Int? {
        if (myProfile == null) return null
        val myCoords = if (myProfile.locationSharingEnabled && myProfile.approxLat != 0.0 && myProfile.approxLng != 0.0) {
            myProfile.approxLat to myProfile.approxLng
        } else {
            val c = cityForDistrictOrName(myProfile.district)
            c.lat to c.lng
        }

        val otherCoords = if (otherProfile.locationSharingEnabled && otherProfile.approxLat != 0.0 && otherProfile.approxLng != 0.0) {
            otherProfile.approxLat to otherProfile.approxLng
        } else {
            val c = cityForDistrictOrName(otherProfile.district)
            c.lat to c.lng
        }

        val rawKm = haversineKm(myCoords.first, myCoords.second, otherCoords.first, otherCoords.second)
        // Never show < 1 km to preserve privacy
        return rawKm.roundToInt().coerceAtLeast(1)
    }

    fun formatDistanceBadge(distanceKm: Int?, isNepali: Boolean): String {
        if (distanceKm == null) return ""
        return if (isNepali) "~$distanceKm कि.मि. टाढा" else "~$distanceKm km away"
    }

    fun vibeById(vibeId: String): NepaliVibeCard? =
        NEPALI_VIBE_CARDS.firstOrNull { it.id == vibeId }

    fun todayQuestion(): MayaluDailyQuestion {
        val dayIndex = ((System.currentTimeMillis() / (24L * 3600_000L)).toInt()).coerceAtLeast(0)
        return DAILY_QUESTIONS[dayIndex % DAILY_QUESTIONS.size]
    }

    fun computeCompatibility(me: PublicProfile?, other: PublicProfile): CompatibilityBreakdown {
        if (me == null) {
            return CompatibilityBreakdown(
                totalPercent = 78,
                sharedInterestsScore = 80,
                culturalPromptsScore = 76,
                locationProximityScore = 78,
                sharedActivitiesScore = 78,
                matchedTags = other.interests.take(3),
                summaryEn = "Strong cultural alignment and shared appreciation for Nepali traditions.",
                summaryNe = "नेपाली संस्कृति र परम्पराप्रति समान रुचि र सकारात्मक मेल।"
            )
        }

        val myInterests = (me.interests + me.hobbies).map { it.lowercase().trim() }.toSet()
        val otherInterests = (other.interests + other.hobbies).map { it.lowercase().trim() }.toSet()
        val sharedInterests = other.interests.filter { it.lowercase().trim() in myInterests }

        val interestRatio = if (myInterests.isEmpty() || otherInterests.isEmpty()) {
            0.65
        } else {
            val overlap = myInterests.intersect(otherInterests).size
            (0.55 + (overlap.toDouble() / 4.0).coerceAtMost(0.43))
        }
        val interestsScore = (interestRatio * 100).roundToInt().coerceIn(58, 98)

        val promptCount = other.prompts.size + (if (other.culturalBadge.isNotBlank()) 1 else 0)
        val culturalScore = (65 + promptCount * 7).coerceIn(64, 97)

        val distKm = estimateDistanceKm(me, other) ?: 25
        val locationScore = when {
            me.district.equals(other.district, ignoreCase = true) -> 95
            me.province.equals(other.province, ignoreCase = true) -> 86
            distKm <= 30 -> 88
            distKm <= 120 -> 78
            else -> 70
        }

        val myActs = me.favoriteActivities.toSet()
        val otherActs = other.favoriteActivities.toSet()
        val sharedActs = myActs.intersect(otherActs)
        val sameVibe = me.currentVibeId.isNotBlank() && me.currentVibeId == other.currentVibeId
        val activityScore = (70 + sharedActs.size * 10 + (if (sameVibe) 8 else 0)).coerceIn(68, 98)

        val total = ((interestsScore * 0.35) + (culturalScore * 0.25) + (locationScore * 0.20) + (activityScore * 0.20))
            .roundToInt()
            .coerceIn(62, 98)

        val summaryEn = buildString {
            append("$total% Mayalu Match! ")
            if (sharedInterests.isNotEmpty()) {
                append("You both enjoy ${sharedInterests.take(2).joinToString(" & ")}. ")
            }
            if (me.district.equals(other.district, ignoreCase = true)) {
                append("Both based in ${other.district} for easy heritage walks.")
            } else {
                append("Complementary cultural values and lifestyle preferences.")
            }
        }

        val summaryNe = buildString {
            append("$total% मायालु मेल! ")
            if (sharedInterests.isNotEmpty()) {
                append("तपाईं दुवैमा ${sharedInterests.take(2).joinToString(" र ")} प्रति समान रुचि छ। ")
            }
            if (me.district.equals(other.district, ignoreCase = true)) {
                append("दुवै जना ${other.district} मा हुनुहुन्छ।")
            } else {
                append("सांस्कृतिक सोच र जीवनशैलीमा सुन्दर सामञ्जस्यता छ।")
            }
        }

        return CompatibilityBreakdown(
            totalPercent = total,
            sharedInterestsScore = interestsScore,
            culturalPromptsScore = culturalScore,
            locationProximityScore = locationScore,
            sharedActivitiesScore = activityScore,
            matchedTags = sharedInterests.ifEmpty { other.interests.take(3) },
            summaryEn = summaryEn,
            summaryNe = summaryNe
        )
    }

    fun isProfileOnline(profile: PublicProfile?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (profile == null || !profile.onlineNow) return false
        val lastMs = profile.lastActiveAt?.toDate()?.time ?: return true
        return (nowMs - lastMs) <= 15 * 60_000L
    }

    fun formatPresenceLabel(
        profile: PublicProfile?,
        isNepali: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        if (isProfileOnline(profile, nowMs)) {
            return if (isNepali) "अनलाइन" else "Online"
        }
        val lastMs = profile?.lastActiveAt?.toDate()?.time
            ?: profile?.updatedAt?.toDate()?.time
            ?: return if (isNepali) "अफलाइन" else "Offline"
        val diffMin = ((nowMs - lastMs).coerceAtLeast(0L) / 60_000L)
        return when {
            diffMin < 60L -> if (isNepali) "${diffMin.coerceAtLeast(1)} मि. अघि सक्रिय" else "Active ${diffMin.coerceAtLeast(1)}m ago"
            diffMin < 24L * 60L -> {
                val hrs = diffMin / 60L
                if (isNepali) "$hrs घण्टा अघि" else "Active ${hrs}h ago"
            }
            else -> if (isNepali) "अफलाइन" else "Offline"
        }
    }
}
