package com.example.core.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val dateOfBirthPrivate: String = "",
    val age: Int = 18,
    val ageVerified: Boolean = false,
    val gender: String = "Male",
    val language: String = "ne",
    val theme: String = "himalayan_dusk",
    val fontStyle: String = "mukta",
    val themeMode: String = "system",
    val useBikramSambat: Boolean = true,
    val reducedMotion: Boolean = false,
    val discoveryRadiusKm: Int = 50,
    val notificationsEnabled: Boolean = true,
    val discreetNotifications: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val visibility: String = "visible",
    val activeOutgoingRequestId: String = "",
    val moderationStatus: String = "active",
    @ServerTimestamp val acceptedTermsAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val age: Int = 18,
    val ageVerified: Boolean = false,
    val gender: String = "Female",
    val bio: String = "",
    val lookingFor: String = "",
    val photoDataList: List<String> = emptyList(),
    val bannerData: String = "preset:himalayan_dusk",
    val interests: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val prompts: List<String> = emptyList(),
    val culturalBadge: String = "",
    val province: String = "Bagmati",
    val district: String = "Kathmandu",
    val municipality: String = "",
    val publicLocationLabel: String = "Kathmandu, Nepal",
    val visibility: String = "visible",
    val moderationStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CulturePost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhoto: String = "",
    val authorLocation: String = "",
    val thoughtText: String = "",
    val imageData: String = "",
    val culturalTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostReaction(
    @DocumentId val id: String = "",
    val userUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedNote(
    @DocumentId val id: String = "",
    val title: String = "",
    val noteText: String = "",
    val sourcePostId: String = "",
    val sourceAuthorName: String = "",
    val sourceExcerpt: String = "",
    val imageData: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InterestRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val fromPhoto: String = "",
    val fromLocation: String = "",
    val toName: String = "",
    val toPhoto: String = "",
    val toLocation: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val decisionAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Connection(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val sourceRequestId: String = "",
    val conversationId: String = "",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val userAId: String = "",
    val userAName: String = "",
    val userAPhoto: String = "",
    val userBId: String = "",
    val userBName: String = "",
    val userBPhoto: String = "",
    val connectionId: String = "",
    val status: String = "ACTIVE",
    val lastMessageText: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val memberUids: List<String> = emptyList(),
    val type: String = "TEXT",
    val text: String = "",
    val mediaData: String = "",
    val audioDurationSec: Int = 0,
    val waveform: List<Int> = emptyList(),
    val moderationStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockRecord(
    @DocumentId val id: String = "",
    val blockerUid: String = "",
    val blockedUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReportRecord(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetUid: String = "",
    val category: String = "OTHER",
    val details: String = "",
    val status: String = "SUBMITTED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MapsGroundedPlace(
    val title: String,
    val uri: String,
    val placeId: String?
)

data class HeritagePlacesResult(
    val summaryText: String,
    val places: List<MapsGroundedPlace>
)
