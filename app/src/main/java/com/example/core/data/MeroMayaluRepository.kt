package com.example.core.data

import android.content.Context
import com.example.R
import com.example.core.domain.BlockRecord
import com.example.core.domain.ChatMessage
import com.example.core.domain.Connection
import com.example.core.domain.Conversation
import com.example.core.domain.CulturePost
import com.example.core.domain.InterestRequest
import com.example.core.domain.PostReaction
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.example.core.domain.ReportRecord
import com.example.core.domain.SavedNote
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
        require(publicProfile.photoDataList.isNotEmpty()) { "At least one profile photo is required." }

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
                    "photoDataList" to publicProfile.photoDataList.take(4),
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
                    "visibility" to privateUser.visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val publicUpdates = mapOf(
                    "displayName" to publicProfile.displayName.trim(),
                    "bio" to publicProfile.bio.trim(),
                    "lookingFor" to publicProfile.lookingFor.trim(),
                    "photoDataList" to publicProfile.photoDataList.take(4),
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
            if (visibility != null) {
                db.collection("publicProfiles").document(uid).update(
                    mapOf(
                        "visibility" to visibility,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
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
        culturalTag: String
    ) {
        val uid = requireUserId()
        val postId = UUID.randomUUID().toString()
        val ref = db.collection("culturePosts").document(postId)
        val cleanText = thoughtText.trim().take(600)
        require(cleanText.isNotBlank() || imageData.isNotBlank()) { "Add a thought or photo to post." }
        val payload = mapOf(
            "authorUid" to uid,
            "authorName" to author.displayName.take(60).ifBlank { "Member" },
            "authorPhoto" to (author.photoDataList.firstOrNull() ?: ""),
            "authorLocation" to author.publicLocationLabel.take(80),
            "thoughtText" to cleanText,
            "imageData" to imageData,
            "culturalTag" to culturalTag.trim().take(60),
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
