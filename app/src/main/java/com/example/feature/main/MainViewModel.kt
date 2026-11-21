package com.example.feature.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.LocalAppPreferences
import com.example.core.data.MeroMayaluRepository
import com.example.core.data.PreferencesRepository
import com.example.core.domain.BlockRecord
import com.example.core.domain.CallSession
import com.example.core.domain.ChatMessage
import com.example.core.domain.Connection
import com.example.core.domain.Conversation
import com.example.core.domain.CulturePost
import com.example.core.domain.HangoutItinerary
import com.example.core.domain.HeritagePlacesResult
import com.example.core.domain.InterestRequest
import com.example.core.domain.InviteCodeRecord
import com.example.core.domain.MeetupInvitation
import com.example.core.domain.PollVote
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.example.core.domain.SavedHangoutPlan
import com.example.core.domain.SavedNote
import com.example.core.domain.SecretCompliment
import com.example.core.domain.TodoItem
import com.example.core.domain.TogetherMemory
import com.example.core.domain.UiState
import com.example.core.firebase.MapsGroundingService
import com.example.core.localization.AppLanguage
import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.designsystem.MeroThemeMode
import com.example.core.util.DataExportHelper
import com.example.core.util.ExportUserData
import com.example.core.util.LocationServiceHelper
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

    val todoItems: StateFlow<UiState<List<TodoItem>>> = repository.observeTodoItems()
        .map<List<TodoItem>, UiState<List<TodoItem>>> { UiState.Success(it) }
        .catch { err -> emit(UiState.Error(extractReadableError(err))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myInviteCode: StateFlow<InviteCodeRecord?> = repository.observeMyInviteCode()
        .catch { err -> Log.w(TAG, "invite code flow error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val savedHangoutPlans: StateFlow<List<SavedHangoutPlan>> = repository.observeSavedHangoutPlans()
        .catch { err -> Log.w(TAG, "saved hangout plans error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val meetupInvites: StateFlow<List<MeetupInvitation>> = repository.observeMeetupInvites()
        .catch { err -> Log.w(TAG, "meetup invites error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val receivedCompliments: StateFlow<List<SecretCompliment>> = repository.observeReceivedCompliments()
        .catch { err -> Log.w(TAG, "compliments error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCallSession: StateFlow<CallSession?> = repository.observeCallSessions()
        .map { list ->
            val nowSec = System.currentTimeMillis() / 1000L
            list.firstOrNull { call ->
                val createdSec = call.createdAt?.seconds ?: nowSec
                val ageSec = (nowSec - createdSec).coerceAtLeast(0L)
                when (call.status) {
                    "RINGING" -> ageSec <= 90L
                    "ACCEPTED" -> ageSec <= 7200L
                    else -> false
                }
            }
        }
        .catch { err -> Log.w(TAG, "call session flow error: ${err.message}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _selectedActivityFilter = MutableStateFlow<String?>(null)
    val selectedActivityFilter: StateFlow<String?> = _selectedActivityFilter.asStateFlow()

    private val _hangoutPlannerState = MutableStateFlow<UiState<HangoutItinerary>?>(null)
    val hangoutPlannerState: StateFlow<UiState<HangoutItinerary>?> = _hangoutPlannerState.asStateFlow()

    private val _isHangoutAiEnhancing = MutableStateFlow(false)
    val isHangoutAiEnhancing: StateFlow<Boolean> = _isHangoutAiEnhancing.asStateFlow()

    private val _discoverySubTab = MutableStateFlow(DiscoverySubTab.FOR_YOU)
    val discoverySubTab: StateFlow<DiscoverySubTab> = _discoverySubTab.asStateFlow()

    val filteredDiscovery: StateFlow<UiState<List<PublicProfile>>> = combine(
        _rawDiscovery,
        _dismissedIds,
        _blockedUsers,
        _connections,
        _discoverySubTab,
        _session,
        _selectedActivityFilter
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
        val activityFilter = values[6] as String?

        when (raw) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> raw
            is UiState.Success -> {
                val blockedSet = blocked.map { it.blockedUid }.toSet()
                val connectedSet = conns.flatMap { it.memberUids }.filter { it != currentUserId }.toSet()
                val myProfile = sessionUi.publicProfile
                val eligibleBase = raw.data.filter { p ->
                    p.uid != currentUserId &&
                        p.uid !in dismissed &&
                        p.uid !in blockedSet &&
                        p.uid !in connectedSet
                }
                val eligible = if (!activityFilter.isNullOrBlank()) {
                    val activityMatched = eligibleBase.filter { p ->
                        activityFilter in p.favoriteActivities ||
                            p.interests.any { it.contains(activityFilter, ignoreCase = true) }
                    }
                    activityMatched.ifEmpty { eligibleBase }
                } else {
                    eligibleBase
                }
                val ranked = when (tab) {
                    DiscoverySubTab.NEARBY -> {
                        val sortedByKm = eligible.sortedBy { candidate ->
                            LocationServiceHelper.estimateDistanceKm(myProfile, candidate) ?: 999
                        }
                        val sameDistrict = sortedByKm.filter {
                            myProfile != null && it.district.equals(myProfile.district, ignoreCase = true)
                        }
                        if (sameDistrict.isNotEmpty()) {
                            sameDistrict + sortedByKm.filterNot { it in sameDistrict }
                        } else {
                            sortedByKm
                        }
                    }
                    DiscoverySubTab.FOR_YOU -> {
                        eligible.sortedByDescending { candidate ->
                            LocationServiceHelper.computeCompatibility(myProfile, candidate).totalPercent
                        }
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
    private var hangoutAiJob: Job? = null

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
            if (current.showOnlineStatus != priv.showOnlineStatus) {
                prefsRepository.setShowOnlineStatus(priv.showOnlineStatus)
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

    fun setShowOnlineStatus(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setShowOnlineStatus(enabled)
            if (_session.value.privateUser != null) {
                runCatching { repository.updatePreferences(mapOf("showOnlineStatus" to enabled)) }
                    .onFailure { err -> showToast(extractReadableError(err)) }
            }
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setDailyRemindersEnabled(enabled)
        }
    }

    fun setRingtone(channel: String, ringtoneId: String, customUri: String? = null) {
        viewModelScope.launch {
            prefsRepository.setRingtone(channel, ringtoneId, customUri)
        }
    }

    fun markDisclaimerAccepted() {
        viewModelScope.launch {
            prefsRepository.markDisclaimerAccepted(currentUserId)
        }
    }

    fun updateBooleanPreference(field: String, value: Boolean) {
        viewModelScope.launch {
            when (field) {
                "useBikramSambat" -> prefsRepository.setUseBikramSambat(value)
                "reducedMotion" -> prefsRepository.setReducedMotion(value)
                "showOnlineStatus" -> prefsRepository.setShowOnlineStatus(value)
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

    fun observePublicProfileFlow(uid: String): Flow<PublicProfile?> =
        repository.observePublicProfile(uid)
            .catch { err ->
                Log.w(TAG, "profile presence observe error for $uid: ${err.message}")
                emit(null)
            }

    fun setOnlinePresence(onlineNow: Boolean) {
        if (_session.value.publicProfile == null) return
        if (onlineNow && !localPrefs.value.showOnlineStatus) return
        viewModelScope.launch {
            repository.updateOnlinePresence(onlineNow)
        }
    }

    fun saveOrUpdateTodoItem(
        existingTodoId: String?,
        title: String,
        notes: String,
        category: String,
        priority: String,
        dueDateLabel: String,
        completed: Boolean = false,
        successToast: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.saveOrUpdateTodoItem(
                    existingTodoId = existingTodoId,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    dueDateLabel = dueDateLabel,
                    completed = completed
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

    fun toggleTodoCompleted(todoId: String, completed: Boolean) {
        viewModelScope.launch {
            runCatching { repository.toggleTodoCompleted(todoId, completed) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun deleteTodoItem(todoId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTodoItem(todoId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun searchHeritagePlaces(query: String, isNepali: Boolean) {
        val profile = _session.value.publicProfile
        val cityHint = profile?.district?.takeIf { it.isNotBlank() } ?: "Kathmandu Valley"
        val coords = if (profile != null && profile.locationSharingEnabled && profile.approxLat != 0.0 && profile.approxLng != 0.0) {
            profile.approxLat to profile.approxLng
        } else {
            coordsForDistrict(cityHint)
        }
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

    fun setActivityFilter(activityId: String?) {
        _selectedActivityFilter.value = if (_selectedActivityFilter.value == activityId) null else activityId
    }

    fun updateLocationAndPrivacy(
        locationSharingEnabled: Boolean,
        coarseLat: Double,
        coarseLng: Double,
        province: String,
        district: String,
        publicLocationLabel: String,
        successToast: String
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.updateLocationAndPrivacy(
                    locationSharingEnabled = locationSharingEnabled,
                    coarseLat = coarseLat,
                    coarseLng = coarseLng,
                    province = province,
                    district = district,
                    publicLocationLabel = publicLocationLabel
                )
            }.onSuccess {
                showToast(successToast)
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
            _isActionBusy.value = false
        }
    }

    fun updateVibeActivitiesAndQuestion(
        currentVibeId: String? = null,
        favoriteActivities: List<String>? = null,
        dailyQuestionAnswer: String? = null,
        successToast: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateVibeActivitiesAndQuestion(
                    currentVibeId = currentVibeId,
                    favoriteActivities = favoriteActivities,
                    dailyQuestionAnswer = dailyQuestionAnswer
                )
            }.onSuccess {
                showToast(successToast)
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun generateHangoutPlan(
        city: String,
        budgetNpr: Int,
        groupSize: Int,
        durationLabel: String,
        preferredActivities: List<String>,
        isNepali: Boolean,
        forceFreshVariation: Boolean = false
    ) {
        val profile = _session.value.publicProfile
        val targetCity = city.ifBlank { profile?.district ?: "Kathmandu" }
        val coords = if (profile != null && profile.locationSharingEnabled && profile.approxLat != 0.0 && profile.approxLng != 0.0) {
            profile.approxLat to profile.approxLng
        } else {
            coordsForDistrict(targetCity)
        }
        // Instant sub-10ms local/cached grounded plan so there is zero waiting or spinner lock!
        val instantPlan = mapsService.buildInstantHangoutItinerary(
            city = targetCity,
            budgetNpr = budgetNpr,
            groupSize = groupSize,
            durationLabel = durationLabel,
            preferredActivities = preferredActivities,
            isNepali = isNepali,
            forceFreshVariation = forceFreshVariation
        )
        _hangoutPlannerState.value = UiState.Success(instantPlan)

        // Optional background Gemini AI enhancement
        hangoutAiJob?.cancel()
        hangoutAiJob = viewModelScope.launch {
            _isHangoutAiEnhancing.value = true
            runCatching {
                val aiPlan = mapsService.enhanceItineraryWithGemini(
                    city = targetCity,
                    budgetNpr = budgetNpr,
                    groupSize = groupSize,
                    durationLabel = durationLabel,
                    preferredActivities = preferredActivities,
                    latitude = coords.first,
                    longitude = coords.second,
                    isNepali = isNepali
                )
                if (aiPlan != null && aiPlan.stops.isNotEmpty()) {
                    _hangoutPlannerState.value = UiState.Success(aiPlan)
                }
            }
            _isHangoutAiEnhancing.value = false
        }
    }

    fun swapHangoutStopWithAlternative(stepNumber: Int) {
        val current = (_hangoutPlannerState.value as? UiState.Success)?.data ?: return
        val updatedStops = current.stops.map { stop ->
            if (stop.stepNumber == stepNumber && stop.backupAlternative.isNotBlank()) {
                val oldPlace = stop.placeName
                val newPlace = stop.backupAlternative
                stop.copy(
                    placeName = newPlace,
                    backupAlternative = oldPlace,
                    mapsUri = "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode("$newPlace, ${current.city}, Nepal")}"
                )
            } else {
                stop
            }
        }
        _hangoutPlannerState.value = UiState.Success(current.copy(stops = updatedStops))
    }

    fun saveCurrentHangoutPlan(itinerary: HangoutItinerary, successToast: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching { repository.saveHangoutPlan(itinerary) }
                .onSuccess { showToast(successToast) }
                .onFailure { err -> showToast(extractReadableError(err)) }
            _isActionBusy.value = false
        }
    }

    fun deleteSavedHangoutPlan(planId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteHangoutPlan(planId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun sendMeetupInvitation(
        conversationId: String,
        recipientUid: String,
        recipientName: String,
        planTitle: String,
        city: String,
        proposedDateLabel: String,
        estimatedBudgetNpr: Int,
        stopsPreview: List<String>,
        successToast: String
    ) {
        val myName = _session.value.publicProfile?.displayName ?: "Member"
        viewModelScope.launch {
            runCatching {
                repository.sendMeetupInvitation(
                    conversationId = conversationId,
                    senderName = myName,
                    recipientUid = recipientUid,
                    recipientName = recipientName,
                    planTitle = planTitle,
                    city = city,
                    proposedDateLabel = proposedDateLabel,
                    estimatedBudgetNpr = estimatedBudgetNpr,
                    stopsPreview = stopsPreview
                )
            }.onSuccess {
                showToast(successToast)
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun respondToMeetupInvitation(inviteId: String, accepted: Boolean, toastMsg: String) {
        viewModelScope.launch {
            runCatching { repository.respondToMeetupInvitation(inviteId, accepted) }
                .onSuccess { showToast(toastMsg) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun observeTogetherMemories(conversationId: String): Flow<List<TogetherMemory>> =
        repository.observeTogetherMemories(conversationId)

    fun addTogetherMemory(
        conversationId: String,
        memberUids: List<String>,
        title: String,
        noteText: String,
        imageData: String,
        milestoneTag: String,
        successToast: String,
        onDone: () -> Unit = {}
    ) {
        val myName = _session.value.publicProfile?.displayName ?: "Member"
        viewModelScope.launch {
            _isActionBusy.value = true
            runCatching {
                repository.addTogetherMemory(
                    conversationId = conversationId,
                    authorName = myName,
                    memberUids = memberUids,
                    title = title,
                    noteText = noteText,
                    imageData = imageData,
                    milestoneTag = milestoneTag
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

    fun deleteTogetherMemory(conversationId: String, memoryId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTogetherMemory(conversationId, memoryId) }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun sendSecretCompliment(
        recipientUid: String,
        complimentKey: String,
        complimentText: String,
        successToast: String
    ) {
        val myProfile = _session.value.publicProfile ?: return
        viewModelScope.launch {
            runCatching {
                repository.sendSecretCompliment(
                    senderProfile = myProfile,
                    recipientUid = recipientUid,
                    complimentKey = complimentKey,
                    complimentText = complimentText
                )
            }.onSuccess {
                showToast(successToast)
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun startAudioCall(
        conversationId: String,
        calleeUid: String,
        calleeName: String,
        calleePhoto: String
    ) {
        val myProfile = _session.value.publicProfile ?: return
        val existingCall = activeCallSession.value
        if (existingCall != null && (existingCall.status == "RINGING" || existingCall.status == "ACCEPTED")) {
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.startAudioCall(
                    conversationId = conversationId,
                    callerName = myProfile.displayName,
                    callerPhoto = myProfile.photoDataList.firstOrNull().orEmpty(),
                    calleeUid = calleeUid,
                    calleeName = calleeName,
                    calleePhoto = calleePhoto
                )
            }.onFailure { err ->
                showToast(extractReadableError(err))
            }
        }
    }

    fun acceptIncomingCall(callId: String) {
        viewModelScope.launch {
            runCatching { repository.updateCallStatus(callId, "ACCEPTED") }
                .onFailure { err -> showToast(extractReadableError(err)) }
        }
    }

    fun declineOrEndCall(callId: String, newStatus: String = "ENDED") {
        viewModelScope.launch {
            runCatching { repository.updateCallStatus(callId, newStatus) }
                .onFailure { err -> Log.w(TAG, "endCall error: ${err.message}") }
        }
    }

    fun transmitCallAudioChunk(callSession: CallSession, base64Chunk: String, seq: Int) {
        viewModelScope.launch {
            repository.transmitAcceptedCallAudioChunk(callSession, base64Chunk, seq)
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

    suspend fun searchUserById(rawUid: String, isNepali: Boolean): Result<PublicProfile> {
        val cleanUid = rawUid.trim()
        if (cleanUid.length < 6) {
            return Result.failure(
                IllegalArgumentException(
                    if (isNepali) "कृपया मान्य प्रयोगकर्ता ID राख्नुहोस्।" else "Please enter a valid User ID."
                )
            )
        }
        if (cleanUid == currentUserId) {
            return Result.failure(
                IllegalArgumentException(
                    if (isNepali) "यो तपाईंको आफ्नै प्रयोगकर्ता ID हो।" else "That is your own User ID."
                )
            )
        }
        val profile = runCatching { repository.getPublicProfile(cleanUid) }.getOrNull()
            ?: return Result.failure(
                NoSuchElementException(
                    if (isNepali) "यो ID सँग मिल्ने सक्रिय प्रोफाइल भेटिएन।" else "No active profile found with that User ID."
                )
            )
        val myGender = _session.value.privateUser?.gender.orEmpty()
        if (myGender.isNotBlank() && profile.gender.equals(myGender, ignoreCase = true)) {
            return Result.failure(
                IllegalStateException(
                    if (isNepali) "नियम अनुसार विपरीत लिङ्गका सदस्यहरू मात्र खोज्न सकिन्छ।" else "Eligibility policy allows connecting with members of the opposite gender."
                )
            )
        }
        return Result.success(profile)
    }

    private fun coordsForDistrict(district: String): Pair<Double, Double> {
        val city = LocationServiceHelper.cityForDistrictOrName(district)
        return city.lat to city.lng
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
