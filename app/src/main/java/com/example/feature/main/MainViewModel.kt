package com.example.feature.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.LocalAppPreferences
import com.example.core.data.MeroMayaluRepository
import com.example.core.data.PreferencesRepository
import com.example.core.domain.BlockRecord
import com.example.core.domain.ChatMessage
import com.example.core.domain.Connection
import com.example.core.domain.Conversation
import com.example.core.domain.CulturePost
import com.example.core.domain.HeritagePlacesResult
import com.example.core.domain.InterestRequest
import com.example.core.domain.InviteCodeRecord
import com.example.core.domain.PollVote
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.example.core.domain.SavedNote
import com.example.core.domain.UiState
import com.example.core.firebase.MapsGroundingService
import com.example.core.localization.AppLanguage
import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.designsystem.MeroThemeMode
import com.example.core.util.DataExportHelper
import com.example.core.util.ExportUserData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DiscoverySubTab { NEARBY, FOR_YOU, HERITAGE }

sealed interface MeroPushEvent {
    data class NewIncomingRequest(val senderName: String) : MeroPushEvent
    data class NewMessage(val senderName: String, val preview: String) : MeroPushEvent
}

data class MainSessionUi(
    val isLoadingProfile: Boolean = true,
    val privateUser: PrivateUser? = null,
    val publicProfile: PublicProfile? = null,
    val error: String? = null
)

