package com.example.core.data

import android.content.Context
import com.example.R
import com.example.core.domain.BlockRecord
import com.example.core.domain.CallSession
import com.example.core.domain.ChatMessage
import com.example.core.domain.Connection
import com.example.core.domain.Conversation
import com.example.core.domain.CulturePost
import com.example.core.domain.HangoutItinerary
import com.example.core.domain.InterestRequest
import com.example.core.domain.InviteCodeRecord
import com.example.core.domain.MeetupInvitation
import com.example.core.domain.PollVote
import com.example.core.domain.PostReaction
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.example.core.domain.ReportRecord
import com.example.core.domain.SavedHangoutPlan
import com.example.core.domain.SavedNote
import com.example.core.domain.SecretCompliment
import com.example.core.domain.TodoItem
import com.example.core.domain.TogetherMemory
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.util.LocationServiceHelper
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import java.security.SecureRandom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class MeroMayaluRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun pairId(a: String, b: String): String = if (a < b) "${a}_${b}" else "${b}_${a}"

    fun sortedPair(a: String, b: String): List<String> = if (a < b) listOf(a, b) else listOf(b, a)

    // --- OBSERVE OWN PRIVATE USER ---
    fun observePrivateUser(uid: String): Flow<PrivateUser?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, "users/$uid", uid)
                throw error
            }

    // --- OBSERVE OWN PUBLIC PROFILE ---
    fun observePublicProfile(uid: String): Flow<PublicProfile?> =
        db.collection("publicProfiles").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, "publicProfiles/$uid", uid)
                throw error
            }

    // --- SAVE / UPDATE FULL PROFILE (ONBOARDING OR EDIT) ---
    suspend fun saveProfile(
        privateUser: PrivateUser,
        publicProfile: PublicProfile,
        isInitialCreate: Boolean
    ) {
        val uid = requireUserId()
        require(privateUser.age >= 18 && privateUser.ageVerified) { "Age 18+ verification required." }
        require(privateUser.gender == "Male" || privateUser.gender == "Female") { "Gender must be Male or Female." }
        require(publicProfile.photoDataList.size <= 4) { "Up to 4 profile photos are supported." }

        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("publicProfiles").document(uid)

        try {
            if (isInitialCreate) {
                val privatePayload = mapOf(
                    "uid" to uid,
                    "displayName" to privateUser.displayName.trim(),
                    "dateOfBirthPrivate" to privateUser.dateOfBirthPrivate,
                    "age" to privateUser.age,
                    "ageVerified" to true,
                    "gender" to privateUser.gender,
                    "language" to privateUser.language,
                    "theme" to privateUser.theme,
                    "fontStyle" to privateUser.fontStyle,
                    "themeMode" to privateUser.themeMode,
                    "useBikramSambat" to privateUser.useBikramSambat,
                    "reducedMotion" to privateUser.reducedMotion,
                    "discoveryRadiusKm" to privateUser.discoveryRadiusKm,
                    "notificationsEnabled" to privateUser.notificationsEnabled,
                    "discreetNotifications" to privateUser.discreetNotifications,
                    "quietHoursEnabled" to privateUser.quietHoursEnabled,
                    "showOnlineStatus" to privateUser.showOnlineStatus,
                    "visibility" to privateUser.visibility,
                    "activeOutgoingRequestId" to "",
                    "moderationStatus" to "active",
                    "acceptedTermsAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val publicPayload = mapOf(
                    "uid" to uid,
                    "displayName" to publicProfile.displayName.trim(),
                    "age" to publicProfile.age,
                    "ageVerified" to true,
                    "gender" to publicProfile.gender,
                    "bio" to publicProfile.bio.trim(),
                    "lookingFor" to publicProfile.lookingFor.trim(),
                    "photoDataList" to publicProfile.photoDataList.filter { it.isNotBlank() }.take(4),
                    "bannerData" to publicProfile.bannerData.ifBlank { "preset:himalayan_dusk" },
                    "interests" to publicProfile.interests.distinct().take(16),
                    "hobbies" to publicProfile.hobbies.take(12),
                    "prompts" to publicProfile.prompts.take(6),
                    "culturalBadge" to publicProfile.culturalBadge,
                    "province" to publicProfile.province,
                    "district" to publicProfile.district,
                    "municipality" to publicProfile.municipality.trim(),
                    "publicLocationLabel" to publicProfile.publicLocationLabel,
                    "onlineNow" to true,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "visibility" to publicProfile.visibility,
                    "moderationStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.runBatch { batch ->
                    batch.set(userRef, privatePayload)
                    batch.set(publicRef, publicPayload)
                }.await()
            } else {
                val userUpdates = mapOf(
                    "displayName" to privateUser.displayName.trim(),
                    "language" to privateUser.language,
                    "theme" to privateUser.theme,
                    "fontStyle" to privateUser.fontStyle,
                    "themeMode" to privateUser.themeMode,
                    "useBikramSambat" to privateUser.useBikramSambat,
                    "reducedMotion" to privateUser.reducedMotion,
                    "discoveryRadiusKm" to privateUser.discoveryRadiusKm,
                    "notificationsEnabled" to privateUser.notificationsEnabled,
                    "discreetNotifications" to privateUser.discreetNotifications,
                    "quietHoursEnabled" to privateUser.quietHoursEnabled,
                    "showOnlineStatus" to privateUser.showOnlineStatus,
                    "visibility" to privateUser.visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val publicUpdates = mapOf(
                    "displayName" to publicProfile.displayName.trim(),
                    "bio" to publicProfile.bio.trim(),
                    "lookingFor" to publicProfile.lookingFor.trim(),
                    "photoDataList" to publicProfile.photoDataList.filter { it.isNotBlank() }.take(4),
                    "bannerData" to publicProfile.bannerData.ifBlank { "preset:himalayan_dusk" },
                    "interests" to publicProfile.interests.distinct().take(16),
                    "hobbies" to publicProfile.hobbies.take(12),
                    "prompts" to publicProfile.prompts.take(6),
                    "culturalBadge" to publicProfile.culturalBadge,
                    "province" to publicProfile.province,
                    "district" to publicProfile.district,
                    "municipality" to publicProfile.municipality.trim(),
                    "publicLocationLabel" to publicProfile.publicLocationLabel,
                    "visibility" to publicProfile.visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.runBatch { batch ->
                    batch.update(userRef, userUpdates)
                    batch.update(publicRef, publicUpdates)
                }.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path, auth)
            throw e
        }
    }

    suspend fun updatePreferences(updates: Map<String, Any>) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val payload = updates.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            userRef.update(payload).await()
            val visibility = updates["visibility"] as? String
            val showOnline = updates["showOnlineStatus"] as? Boolean
            if (visibility != null || showOnline != null) {
                val publicPatch = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (visibility != null) publicPatch["visibility"] = visibility
                if (showOnline != null) {
                    publicPatch["onlineNow"] = showOnline
                    publicPatch["lastActiveAt"] = FieldValue.serverTimestamp()
                }
                db.collection("publicProfiles").document(uid).update(publicPatch).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            throw e
        }
    }

    suspend fun updateBannerData(bannerData: String) {
        val uid = requireUserId()
        val publicRef = db.collection("publicProfiles").document(uid)
        try {
            publicRef.update(
                mapOf(
                    "bannerData" to bannerData,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, publicRef.path, auth)
            throw e
        }
    }

    suspend fun updateProfilePhotos(photoDataList: List<String>) {
        val uid = requireUserId()
        val publicRef = db.collection("publicProfiles").document(uid)
        try {
            publicRef.update(
                mapOf(
                    "photoDataList" to photoDataList.filter { it.isNotBlank() }.take(4),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, publicRef.path, auth)
            throw e
        }
    }

    suspend fun updateInterests(interests: List<String>) {
        val uid = requireUserId()
        val publicRef = db.collection("publicProfiles").document(uid)
        try {
            publicRef.update(
                mapOf(
                    "interests" to interests.distinct().take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, publicRef.path, auth)
            throw e
        }
    }

    // --- DISCOVERY FEED (STRICT OPPOSITE GENDER QUERY ALIGNED WITH RULES) ---
    fun observeDiscoveryFeed(myGender: String): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val targetGender = if (myGender == "Male") "Female" else "Male"
        val path = "publicProfiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("gender", targetGender)
                .whereEqualTo("visibility", "visible")
                .whereEqualTo("ageVerified", true)
                .whereEqualTo("moderationStatus", "active")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.uid != uid }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun getPublicProfile(targetUid: String): PublicProfile? {
        val path = "publicProfiles/$targetUid"
        return try {
            db.document(path).get().await()
                .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            throw e
        }
    }

    // --- DISMISSAL ("Not Interested") ---
    fun observeDismissedIds(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/dismissed"
        emitAll(
            db.collection("users").document(uid).collection("dismissed")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun dismissProfile(targetUid: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("dismissed").document(targetUid)
        try {
            ref.set(
                mapOf(
                    "targetUid" to targetUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun undoDismissProfile(targetUid: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("dismissed").document(targetUid)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // --- INTEREST REQUESTS (ONE ACTIVE OUTGOING RULE) ---
    fun observeIncomingRequests(): Flow<List<InterestRequest>> = flow {
        val uid = requireUserId()
        val path = "interestRequests"
        emitAll(
            db.collection(path)
                .whereEqualTo("toUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(InterestRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeOutgoingRequests(): Flow<List<InterestRequest>> = flow {
        val uid = requireUserId()
        val path = "interestRequests"
        emitAll(
            db.collection(path)
                .whereEqualTo("fromUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(InterestRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendInterestRequest(sender: PublicProfile, target: PublicProfile): String {
        val uid = requireUserId()
        val requestId = UUID.randomUUID().toString()
        val userRef = db.collection("users").document(uid)
        val reqRef = db.collection("interestRequests").document(requestId)
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 7L * 24L * 3600L * 1000L))

        val reqPayload = mapOf(
            "fromUid" to uid,
            "toUid" to target.uid,
            "fromName" to sender.displayName.take(60),
            "fromPhoto" to (sender.photoDataList.firstOrNull() ?: ""),
            "fromLocation" to sender.publicLocationLabel.take(80),
            "toName" to target.displayName.take(60),
            "toPhoto" to (target.photoDataList.firstOrNull() ?: ""),
            "toLocation" to target.publicLocationLabel.take(80),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )

        try {
            db.runBatch { batch ->
                batch.set(reqRef, reqPayload)
                batch.update(
                    userRef,
                    mapOf(
                        "activeOutgoingRequestId" to requestId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            return requestId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqRef.path, auth)
            throw e
        }
    }

    suspend fun approveInterestRequest(request: InterestRequest, recipientProfile: PublicProfile) {
        val uid = requireUserId()
        require(request.toUid == uid) { "Only the recipient can approve an interest request." }
        val reqRef = db.collection("interestRequests").document(request.id)
        val pid = pairId(request.fromUid, request.toUid)
        val members = sortedPair(request.fromUid, request.toUid)
        val connRef = db.collection("connections").document(pid)
        val convRef = db.collection("conversations").document(pid)

        val userAIsSender = members[0] == request.fromUid
        val userAName = if (userAIsSender) request.fromName else recipientProfile.displayName
        val userAPhoto = if (userAIsSender) request.fromPhoto else (recipientProfile.photoDataList.firstOrNull() ?: "")
        val userBName = if (userAIsSender) recipientProfile.displayName else request.fromName
        val userBPhoto = if (userAIsSender) (recipientProfile.photoDataList.firstOrNull() ?: "") else request.fromPhoto

        try {
            db.runBatch { batch ->
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to "APPROVED",
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "decisionAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    connRef,
                    mapOf(
                        "memberUids" to members,
                        "sourceRequestId" to request.id,
                        "conversationId" to pid,
                        "status" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    convRef,
                    mapOf(
                        "memberUids" to members,
                        "userAId" to members[0],
                        "userAName" to userAName.take(60),
                        "userAPhoto" to userAPhoto,
                        "userBId" to members[1],
                        "userBName" to userBName.take(60),
                        "userBPhoto" to userBPhoto,
                        "connectionId" to pid,
                        "status" to "ACTIVE",
                        "lastMessageText" to "",
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
            throw e
        }
    }

    suspend fun declineInterestRequest(requestId: String) {
        val reqRef = db.collection("interestRequests").document(requestId)
        try {
            reqRef.update(
                mapOf(
                    "status" to "DECLINED",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "decisionAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
            throw e
        }
    }

    suspend fun withdrawInterestRequest(requestId: String) {
        val uid = requireUserId()
        val reqRef = db.collection("interestRequests").document(requestId)
        val userRef = db.collection("users").document(uid)
        try {
            db.runBatch { batch ->
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to "WITHDRAWN",
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "decisionAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    userRef,
                    mapOf(
                        "activeOutgoingRequestId" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
            throw e
        }
    }

    suspend fun clearResolvedOutgoingRequest() {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            userRef.update(
                mapOf(
                    "activeOutgoingRequestId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            throw e
        }
    }

    // --- CONNECTIONS & CONVERSATIONS ---
    fun observeConnections(): Flow<List<Connection>> = flow {
        val uid = requireUserId()
        val path = "connections"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Connection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ACTIVE" }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status != "BLOCKED" }
                        .sortedByDescending { it.lastMessageAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId"
        emitAll(
            db.document(path)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        conversation: Conversation,
        type: String,
        text: String = "",
        mediaData: String = "",
        audioDurationSec: Int = 0,
        waveform: List<Int> = emptyList()
    ) {
        val uid = requireUserId()
        val msgId = UUID.randomUUID().toString()
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)
        val members = conversation.memberUids

        val previewText = when (type) {
            "IMAGE" -> "📷 Photo"
            "VIDEO" -> "🎬 Video (${audioDurationSec}s)"
            "VOICE" -> "🎤 Voice note (${audioDurationSec}s)"
            else -> text.trim().take(200)
        }

        val msgPayload = mapOf(
            "senderUid" to uid,
            "memberUids" to members,
            "type" to type,
            "text" to text.trim().take(2000),
            "mediaData" to mediaData,
            "audioDurationSec" to audioDurationSec,
            "waveform" to waveform.take(40),
            "moderationStatus" to "active",
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runBatch { batch ->
                batch.set(msgRef, msgPayload)
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessageText" to previewText,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun deleteMessage(conversationId: String, message: ChatMessage) {
        val uid = requireUserId()
        require(message.senderUid == uid) { "You can only delete your own messages." }
        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document(message.id)
        try {
            msgRef.delete().await()
            val remaining = runCatching {
                convRef.collection("messages")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                    .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }.getOrNull()
            if (remaining != null) {
                val newest = remaining.firstOrNull()
                val newPreview = when (newest?.type) {
                    "IMAGE" -> "📷 Photo"
                    "VIDEO" -> "🎬 Video (${newest.audioDurationSec}s)"
                    "VOICE" -> "🎤 Voice note (${newest.audioDurationSec}s)"
                    "TEXT" -> newest.text.trim().take(200)
                    else -> ""
                }
                val newAt = newest?.createdAt ?: FieldValue.serverTimestamp()
                runCatching {
                    convRef.update(
                        mapOf(
                            "lastMessageText" to newPreview,
                            "lastMessageAt" to newAt,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun toggleConversationMute(conversationId: String, currentlyMuted: Boolean) {
        val convRef = db.collection("conversations").document(conversationId)
        try {
            convRef.update(
                mapOf(
                    "status" to if (currentlyMuted) "ACTIVE" else "MUTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    // --- SAFETY: BLOCKING & REPORTING ---
    fun observeBlockedUsers(): Flow<List<BlockRecord>> = flow {
        val uid = requireUserId()
        val path = "blocks"
        emitAll(
            db.collection(path)
                .whereEqualTo("blockerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BlockRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun blockUser(targetUid: String) {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUid}"
        val blockRef = db.collection("blocks").document(blockId)
        val pid = pairId(uid, targetUid)
        val connRef = db.collection("connections").document(pid)
        val convRef = db.collection("conversations").document(pid)

        try {
            val connSnap = runCatching { connRef.get().await() }.getOrNull()
            val convSnap = runCatching { convRef.get().await() }.getOrNull()
            db.runBatch { batch ->
                batch.set(
                    blockRef,
                    mapOf(
                        "blockerUid" to uid,
                        "blockedUid" to targetUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (connSnap != null && connSnap.exists()) {
                    batch.update(
                        connRef,
                        mapOf("status" to "BLOCKED", "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
                if (convSnap != null && convSnap.exists()) {
                    batch.update(
                        convRef,
                        mapOf("status" to "BLOCKED", "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, blockRef.path, auth)
            throw e
        }
    }

    suspend fun unblockUser(targetUid: String) {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUid}"
        val blockRef = db.collection("blocks").document(blockId)
        try {
            blockRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, blockRef.path, auth)
            throw e
        }
    }

    suspend fun submitReport(targetUid: String, category: String, details: String) {
        val uid = requireUserId()
        val reportId = UUID.randomUUID().toString()
        val reportRef = db.collection("reports").document(reportId)
        try {
            reportRef.set(
                mapOf(
                    "reporterUid" to uid,
                    "targetUid" to targetUid,
                    "category" to category,
                    "details" to details.trim().take(500),
                    "status" to "SUBMITTED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path, auth)
            throw e
        }
    }

    suspend fun deleteAccountData() {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("publicProfiles").document(uid)
        try {
            db.runBatch { batch ->
                batch.delete(publicRef)
                batch.delete(userRef)
            }.await()
            auth.currentUser?.delete()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userRef.path, auth)
            throw e
        }
    }

    // --- CULTURAL MOMENTS & THOUGHTS FEED (CENTER LOTUS HUB) ---
    fun observeCulturePosts(): Flow<List<CulturePost>> = flow {
        val uid = requireUserId()
        val path = "culturePosts"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CulturePost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createCulturePost(
        author: PublicProfile,
        thoughtText: String,
        imageData: String,
        culturalTag: String,
        postType: String = "THOUGHT",
        pollOptions: List<String> = emptyList(),
        scheduledAt: Timestamp? = null
    ) {
        val uid = requireUserId()
        val postId = UUID.randomUUID().toString()
        val ref = db.collection("culturePosts").document(postId)
        val cleanText = thoughtText.trim().take(600)
        val cleanOptions = pollOptions.map { it.trim().take(60) }.filter { it.isNotBlank() }.distinct().take(4)
        val normalizedType = if (postType == "POLL") "POLL" else "THOUGHT"
        if (normalizedType == "POLL") {
            require(cleanText.isNotBlank()) { "Ask a question for your quick poll." }
            require(cleanOptions.size >= 2) { "Add at least 2 poll options." }
        } else {
            require(cleanText.isNotBlank() || imageData.isNotBlank()) { "Add a thought or photo to post." }
        }
        val payload = hashMapOf<String, Any>(
            "authorUid" to uid,
            "authorName" to author.displayName.take(60).ifBlank { "Member" },
            "authorPhoto" to (author.photoDataList.firstOrNull() ?: ""),
            "authorLocation" to author.publicLocationLabel.take(80),
            "thoughtText" to cleanText,
            "imageData" to imageData,
            "culturalTag" to culturalTag.trim().take(60),
            "postType" to normalizedType,
            "pollOptions" to cleanOptions,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (scheduledAt != null) {
            payload["scheduledAt"] = scheduledAt
        }
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteCulturePost(postId: String) {
        val uid = requireUserId()
        val ref = db.collection("culturePosts").document(postId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    fun observePostReactions(postId: String): Flow<List<String>> = flow {
        val uid = requireUserId()
        val path = "culturePosts/$postId/reactions"
        emitAll(
            db.collection("culturePosts").document(postId).collection("reactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostReaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.userUid.ifBlank { it.id } }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun togglePostReaction(postId: String, currentlyLiked: Boolean) {
        val uid = requireUserId()
        val ref = db.collection("culturePosts").document(postId).collection("reactions").document(uid)
        try {
            if (currentlyLiked) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            throw e
        }
    }

    // --- OWNER SAVED POST NOTES ---
    fun observeSavedNotes(): Flow<List<SavedNote>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/savedNotes"
        emitAll(
            db.collection("users").document(uid).collection("savedNotes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveOrUpdateNote(
        existingNoteId: String?,
        title: String,
        noteText: String,
        sourcePostId: String = "",
        sourceAuthorName: String = "",
        sourceExcerpt: String = "",
        imageData: String = ""
    ) {
        val uid = requireUserId()
        val noteId = existingNoteId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val ref = db.collection("users").document(uid).collection("savedNotes").document(noteId)
        val cleanBody = noteText.trim().take(1000)
        require(cleanBody.isNotEmpty()) { "Note text cannot be empty." }
        try {
            if (existingNoteId.isNullOrBlank()) {
                ref.set(
                    mapOf(
                        "title" to title.trim().take(100),
                        "noteText" to cleanBody,
                        "sourcePostId" to sourcePostId.take(128),
                        "sourceAuthorName" to sourceAuthorName.trim().take(60),
                        "sourceExcerpt" to sourceExcerpt.trim().take(300),
                        "imageData" to imageData,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "title" to title.trim().take(100),
                        "noteText" to cleanBody,
                        "sourceExcerpt" to sourceExcerpt.trim().take(300),
                        "imageData" to imageData,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteSavedNote(noteId: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("savedNotes").document(noteId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // --- ONLINE / OFFLINE ACTIVITY PRESENCE ---
    suspend fun updateOnlinePresence(onlineNow: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val publicRef = db.collection("publicProfiles").document(uid)
        runCatching {
            publicRef.update(
                mapOf(
                    "onlineNow" to onlineNow,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    // --- INBUILT TO-DO LIST ---
    fun observeTodoItems(): Flow<List<TodoItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/todos"
        emitAll(
            db.collection("users").document(uid).collection("todos")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TodoItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<TodoItem> { it.completed }
                                .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveOrUpdateTodoItem(
        existingTodoId: String?,
        title: String,
        notes: String,
        category: String,
        priority: String,
        dueDateLabel: String,
        completed: Boolean = false
    ) {
        val uid = requireUserId()
        val todoId = existingTodoId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val ref = db.collection("users").document(uid).collection("todos").document(todoId)
        val cleanTitle = title.trim().take(140)
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty." }
        val normalizedPriority = when (priority.uppercase()) {
            "HIGH", "LOW", "MEDIUM" -> priority.uppercase()
            else -> "MEDIUM"
        }
        val cleanCategory = category.trim().take(40).ifBlank { "Personal" }
        try {
            if (existingTodoId.isNullOrBlank()) {
                ref.set(
                    mapOf(
                        "title" to cleanTitle,
                        "notes" to notes.trim().take(600),
                        "category" to cleanCategory,
                        "priority" to normalizedPriority,
                        "dueDateLabel" to dueDateLabel.trim().take(60),
                        "completed" to completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "title" to cleanTitle,
                        "notes" to notes.trim().take(600),
                        "category" to cleanCategory,
                        "priority" to normalizedPriority,
                        "dueDateLabel" to dueDateLabel.trim().take(60),
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            throw e
        }
    }

    suspend fun toggleTodoCompleted(todoId: String, completed: Boolean) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("todos").document(todoId)
        try {
            ref.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteTodoItem(todoId: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("todos").document(todoId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // --- POLL VOTES ---
    fun observePollVotes(postId: String): Flow<List<PollVote>> = flow {
        val uid = requireUserId()
        val path = "culturePosts/$postId/pollVotes"
        emitAll(
            db.collection("culturePosts").document(postId).collection("pollVotes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PollVote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun voteOnPoll(postId: String, optionIndex: Int) {
        val uid = requireUserId()
        require(optionIndex in 0..3) { "Invalid poll option index." }
        val ref = db.collection("culturePosts").document(postId).collection("pollVotes").document(uid)
        try {
            ref.set(
                mapOf(
                    "userUid" to uid,
                    "optionIndex" to optionIndex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            throw e
        }
    }

    // --- SECRET INVITE CODES ---
    fun observeMyInviteCode(): Flow<InviteCodeRecord?> = flow {
        val uid = requireUserId()
        val path = "inviteCodes"
        emitAll(
            db.collection(path)
                .whereEqualTo("creatorUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(InviteCodeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.active }
                        .maxByOrNull { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun generateOrRotateInviteCode(profile: PublicProfile): String {
        val uid = requireUserId()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val newCode = "MM" + (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
        val coll = db.collection("inviteCodes")
        try {
            val existing = coll.whereEqualTo("creatorUid", uid).get().await()
            val newRef = coll.document(newCode)
            val payload = mapOf(
                "code" to newCode,
                "creatorUid" to uid,
                "creatorName" to profile.displayName.take(60).ifBlank { "Member" },
                "creatorPhoto" to (profile.photoDataList.firstOrNull() ?: ""),
                "creatorLocation" to profile.publicLocationLabel.take(80),
                "creatorGender" to profile.gender,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.runBatch { batch ->
                existing.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.set(newRef, payload)
            }.await()
            return newCode
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "inviteCodes/$newCode", auth)
            throw e
        }
    }

    suspend fun redeemInviteCodeAndSendRequest(
        rawCode: String,
        sender: PublicProfile
    ): PublicProfile {
        val uid = requireUserId()
        val cleanCode = rawCode.trim().uppercase().replace("[^A-Z0-9]".toRegex(), "")
        require(cleanCode.length in 6..16) { "Enter a valid secret invite code (e.g. MM8K4P2X)." }
        val docRef = db.collection("inviteCodes").document(cleanCode)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                throw IllegalArgumentException("Secret invite code not found. Double-check with your favorite person.")
            }
            val record = snap.toObject(InviteCodeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Invite code could not be read.")
            require(record.active) { "This invite code is no longer active." }
            require(record.creatorUid != uid) { "You cannot redeem your own secret invite code." }
            require(record.creatorGender != sender.gender) {
                "Eligibility rule allows interest requests with members of the opposite gender."
            }
            val targetProfile = getPublicProfile(record.creatorUid)
                ?: throw IllegalStateException("This profile is paused or unavailable right now.")
            sendInterestRequest(sender, targetProfile)
            return targetProfile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            }
            throw e
        }
    }

    // --- REAL-TIME LOCATION & PRIVACY CONTROLS ---
    suspend fun updateLocationAndPrivacy(
        locationSharingEnabled: Boolean,
        coarseLat: Double,
        coarseLng: Double,
        province: String,
        district: String,
        publicLocationLabel: String
    ) {
        val uid = requireUserId()
        val publicRef = db.collection("publicProfiles").document(uid)
        val safeLat = if (locationSharingEnabled) LocationServiceHelper.roundToCoarsePrivacyGrid(coarseLat).coerceIn(-90.0, 90.0) else 0.0
        val safeLng = if (locationSharingEnabled) LocationServiceHelper.roundToCoarsePrivacyGrid(coarseLng).coerceIn(-180.0, 180.0) else 0.0
        val updates = mapOf(
            "locationSharingEnabled" to locationSharingEnabled,
            "approxLat" to safeLat,
            "approxLng" to safeLng,
            "province" to province.trim().take(60).ifBlank { "Bagmati" },
            "district" to district.trim().take(60).ifBlank { "Kathmandu" },
            "publicLocationLabel" to publicLocationLabel.trim().take(80).ifBlank { "Kathmandu, Nepal" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            publicRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, publicRef.path, auth)
            throw e
        }
    }

    // --- NEPALI VIBE CHECK, NEARBY ACTIVITY MATCH & DAILY QUESTION ---
    suspend fun updateVibeActivitiesAndQuestion(
        currentVibeId: String? = null,
        favoriteActivities: List<String>? = null,
        dailyQuestionAnswer: String? = null
    ) {
        val uid = requireUserId()
        val publicRef = db.collection("publicProfiles").document(uid)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (currentVibeId != null) {
            updates["currentVibeId"] = currentVibeId.trim().take(40)
        }
        if (favoriteActivities != null) {
            updates["favoriteActivities"] = favoriteActivities.map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(10)
        }
        if (dailyQuestionAnswer != null) {
            updates["dailyQuestionAnswer"] = dailyQuestionAnswer.trim().take(240)
        }
        try {
            publicRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, publicRef.path, auth)
            throw e
        }
    }

    // --- MAYALU AI HANGOUT PLANNER SAVED PLANS ---
    fun observeSavedHangoutPlans(): Flow<List<SavedHangoutPlan>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/hangoutPlans"
        emitAll(
            db.collection("users").document(uid).collection("hangoutPlans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedHangoutPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveHangoutPlan(itinerary: HangoutItinerary) {
        val uid = requireUserId()
        val planId = UUID.randomUUID().toString()
        val ref = db.collection("users").document(uid).collection("hangoutPlans").document(planId)
        val stopsSummary = itinerary.stops.take(10).map { stop ->
            "${stop.stepNumber}. ${stop.placeName} (${stop.timeSlot}) - NPR ${stop.estimatedCostNpr} | ${stop.foodSuggestion}"
                .take(220)
        }
        val avoidSummary = itinerary.placesToAvoid.take(6).map { av ->
            "${av.placeOrArea}: ${av.reason}".take(220)
        }
        val safetySummary = itinerary.safetyPrecautions.take(8).map { it.take(200) }
        val payload = mapOf(
            "title" to itinerary.title.trim().take(120).ifBlank { "Mayalu Hangout Plan" },
            "city" to itinerary.city.trim().take(60).ifBlank { "Kathmandu" },
            "startingLocation" to itinerary.startingLocation.trim().take(120),
            "durationLabel" to itinerary.durationLabel.trim().take(60),
            "groupSize" to itinerary.groupSize.coerceIn(1, 20),
            "totalBudgetNpr" to itinerary.totalEstimatedBudgetNpr.coerceIn(100, 200000),
            "summaryNotes" to itinerary.weatherAndTimingNote.trim().take(1000),
            "stopsSummary" to stopsSummary,
            "avoidSummary" to avoidSummary,
            "safetySummary" to safetySummary,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteHangoutPlan(planId: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("hangoutPlans").document(planId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // --- VIRTUAL MEETUP PLANNER INVITATIONS ---
    fun observeMeetupInvites(): Flow<List<MeetupInvitation>> = flow {
        val uid = requireUserId()
        val path = "meetupInvites"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MeetupInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendMeetupInvitation(
        conversationId: String,
        senderName: String,
        recipientUid: String,
        recipientName: String,
        planTitle: String,
        city: String,
        proposedDateLabel: String,
        estimatedBudgetNpr: Int,
        stopsPreview: List<String>
    ) {
        val uid = requireUserId()
        val inviteId = UUID.randomUUID().toString()
        val ref = db.collection("meetupInvites").document(inviteId)
        val members = sortedPair(uid, recipientUid)
        val payload = mapOf(
            "conversationId" to conversationId.ifBlank { pairId(uid, recipientUid) },
            "senderUid" to uid,
            "senderName" to senderName.trim().take(60).ifBlank { "Member" },
            "recipientUid" to recipientUid,
            "recipientName" to recipientName.trim().take(60).ifBlank { "Member" },
            "memberUids" to members,
            "planTitle" to planTitle.trim().take(120).ifBlank { "Heritage Meetup" },
            "city" to city.trim().take(60).ifBlank { "Kathmandu" },
            "proposedDateLabel" to proposedDateLabel.trim().take(80).ifBlank { "This Saturday Afternoon" },
            "estimatedBudgetNpr" to estimatedBudgetNpr.coerceIn(100, 200000),
            "stopsPreview" to stopsPreview.map { it.take(140) }.take(8),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun respondToMeetupInvitation(inviteId: String, accepted: Boolean) {
        requireUserId()
        val ref = db.collection("meetupInvites").document(inviteId)
        try {
            ref.update(
                mapOf(
                    "status" to if (accepted) "ACCEPTED" else "DECLINED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    // --- TOGETHER MEMORIES (SHARED ALBUM IN MUTUAL CONNECTIONS) ---
    fun observeTogetherMemories(conversationId: String): Flow<List<TogetherMemory>> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId/memories"
        emitAll(
            db.collection("conversations").document(conversationId).collection("memories")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TogetherMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addTogetherMemory(
        conversationId: String,
        authorName: String,
        memberUids: List<String>,
        title: String,
        noteText: String,
        imageData: String = "",
        milestoneTag: String = "Special Moment"
    ) {
        val uid = requireUserId()
        val memId = UUID.randomUUID().toString()
        val ref = db.collection("conversations").document(conversationId).collection("memories").document(memId)
        val payload = mapOf(
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifBlank { "Member" },
            "memberUids" to memberUids,
            "title" to title.trim().take(100).ifBlank { "Together Memory" },
            "noteText" to noteText.trim().take(600),
            "imageData" to imageData,
            "milestoneTag" to milestoneTag.trim().take(60),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun deleteTogetherMemory(conversationId: String, memoryId: String) {
        requireUserId()
        val ref = db.collection("conversations").document(conversationId).collection("memories").document(memoryId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            throw e
        }
    }

    // --- SECRET COMPLIMENTS ---
    fun observeReceivedCompliments(): Flow<List<SecretCompliment>> = flow {
        val uid = requireUserId()
        val path = "compliments"
        emitAll(
            db.collection(path)
                .whereEqualTo("recipientUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SecretCompliment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendSecretCompliment(
        senderProfile: PublicProfile,
        recipientUid: String,
        complimentKey: String,
        complimentText: String
    ) {
        val uid = requireUserId()
        require(recipientUid != uid) { "Cannot send a compliment to yourself." }
        val compId = UUID.randomUUID().toString()
        val ref = db.collection("compliments").document(compId)
        val hint = buildString {
            append("Someone from ${senderProfile.district}")
            if (senderProfile.culturalBadge.isNotBlank()) {
                append(" · ${senderProfile.culturalBadge}")
            }
        }.take(80)
        val payload = mapOf(
            "senderUid" to uid,
            "senderHint" to hint,
            "recipientUid" to recipientUid,
            "complimentKey" to complimentKey.trim().take(40).ifBlank { "kind_energy" },
            "complimentText" to complimentText.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    // --- MUTUAL-CONSENT 1-TO-1 AUDIO CALLING ---
    fun observeCallSessions(): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        val path = "callSessions"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun startAudioCall(
        conversationId: String,
        callerName: String,
        callerPhoto: String,
        calleeUid: String,
        calleeName: String,
        calleePhoto: String
    ): String {
        val uid = requireUserId()
        require(calleeUid.isNotBlank() && calleeUid != uid) { "Invalid call recipient." }
        val callId = UUID.randomUUID().toString()
        val ref = db.collection("callSessions").document(callId)
        val members = sortedPair(uid, calleeUid)
        val payload = mapOf(
            "conversationId" to conversationId.ifBlank { pairId(uid, calleeUid) },
            "callerUid" to uid,
            "callerName" to callerName.trim().take(60).ifBlank { "Member" },
            "callerPhoto" to callerPhoto,
            "calleeUid" to calleeUid,
            "calleeName" to calleeName.trim().take(60).ifBlank { "Member" },
            "calleePhoto" to calleePhoto,
            "memberUids" to members,
            "status" to "RINGING",
            "callerAudioChunk" to "",
            "calleeAudioChunk" to "",
            "callerSeq" to 0,
            "calleeSeq" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
            return callId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            throw e
        }
    }

    suspend fun updateCallStatus(callId: String, newStatus: String) {
        requireUserId()
        val ref = db.collection("callSessions").document(callId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newStatus != "ACCEPTED") {
            updates["callerAudioChunk"] = ""
            updates["calleeAudioChunk"] = ""
        }
        try {
            ref.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            throw e
        }
    }

    suspend fun transmitAcceptedCallAudioChunk(
        callSession: CallSession,
        base64AudioChunk: String,
        nextSeq: Int
    ) {
        val uid = requireUserId()
        // CRITICAL MUTUAL-CONSENT GUARD: Audio is NEVER transmitted unless status == ACCEPTED
        if (callSession.status != "ACCEPTED" || base64AudioChunk.isBlank()) return
        val ref = db.collection("callSessions").document(callSession.id)
        val isCaller = callSession.callerUid == uid
        val updates = if (isCaller) {
            mapOf(
                "callerAudioChunk" to base64AudioChunk.take(64000),
                "callerSeq" to nextSeq,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "calleeAudioChunk" to base64AudioChunk.take(64000),
                "calleeSeq" to nextSeq,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        runCatching { ref.update(updates).await() }
    }

    private fun logFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, operationType, path, auth)
        }
    }
}