class MainViewModel(
    private val repository: MeroMayaluRepository,
    private val prefsRepository: PreferencesRepository,
    private val mapsService: MapsGroundingService,
    val currentUserId: String
) : ViewModel() {

    private val _session = MutableStateFlow(MainSessionUi())
    val session: StateFlow<MainSessionUi> = _session.asStateFlow()

    val localPrefs: StateFlow<LocalAppPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPreferences())

    private val _rawDiscovery = MutableStateFlow<UiState<List<PublicProfile>>>(UiState.Loading)
    private val _dismissedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _blockedUsers = MutableStateFlow<List<BlockRecord>>(emptyList())
    val blockedUsers: StateFlow<List<BlockRecord>> = _blockedUsers.asStateFlow()

    private val _connections = MutableStateFlow<List<Connection>>(emptyList())
    val connections: StateFlow<List<Connection>> = _connections.asStateFlow()

    val incomingRequests: StateFlow<UiState<List<InterestRequest>>> = repository.observeIncomingRequests()
        .map<List<InterestRequest>, UiState<List<InterestRequest>>> { UiState.Success(it) }
        .catch { err -> emit(UiState.Error(extractReadableError(err))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val outgoingRequests: StateFlow<UiState<List<InterestRequest>>> = repository.observeOutgoingRequests()
        .map<List<InterestRequest>, UiState<List<InterestRequest>>> { UiState.Success(it) }
        .catch { err -> emit(UiState.Error(extractReadableError(err))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val conversations: StateFlow<UiState<List<Conversation>>> = repository.observeConversations()
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { err -> emit(UiState.Error(extractReadableError(err))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val culturePosts: StateFlow<UiState<List<CulturePost>>> = combine(
        repository.observeCulturePosts()
            .map<List<CulturePost>, UiState<List<CulturePost>>> { UiState.Success(it) }
            .catch { err -> emit(UiState.Error(extractReadableError(err))) },
        _blockedUsers
    ) { state, blocked ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val nowMs = System.currentTimeMillis()
                val blockedSet = blocked.map { it.blockedUid }.toSet()
                UiState.Success(
                    state.data.filter { post ->
                        post.authorUid !in blockedSet &&
                            (post.authorUid == currentUserId ||
                                post.scheduledAt == null ||
                                post.scheduledAt.toDate().time <= nowMs)
                    }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedNotes: StateFlow<UiState<List<SavedNote>>> = repository.observeSavedNotes()
        .map<List<SavedNote>, UiState<List<SavedNote>>> { UiState.Success(it) }
        .catch { err -> emit(UiState.Error(extractReadableError(err))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myInviteCode: StateFlow<InviteCodeRecord?> = repository.observeMyInviteCode()
        .catch { err -> Log.w(TAG, "invite code flow error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _discoverySubTab = MutableStateFlow(DiscoverySubTab.FOR_YOU)
    val discoverySubTab: StateFlow<DiscoverySubTab> = _discoverySubTab.asStateFlow()

    val filteredDiscovery: StateFlow<UiState<List<PublicProfile>>> = combine(
        _rawDiscovery,
        _dismissedIds,
        _blockedUsers,
        _connections,
        _discoverySubTab,
        _session
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val raw = values[0] as UiState<List<PublicProfile>>
        @Suppress("UNCHECKED_CAST")
        val dismissed = values[1] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val blocked = values[2] as List<BlockRecord>
        @Suppress("UNCHECKED_CAST")
        val conns = values[3] as List<Connection>
        val tab = values[4] as DiscoverySubTab
        val sessionUi = values[5] as MainSessionUi

        when (raw) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> raw
            is UiState.Success -> {
                val blockedSet = blocked.map { it.blockedUid }.toSet()
                val connectedSet = conns.flatMap { it.memberUids }.filter { it != currentUserId }.toSet()
                val myProfile = sessionUi.publicProfile
                val eligible = raw.data.filter { p ->
                    p.uid != currentUserId &&
                        p.uid !in dismissed &&
                        p.uid !in blockedSet &&
                        p.uid !in connectedSet
                }
                val ranked = when (tab) {
                    DiscoverySubTab.NEARBY -> {
                        val sameDistrict = eligible.filter {
                            myProfile != null && it.district.equals(myProfile.district, ignoreCase = true)
                        }
                        if (sameDistrict.isNotEmpty()) sameDistrict else eligible.filter {
                            myProfile != null && it.province.equals(myProfile.province, ignoreCase = true)
                        }
                    }
                    DiscoverySubTab.FOR_YOU -> {
                        val myInterests = myProfile?.interests?.toSet().orEmpty()
                        eligible.sortedByDescending { it.interests.count { tag -> tag in myInterests } }
                    }
                    DiscoverySubTab.HERITAGE -> {
                        eligible.sortedByDescending {
                            (if (it.culturalBadge.isNotBlank()) 2 else 0) + it.prompts.size
                        }
                    }
                }
                UiState.Success(ranked)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _interestSentTarget = MutableStateFlow<PublicProfile?>(null)
    val interestSentTarget: StateFlow<PublicProfile?> = _interestSentTarget.asStateFlow()

    private val _celebrationPair = MutableStateFlow<Pair<String, String>?>(null) // name, conversationId
    val celebrationPair: StateFlow<Pair<String, String>?> = _celebrationPair.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _undoDismissTargetUid = MutableStateFlow<String?>(null)
    val undoDismissTargetUid: StateFlow<String?> = _undoDismissTargetUid.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _culturePlacesState = MutableStateFlow<UiState<HeritagePlacesResult>?>(null)
    val culturePlacesState: StateFlow<UiState<HeritagePlacesResult>?> = _culturePlacesState.asStateFlow()

    private val _pushEvents = MutableSharedFlow<MeroPushEvent>(extraBufferCapacity = 8)
    val pushEvents: SharedFlow<MeroPushEvent> = _pushEvents.asSharedFlow()

    private var discoveryJob: Job? = null
    private var observedGender: String? = null
    private var toastJob: Job? = null

    init {
        startObservingSession()
        startObservingSafetyAndConnections()
        startObservingPushEvents()
    }

    private fun startObservingSession() {
        viewModelScope.launch {
            combine(
                repository.observePrivateUser(currentUserId),
                repository.observePublicProfile(currentUserId)
            ) { priv, pub -> priv to pub }
                .catch { err ->
                    _session.value = MainSessionUi(
                        isLoadingProfile = false,
                        error = extractReadableError(err)
                    )
                }
                .collect { (priv, pub) ->
                    _session.value = MainSessionUi(
                        isLoadingProfile = false,
                        privateUser = priv,
                        publicProfile = pub,
                        error = null
                    )
                    if (priv != null) {
                        syncRemotePrefsToLocal(priv)
                        if (observedGender != priv.gender) {
                            startDiscoveryForGender(priv.gender)
                        }
                    }
                }
        }
    }

    private fun syncRemotePrefsToLocal(priv: PrivateUser) {
        viewModelScope.launch {
            val current = localPrefs.value
            val remoteLang = AppLanguage.fromCode(priv.language)
            val remoteTheme = MeroThemeCatalog.fromId(priv.theme)
            val remoteFont = MeroFontCatalog.fromId(priv.fontStyle)
            val remoteMode = MeroThemeMode.fromId(priv.themeMode)
            if (current.language != remoteLang) prefsRepository.setLanguage(remoteLang)
            if (current.themeCatalog != remoteTheme) prefsRepository.setThemeCatalog(remoteTheme)
            if (current.fontCatalog != remoteFont) prefsRepository.setFontCatalog(remoteFont)
            if (current.themeMode != remoteMode) prefsRepository.setThemeMode(remoteMode)
            if (current.useBikramSambat != priv.useBikramSambat) {
                prefsRepository.setUseBikramSambat(priv.useBikramSambat)
            }
            if (current.reducedMotion != priv.reducedMotion) {
                prefsRepository.setReducedMotion(priv.reducedMotion)
            }
        }
    }

    private fun startDiscoveryForGender(gender: String) {
        observedGender = gender
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _rawDiscovery.value = UiState.Loading
            repository.observeDiscoveryFeed(gender)
                .catch { err -> _rawDiscovery.value = UiState.Error(extractReadableError(err)) }
                .collect { list -> _rawDiscovery.value = UiState.Success(list) }
        }
    }

    private fun startObservingSafetyAndConnections() {
        viewModelScope.launch {
            repository.observeDismissedIds()
                .catch { err -> Log.w(TAG, "dismissed flow error: ${err.message}") }
                .collect { _dismissedIds.value = it }
        }
        viewModelScope.launch {
            repository.observeBlockedUsers()
                .catch { err -> Log.w(TAG, "blocked flow error: ${err.message}") }
                .collect { _blockedUsers.value = it }
        }
        viewModelScope.launch {
            repository.observeConnections()
                .catch { err -> Log.w(TAG, "connections flow error: ${err.message}") }
                .collect { _connections.value = it }
        }
    }

    private fun startObservingPushEvents() {
        viewModelScope.launch {
            var initialized = false
            val seenIds = mutableSetOf<String>()
            incomingRequests.collect { state ->
                if (state is UiState.Success) {
                    val pending = state.data.filter { it.status == "PENDING" }
                    if (!initialized) {
                        seenIds.addAll(pending.map { it.id })
                        initialized = true
                    } else {
                        pending.filter { it.id !in seenIds }.forEach { req ->
                            seenIds.add(req.id)
                            _pushEvents.tryEmit(MeroPushEvent.NewIncomingRequest(req.fromName))
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var initialized = false
            val lastTimestamps = mutableMapOf<String, Long>()
            conversations.collect { state ->
                if (state is UiState.Success) {
                    if (!initialized) {
                        state.data.forEach { conv ->
                            lastTimestamps[conv.id] = conv.lastMessageAt?.toDate()?.time ?: 0L
                        }
                        initialized = true
                    } else {
                        state.data.forEach { conv ->
                            val ts = conv.lastMessageAt?.toDate()?.time ?: 0L
                            val prev = lastTimestamps[conv.id] ?: 0L
                            if (ts > prev && conv.lastMessageText.isNotBlank() && conv.status != "MUTED") {
                                lastTimestamps[conv.id] = ts
                                val partnerName = if (conv.userAId == currentUserId) conv.userBName else conv.userAName
                                _pushEvents.tryEmit(MeroPushEvent.NewMessage(partnerName, conv.lastMessageText))
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectDiscoverySubTab(tab: DiscoverySubTab) {
        _discoverySubTab.value = tab
    }

    fun clearInterestSentOverlay() {
        _interestSentTarget.value = null
    }

    fun clearCelebration() {
        _celebrationPair.value = null
    }

    fun showToast(message: String, undoTargetUid: String? = null) {
        toastJob?.cancel()
        _toastMessage.value = message
        _undoDismissTargetUid.value = undoTargetUid
        toastJob = viewModelScope.launch {
            delay(4000L)
            _toastMessage.value = null
            _undoDismissTargetUid.value = null
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _toastMessage.value = null
        _undoDismissTargetUid.value = null
    }

    fun retryDiscovery() {
        val g = _session.value.privateUser?.gender ?: return
        observedGender = null
        startDiscoveryForGender(g)
    }

    fun saveProfile(
        privateUser: PrivateUser,
        publicProfile: PublicProfile,
        isInitialCreate: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.saveProfile(privateUser, publicProfile, isInitialCreate)
            }.onSuccess {
                onComplete()
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun sendInterest(target: PublicProfile, activeOutgoingErrorMsg: String) {
        val myPublic = _session.value.publicProfile ?: return
        val outgoing = (outgoingRequests.value as? UiState.Success)?.data.orEmpty()
        if (outgoing.any { it.status == "PENDING" }) {
            showToast(activeOutgoingErrorMsg)
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                val activeId = _session.value.privateUser?.activeOutgoingRequestId.orEmpty()
                if (activeId.isNotBlank()) {
                    repository.clearResolvedOutgoingRequest()
                }
                repository.sendInterestRequest(myPublic, target)
            }.onSuccess {
                _interestSentTarget.value = target
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun dismissProfile(targetUid: String, toastText: String) {
        viewModelScope.launch {
            runCatching {
                repository.dismissProfile(targetUid)
            }.onSuccess {
                showToast(toastText, undoTargetUid = targetUid)
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun undoDismiss() {
        val target = _undoDismissTargetUid.value ?: return
        dismissToast()
        viewModelScope.launch {
            runCatching { repository.undoDismissProfile(target) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun approveIncomingRequest(request: InterestRequest) {
        val myPublic = _session.value.publicProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.approveInterestRequest(request, myPublic)
            }.onSuccess {
                val convId = repository.pairId(request.fromUid, request.toUid)
                _celebrationPair.value = request.fromName to convId
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun declineIncomingRequest(requestId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.declineInterestRequest(requestId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun withdrawOutgoingRequest(requestId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.withdrawInterestRequest(requestId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun observeConversation(conversationId: String) = repository.observeConversation(conversationId)
    fun observeMessages(conversationId: String) = repository.observeMessages(conversationId)

    fun sendChatMessage(
        conversation: Conversation,
        type: String,
        text: String = "",
        mediaData: String = "",
        audioDurationSec: Int = 0,
        waveform: List<Int> = emptyList(),
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.sendMessage(conversation, type, text, mediaData, audioDurationSec, waveform)
            }.onSuccess {
                onDone()
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun deleteChatMessage(conversationId: String, message: ChatMessage, successToast: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMessage(conversationId, message) }
                .onSuccess { showToast(successToast) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun toggleMuteConversation(conversationId: String, currentlyMuted: Boolean) {
        viewModelScope.launch {
            runCatching { repository.toggleConversationMute(conversationId, currentlyMuted) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun blockUser(targetUid: String, confirmationMsg: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.blockUser(targetUid) }
                .onSuccess { showToast(confirmationMsg) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            runCatching { repository.unblockUser(targetUid) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun submitReport(targetUid: String, category: String, details: String, confirmationMsg: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.submitReport(targetUid, category, details) }
                .onSuccess { showToast(confirmationMsg) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepository.setLanguage(language)
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf("language" to language.code)) }
            }
        }
    }

    fun updateThemeCatalog(catalog: MeroThemeCatalog) {
        viewModelScope.launch {
            prefsRepository.setThemeCatalog(catalog)
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf("theme" to catalog.id)) }
            }
        }
    }

    fun updateFontCatalog(font: MeroFontCatalog) {
        viewModelScope.launch {
            prefsRepository.setFontCatalog(font)
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf("fontStyle" to font.id)) }
            }
        }
    }

    fun updateThemeMode(mode: MeroThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf("themeMode" to mode.id)) }
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setAppLockEnabled(enabled)
        }
    }

    fun updateBooleanPreference(field: String, value: Boolean) {
        viewModelScope.launch {
            when (field) {
                "useBikramSambat" -> prefsRepository.setUseBikramSambat(value)
                "reducedMotion" -> prefsRepository.setReducedMotion(value)
            }
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf(field to value)) }
                    .onFailure { err -> showToast(extractReadableError(err)) }
            }
        }
    }

    fun updateVisibility(visible: Boolean) {
        val status = if (visible) "visible" else "hidden"
        viewModelScope.launch {
            runCatching { repository.updatePreferences(mapOf("visibility" to status)) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun updateBannerData(bannerData: String, successToast: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.updateBannerData(bannerData) }
                .onSuccess { showToast(successToast) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun updateProfilePhotos(
        photoDataList: List<String>,
        successToast: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.updateProfilePhotos(photoDataList) }
                .onSuccess {
                    showToast(successToast)
                    onDone()
                }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun addCustomInterest(rawInterest: String, successToast: String) {
        val clean = rawInterest.trim().take(32)
        if (clean.isBlank()) return
        val current = _session.value.publicProfile?.interests.orEmpty()
        if (current.any { it.equals(clean, ignoreCase = true) }) return
        viewModelScope.launch {
            runCatching { repository.updateInterests((current + clean).distinct().take(16)) }
                .onSuccess { showToast(successToast) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun removeInterest(interest: String) {
        val current = _session.value.publicProfile?.interests.orEmpty()
        viewModelScope.launch {
            runCatching { repository.updateInterests(current.filterNot { it == interest }) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun observePostReactions(postId: String): Flow<List<String>> =
        repository.observePostReactions(postId)

    fun togglePostReaction(postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            runCatching { repository.togglePostReaction(postId, currentlyLiked) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun createCulturePost(
        thoughtText: String,
        imageData: String,
        culturalTag: String,
        postType: String = "THOUGHT",
        pollOptions: List<String> = emptyList(),
        scheduledAt: Timestamp? = null,
        successToast: String,
        onDone: () -> Unit = {}
    ) {
        val author = _session.value.publicProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.createCulturePost(
                    author = author,
                    thoughtText = thoughtText,
                    imageData = imageData,
                    culturalTag = culturalTag,
                    postType = postType,
                    pollOptions = pollOptions,
                    scheduledAt = scheduledAt
                )
            }.onSuccess {
                showToast(successToast)
                onDone()
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun observePollVotes(postId: String): Flow<List<PollVote>> =
        repository.observePollVotes(postId)

    fun voteOnPoll(postId: String, optionIndex: Int) {
        viewModelScope.launch {
            runCatching { repository.voteOnPoll(postId, optionIndex) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun generateOrRotateInviteCode(successToast: String) {
        val myPublic = _session.value.publicProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.generateOrRotateInviteCode(myPublic) }
                .onSuccess { code -> showToast("$successToast: $code") }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun redeemInviteCode(
        rawCode: String,
        activeOutgoingErrorMsg: String,
        onSuccessDone: () -> Unit = {}
    ) {
        val myPublic = _session.value.publicProfile ?: return
        val outgoing = (outgoingRequests.value as? UiState.Success)?.data.orEmpty()
        if (outgoing.any { it.status == "PENDING" }) {
            showToast(activeOutgoingErrorMsg)
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                val activeId = _session.value.privateUser?.activeOutgoingRequestId.orEmpty()
                if (activeId.isNotBlank()) {
                    repository.clearResolvedOutgoingRequest()
                }
                repository.redeemInviteCodeAndSendRequest(rawCode, myPublic)
            }.onSuccess { target ->
                onSuccessDone()
                _interestSentTarget.value = target
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun deleteCulturePost(postId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteCulturePost(postId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun saveOrUpdateNote(
        existingNoteId: String?,
        title: String,
        noteText: String,
        sourcePostId: String = "",
        sourceAuthorName: String = "",
        sourceExcerpt: String = "",
        imageData: String = "",
        successToast: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.saveOrUpdateNote(
                    existingNoteId = existingNoteId,
                    title = title,
                    noteText = noteText,
                    sourcePostId = sourcePostId,
                    sourceAuthorName = sourceAuthorName,
                    sourceExcerpt = sourceExcerpt,
                    imageData = imageData
                )
            }.onSuccess {
                showToast(successToast)
                onDone()
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun deleteSavedNote(noteId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteSavedNote(noteId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun searchHeritagePlaces(query: String, isNepali: Boolean) {
        val profile = _session.value.publicProfile
        val cityHint = profile?.district?.takeIf { it.isNotBlank() } ?: "Kathmandu Valley"
        val coords = coordsForDistrict(cityHint)
        viewModelScope.launch {
            _culturePlacesState.value = UiState.Loading
            val res = mapsService.discoverNepaliPlaces(
                query = query,
                cityHint = cityHint,
                latitude = coords.first,
                longitude = coords.second,
                isNepali = isNepali
            )
            _culturePlacesState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Place search failed.") }
            )
        }
    }

    fun buildExportData(): ExportUserData {
        val priv = _session.value.privateUser
        val pub = _session.value.publicProfile
        val outList = (outgoingRequests.value as? UiState.Success)?.data.orEmpty()
        val inList = (incomingRequests.value as? UiState.Success)?.data.orEmpty()
        val convList = (conversations.value as? UiState.Success)?.data.orEmpty()
        val notes = (savedNotes.value as? UiState.Success)?.data.orEmpty()
        return ExportUserData(
            uid = priv?.uid ?: currentUserId,
            displayName = priv?.displayName ?: pub?.displayName.orEmpty(),
            gender = priv?.gender.orEmpty(),
            age = priv?.age ?: 0,
            dateOfBirthPrivate = priv?.dateOfBirthPrivate.orEmpty(),
            province = pub?.province.orEmpty(),
            district = pub?.district.orEmpty(),
            municipality = pub?.municipality.orEmpty(),
            culturalBadge = pub?.culturalBadge.orEmpty(),
            bio = pub?.bio.orEmpty(),
            lookingFor = pub?.lookingFor.orEmpty(),
            interests = pub?.interests.orEmpty(),
            prompts = pub?.prompts.orEmpty(),
            outgoingRequestsCount = outList.size,
            incomingRequestsCount = inList.size,
            activeConversationsCount = convList.size,
            savedNotes = notes
        )
    }

    fun buildExportJson(): String = DataExportHelper.toJson(buildExportData())

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.deleteAccountData() }
                .onSuccess { onDeleted() }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    suspend fun loadPublicProfile(uid: String): PublicProfile? =
        runCatching { repository.getPublicProfile(uid) }.getOrNull()

    private fun coordsForDistrict(district: String): Pair<Double, Double> = when (district.lowercase()) {
        "lalitpur" -> 27.6588 to 85.3247
        "bhaktapur" -> 27.6710 to 85.4298
        "kaski" -> 28.2096 to 83.9856
        "chitwan" -> 27.5291 to 84.3542
        "rupandehi" -> 27.5000 to 83.4500
        "morang" -> 26.4525 to 87.2718
        else -> 27.7172 to 85.3240
    }

    private fun extractReadableError(error: Throwable): String {
        val fs = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return fs?.localizedMessage ?: error.localizedMessage ?: "Something went wrong."
    }

    private companion object {
        const val TAG = "MainVM"
    }
}
