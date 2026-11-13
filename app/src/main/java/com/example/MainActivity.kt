package com.example

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.data.LocalAppPreferences
import com.example.core.data.MeroMayaluRepository
import com.example.core.data.PreferencesRepository
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroBottomNav
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroLotusEmblem
import com.example.core.designsystem.MeroMayaluTheme
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroTab
import com.example.core.designsystem.MeroToast
import com.example.core.designsystem.MeroType
import com.example.core.domain.PublicProfile
import com.example.core.domain.UiState
import com.example.core.firebase.GoogleAuthHelper
import com.example.core.firebase.MapsGroundingService
import com.example.core.firebase.configureAppCheck
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.notifications.MeroNotificationHelper
import com.example.core.security.BiometricLockHelper
import com.example.feature.auth.WelcomeAuthScreen
import com.example.feature.chat.ChatScreen
import com.example.feature.culture.CultureScreen
import com.example.feature.discovery.DiscoveryScreen
import com.example.feature.discovery.InterestSentScreen
import com.example.feature.interests.LikesConnectionsScreen
import com.example.feature.main.MainViewModel
import com.example.feature.main.MeroPushEvent
import com.example.feature.moments.MomentsScreen
import com.example.feature.moments.MomentsSubTab
import com.example.feature.onboarding.OnboardingScreen
import com.example.feature.profile.AboutAppDialog
import com.example.feature.profile.HelpSafetyDialog
import com.example.feature.profile.ProfileDetailSheet
import com.example.feature.profile.ProfileScreen
import com.example.feature.profile.ReportUserDialog
import com.example.feature.profile.SettingsScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Configure App Check BEFORE enableEdgeToEdge, setContent, or ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MeroMayaluRoot()
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private fun connectivityFlow(context: Context): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    fun isConnected(): Boolean {
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    trySend(isConnected())
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }
        override fun onLost(network: Network) {
            trySend(isConnected())
        }
    }
    runCatching { cm?.registerDefaultNetworkCallback(callback) }
    awaitClose { runCatching { cm?.unregisterNetworkCallback(callback) } }
}

@Composable
private fun MeroMayaluRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val localPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = LocalAppPreferences())
    val isOnline by remember(context) { connectivityFlow(context.applicationContext) }
        .collectAsStateWithLifecycle(initialValue = true)

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Biometric / Device PIN App Lock Gate
    var isAppUnlocked by rememberSaveable { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var stoppedAtMs by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, localPrefs.appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    stoppedAtMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (localPrefs.appLockEnabled && stoppedAtMs > 0L) {
                        val elapsed = System.currentTimeMillis() - stoppedAtMs
                        if (elapsed >= 20_000L) {
                            isAppUnlocked = false
                        }
                        stoppedAtMs = 0L
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun triggerUnlock() {
        val activity = context.findActivity()
        if (activity == null || !BiometricLockHelper.canAuthenticate(context)) {
            isAppUnlocked = true
            return
        }
        lockError = null
        val s = AppStrings.forLanguage(localPrefs.language)
        BiometricLockHelper.authenticate(
            activity = activity,
            title = s.appLockTitle,
            subtitle = s.appLockedTitle,
            description = s.appLockedBody,
            onSuccess = {
                lockError = null
                isAppUnlocked = true
            },
            onError = { msg -> lockError = msg }
        )
    }

    LaunchedEffect(localPrefs.appLockEnabled, isAppUnlocked) {
        if (localPrefs.appLockEnabled && !isAppUnlocked) {
            triggerUnlock()
        }
    }

    // Attempt silent CredentialManager auto sign-in once when unauthenticated
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(context, auth)
        }
    }

    MeroMayaluTheme(
        themeCatalog = localPrefs.themeCatalog,
        themeMode = localPrefs.themeMode,
        fontCatalog = localPrefs.fontCatalog
    ) {
        if (localPrefs.appLockEnabled && !isAppUnlocked) {
            AppLockGateScreen(
                language = localPrefs.language,
                errorText = lockError,
                onUnlock = ::triggerUnlock
            )
            return@MeroMayaluTheme
        }

        val user = currentUser
        if (user == null) {
            WelcomeAuthScreen(
                language = localPrefs.language,
                onLanguageChange = { lang ->
                    scope.launch { prefsRepo.setLanguage(lang) }
                },
                isLoading = isSigningIn,
                errorMessage = authError,
                onContinueWithGoogle = {
                    if (!isSigningIn) {
                        isSigningIn = true
                        authError = null
                        scope.launch {
                            when (val res = GoogleAuthHelper.signInWithGoogleInteractive(context, auth)) {
                                is GoogleAuthHelper.SignInResult.Success -> {
                                    isSigningIn = false
                                }
                                is GoogleAuthHelper.SignInResult.Cancelled -> {
                                    isSigningIn = false
                                }
                                is GoogleAuthHelper.SignInResult.Error -> {
                                    authError = res.message
                                    isSigningIn = false
                                }
                            }
                        }
                    }
                }
            )
        } else {
            key(user.uid) {
                val mainViewModel: MainViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(dbId)
                            MainViewModel(
                                repository = MeroMayaluRepository(db, auth),
                                prefsRepository = PreferencesRepository(app),
                                mapsService = MapsGroundingService(),
                                currentUserId = user.uid
                            )
                        }
                    }
                )
                AuthenticatedShell(
                    viewModel = mainViewModel,
                    isOnline = isOnline,
                    onSignOut = {
                        scope.launch { GoogleAuthHelper.signOut(context, auth) }
                    }
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun AppLockGateScreen(
    language: AppLanguage,
    errorText: String?,
    onUnlock: () -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(MeroSpacing.lg)
    ) {
        MeroCard(modifier = Modifier.widthIn(max = 420.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MeroSpacing.md)
            ) {
                MeroLotusEmblem(size = 56.dp, tint = colors.primary)
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = s.appLockedTitle,
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = s.appLockedTitle,
                    style = MeroType.Title.copy(color = colors.text),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = s.appLockedBody,
                    style = MeroType.Body.copy(color = colors.textMuted),
                    textAlign = TextAlign.Center
                )
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText,
                        style = MeroType.Caption.copy(color = colors.danger),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(MeroSpacing.xs))
                MeroButton(
                    text = s.unlockAppButton,
                    leadingIcon = Icons.Filled.Fingerprint,
                    onClick = onUnlock,
                    testTag = "unlock_app_button"
                )
            }
        }
    }
}

private enum class SubScreenRoute {
    NONE, EDIT_PROFILE, SETTINGS, CHAT
}

@Composable
private fun AuthenticatedShell(
    viewModel: MainViewModel,
    isOnline: Boolean,
    onSignOut: () -> Unit
) {
    val colors = LocalMeroColors.current
    val scope = rememberCoroutineScope()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val feedState by viewModel.filteredDiscovery.collectAsStateWithLifecycle()
    val discoverySubTab by viewModel.discoverySubTab.collectAsStateWithLifecycle()
    val incomingState by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoingState by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val interestSentTarget by viewModel.interestSentTarget.collectAsStateWithLifecycle()
    val celebrationPair by viewModel.celebrationPair.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val undoTargetUid by viewModel.undoDismissTargetUid.collectAsStateWithLifecycle()
    val isActionBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()
    val culturePlacesState by viewModel.culturePlacesState.collectAsStateWithLifecycle()
    val culturePostsState by viewModel.culturePosts.collectAsStateWithLifecycle()
    val savedNotesState by viewModel.savedNotes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lang = localPrefs.language
    val s = AppStrings.forLanguage(lang)

    LaunchedEffect(viewModel) {
        viewModel.pushEvents.collect { event ->
            val priv = session.privateUser
            if (priv?.notificationsEnabled != false) {
                when (event) {
                    is MeroPushEvent.NewIncomingRequest -> {
                        MeroNotificationHelper.showInterestNotification(
                            context = context,
                            senderName = event.senderName,
                            discreet = priv?.discreetNotifications ?: true,
                            quietHoursEnabled = priv?.quietHoursEnabled ?: false,
                            isNepali = localPrefs.language == AppLanguage.NEPALI
                        )
                    }
                    is MeroPushEvent.NewMessage -> {
                        MeroNotificationHelper.showMessageNotification(
                            context = context,
                            senderName = event.senderName,
                            preview = event.preview,
                            discreet = priv?.discreetNotifications ?: true,
                            quietHoursEnabled = priv?.quietHoursEnabled ?: false,
                            isNepali = localPrefs.language == AppLanguage.NEPALI
                        )
                    }
                }
            }
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MeroTab.HOME) }
    var requestedMomentsTab by remember { mutableStateOf<MomentsSubTab?>(null) }
    var subRoute by rememberSaveable { mutableStateOf(SubScreenRoute.NONE) }
    var activeConversationId by rememberSaveable { mutableStateOf("") }
    var inspectingProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var reportingTargetUid by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (session.isLoadingProfile) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
        ) {
            MeroLoadingState()
        }
        return
    }

    if (session.error != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(MeroSpacing.lg)
        ) {
            MeroErrorState(
                message = session.error.orEmpty(),
                retryLabel = s.signOutLabel,
                onRetry = onSignOut
            )
        }
        return
    }

    // First-time user onboarding gate
    if (session.privateUser == null || session.publicProfile == null) {
        OnboardingScreen(
            currentUid = viewModel.currentUserId,
            language = lang,
            existingPrivate = null,
            existingPublic = null,
            isBusy = isActionBusy,
            onLanguageChange = viewModel::updateLanguage,
            onSave = { priv, pub, isInitial ->
                viewModel.saveProfile(priv, pub, isInitial) {}
            }
        )
        return
    }

    // Subscreen routing
    when (subRoute) {
        SubScreenRoute.EDIT_PROFILE -> {
            OnboardingScreen(
                currentUid = viewModel.currentUserId,
                language = lang,
                existingPrivate = session.privateUser,
                existingPublic = session.publicProfile,
                isBusy = isActionBusy,
                onLanguageChange = viewModel::updateLanguage,
                onSave = { priv, pub, isInitial ->
                    viewModel.saveProfile(priv, pub, isInitial) {
                        subRoute = SubScreenRoute.NONE
                    }
                },
                onCancelEdit = { subRoute = SubScreenRoute.NONE }
            )
            return
        }
        SubScreenRoute.SETTINGS -> {
            SettingsScreen(
                language = lang,
                localPrefs = localPrefs,
                privateUser = session.privateUser,
                blockedUsers = blockedUsers,
                buildExportJson = viewModel::buildExportJson,
                onBack = { subRoute = SubScreenRoute.NONE },
                onSelectLanguage = viewModel::updateLanguage,
                onSelectThemeCatalog = viewModel::updateThemeCatalog,
                onSelectFontCatalog = viewModel::updateFontCatalog,
                onSelectThemeMode = viewModel::updateThemeMode,
                onToggleAppLock = viewModel::setAppLockEnabled,
                onToggleBoolean = viewModel::updateBooleanPreference,
                onToggleVisibility = viewModel::updateVisibility,
                onUnblockUser = viewModel::unblockUser,
                onExportSuccessToast = { msg -> viewModel.showToast(msg) },
                onSignOut = onSignOut,
                onDeleteAccount = {
                    viewModel.deleteAccount { onSignOut() }
                }
            )
            return
        }
        SubScreenRoute.CHAT -> {
            val convFlow = remember(activeConversationId) { viewModel.observeConversation(activeConversationId) }
            val msgsFlow = remember(activeConversationId) { viewModel.observeMessages(activeConversationId) }
            ChatScreen(
                currentUid = viewModel.currentUserId,
                language = lang,
                conversationFlow = convFlow,
                messagesFlow = msgsFlow,
                onBack = { subRoute = SubScreenRoute.NONE },
                onViewPartnerProfile = { partnerUid ->
                    scope.launch {
                        inspectingProfile = viewModel.loadPublicProfile(partnerUid)
                    }
                },
                onSendText = { conv, text ->
                    viewModel.sendChatMessage(conv, "TEXT", text = text)
                },
                onSendImage = { conv, mediaData ->
                    viewModel.sendChatMessage(conv, "IMAGE", mediaData = mediaData)
                },
                onSendVoice = { conv, note ->
                    viewModel.sendChatMessage(
                        conv,
                        "VOICE",
                        mediaData = note.base64Audio,
                        audioDurationSec = note.durationSec,
                        waveform = note.waveform
                    )
                },
                onToggleMute = { conv ->
                    viewModel.toggleMuteConversation(conv.id, conv.status == "MUTED")
                },
                onBlockPartner = { uid ->
                    val msg = if (lang == AppLanguage.NEPALI) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                    viewModel.blockUser(uid, msg)
                    subRoute = SubScreenRoute.NONE
                },
                onReportPartner = { uid -> reportingTargetUid = uid },
                onShowError = { msg -> viewModel.showToast(msg) }
            )
            if (reportingTargetUid != null) {
                ReportUserDialog(
                    language = lang,
                    onDismiss = { reportingTargetUid = null },
                    onSubmit = { cat, details ->
                        val target = reportingTargetUid ?: return@ReportUserDialog
                        reportingTargetUid = null
                        val msg = if (lang == AppLanguage.NEPALI) "रिपोर्ट पठाइयो।" else "Report submitted."
                        viewModel.submitReport(target, cat, details, msg)
                    }
                )
            }
            inspectingProfile?.let { p ->
                ProfileDetailSheet(
                    profile = p,
                    language = lang,
                    canSendInterest = false,
                    onDismiss = { inspectingProfile = null },
                    onInterested = {},
                    onReport = { reportingTargetUid = p.uid },
                    onBlock = {
                        val msg = if (lang == AppLanguage.NEPALI) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                        viewModel.blockUser(p.uid, msg)
                        subRoute = SubScreenRoute.NONE
                    }
                )
            }
            return
        }
        SubScreenRoute.NONE -> Unit
    }

    // Screen 3: Interest Sent confirmation waiting state
    val sentTarget = interestSentTarget
    if (sentTarget != null) {
        BackHandler { viewModel.clearInterestSentOverlay() }
        InterestSentScreen(
            target = sentTarget,
            language = lang,
            onViewProfile = {
                inspectingProfile = sentTarget
            },
            onKeepExploring = {
                viewModel.clearInterestSentOverlay()
            },
            onJumpToTab = { tab ->
                viewModel.clearInterestSentOverlay()
                currentTab = if (tab == MeroTab.EXPLORE) MeroTab.CULTURE else tab
            }
        )
        inspectingProfile?.let { p ->
            ProfileDetailSheet(
                profile = p,
                language = lang,
                canSendInterest = false,
                onDismiss = { inspectingProfile = null },
                onInterested = {},
                onReport = { reportingTargetUid = p.uid },
                onBlock = {
                    val msg = if (lang == AppLanguage.NEPALI) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                    viewModel.blockUser(p.uid, msg)
                    viewModel.clearInterestSentOverlay()
                }
            )
        }
        return
    }

    val outgoingList = (outgoingState as? UiState.Success)?.data.orEmpty()
    val incomingList = (incomingState as? UiState.Success)?.data.orEmpty()
    val conversationsList = (conversationsState as? UiState.Success)?.data.orEmpty()
    val hasPendingOutgoing = outgoingList.any { it.status == "PENDING" }
    val pendingIncomingCount = incomingList.count { it.status == "PENDING" }

    val navLabels = mapOf(
        MeroTab.HOME to s.navHome,
        MeroTab.LIKES to s.navLikes,
        MeroTab.EXPLORE to s.navExplore,
        MeroTab.CULTURE to s.navCulture,
        MeroTab.PROFILE to s.navProfile
    )

    val bottomContentPadding = 96.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (!isOnline) {
                Surface(
                    color = colors.danger,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = s.offlineNotice,
                        style = MeroType.Caption.copy(color = colors.onPrimary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    MeroTab.HOME -> {
                        DiscoveryScreen(
                            language = lang,
                            subTab = discoverySubTab,
                            feedState = feedState,
                            hasPendingOutgoing = hasPendingOutgoing,
                            incomingCount = pendingIncomingCount,
                            onSelectSubTab = viewModel::selectDiscoverySubTab,
                            onOpenNotifications = { currentTab = MeroTab.LIKES },
                            onViewProfile = { inspectingProfile = it },
                            onNotInterested = { profile ->
                                viewModel.dismissProfile(profile.uid, s.dismissedUndo)
                            },
                            onInterested = { profile ->
                                viewModel.sendInterest(profile, s.activeRequestBanner)
                            },
                            onReportUser = { reportingTargetUid = it.uid },
                            onBlockUser = { profile ->
                                val msg = if (lang == AppLanguage.NEPALI) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                                viewModel.blockUser(profile.uid, msg)
                            },
                            onRetry = viewModel::retryDiscovery,
                            bottomPadding = bottomContentPadding
                        )
                    }
                    MeroTab.EXPLORE -> {
                        MomentsScreen(
                            currentUid = viewModel.currentUserId,
                            language = lang,
                            useBikramSambat = localPrefs.useBikramSambat,
                            requestedSubTab = requestedMomentsTab,
                            onConsumedRequestedTab = { requestedMomentsTab = null },
                            postsState = culturePostsState,
                            notesState = savedNotesState,
                            isBusy = isActionBusy,
                            observeReactions = viewModel::observePostReactions,
                            onToggleReaction = viewModel::togglePostReaction,
                            onCreatePost = { thought, img, tag, onDone ->
                                viewModel.createCulturePost(
                                    thoughtText = thought,
                                    imageData = img,
                                    culturalTag = tag,
                                    successToast = s.postPublishedToast,
                                    onDone = onDone
                                )
                            },
                            onDeletePost = viewModel::deleteCulturePost,
                            onSaveNote = { existingId, title, body, postId, author, excerpt, img, onDone ->
                                viewModel.saveOrUpdateNote(
                                    existingNoteId = existingId,
                                    title = title,
                                    noteText = body,
                                    sourcePostId = postId,
                                    sourceAuthorName = author,
                                    sourceExcerpt = excerpt,
                                    imageData = img,
                                    successToast = s.noteSavedToast,
                                    onDone = onDone
                                )
                            },
                            onDeleteNote = viewModel::deleteSavedNote,
                            onOpenAuthorProfile = { uid ->
                                scope.launch {
                                    inspectingProfile = viewModel.loadPublicProfile(uid)
                                }
                            },
                            onShowError = viewModel::showToast,
                            bottomPadding = bottomContentPadding
                        )
                    }
                    MeroTab.LIKES -> {
                        LikesConnectionsScreen(
                            currentUid = viewModel.currentUserId,
                            language = lang,
                            incomingState = incomingState,
                            outgoingState = outgoingState,
                            conversationsState = conversationsState,
                            isBusy = isActionBusy,
                            celebrationPair = celebrationPair,
                            onDismissCelebration = viewModel::clearCelebration,
                            onApprove = viewModel::approveIncomingRequest,
                            onDecline = viewModel::declineIncomingRequest,
                            onWithdraw = viewModel::withdrawOutgoingRequest,
                            onOpenProfileUid = { uid ->
                                scope.launch {
                                    inspectingProfile = viewModel.loadPublicProfile(uid)
                                }
                            },
                            onOpenChat = { convId ->
                                activeConversationId = convId
                                subRoute = SubScreenRoute.CHAT
                            },
                            bottomPadding = bottomContentPadding
                        )
                    }
                    MeroTab.CULTURE -> {
                        CultureScreen(
                            language = lang,
                            useBikramSambat = localPrefs.useBikramSambat,
                            placesState = culturePlacesState,
                            onSearchPlaces = { q ->
                                viewModel.searchHeritagePlaces(q, isNepali = lang == AppLanguage.NEPALI)
                            },
                            bottomPadding = bottomContentPadding
                        )
                    }
                    MeroTab.PROFILE -> {
                        ProfileScreen(
                            language = lang,
                            publicProfile = session.publicProfile,
                            interestsCount = incomingList.size + outgoingList.size,
                            matchesCount = conversationsList.size,
                            chatsCount = conversationsList.size,
                            onUpdateBanner = { bannerData ->
                                val msg = if (lang == AppLanguage.NEPALI) "ब्यानर अद्यावधिक गरियो।" else "Profile banner updated!"
                                viewModel.updateBannerData(bannerData, msg)
                            },
                            onAddCustomInterest = { tag ->
                                val msg = if (lang == AppLanguage.NEPALI) "रुचि थपियो।" else "Interest added!"
                                viewModel.addCustomInterest(tag, msg)
                            },
                            onRemoveInterest = viewModel::removeInterest,
                            onOpenSavedNotes = {
                                requestedMomentsTab = MomentsSubTab.NOTES
                                currentTab = MeroTab.EXPLORE
                            },
                            onEditProfile = { subRoute = SubScreenRoute.EDIT_PROFILE },
                            onOpenSettings = { subRoute = SubScreenRoute.SETTINGS },
                            onOpenHelpSafety = { showHelpDialog = true },
                            onOpenAbout = { showAboutDialog = true },
                            onShowError = viewModel::showToast,
                            bottomPadding = bottomContentPadding
                        )
                    }
                }
            }
        }

        MeroToast(
            message = toastMessage,
            actionLabel = if (undoTargetUid != null) s.undo else null,
            onAction = if (undoTargetUid != null) viewModel::undoDismiss else null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )

        MeroBottomNav(
            currentTab = currentTab,
            onSelectTab = { tab -> currentTab = tab },
            labels = navLabels,
            pendingCount = pendingIncomingCount,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    inspectingProfile?.let { p ->
        ProfileDetailSheet(
            profile = p,
            language = lang,
            canSendInterest = !hasPendingOutgoing && p.uid != viewModel.currentUserId,
            onDismiss = { inspectingProfile = null },
            onInterested = { viewModel.sendInterest(p, s.activeRequestBanner) },
            onReport = { reportingTargetUid = p.uid },
            onBlock = {
                val msg = if (lang == AppLanguage.NEPALI) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                viewModel.blockUser(p.uid, msg)
            }
        )
    }

    if (reportingTargetUid != null) {
        ReportUserDialog(
            language = lang,
            onDismiss = { reportingTargetUid = null },
            onSubmit = { cat, details ->
                val target = reportingTargetUid ?: return@ReportUserDialog
                reportingTargetUid = null
                val msg = if (lang == AppLanguage.NEPALI) "रिपोर्ट पठाइयो।" else "Report submitted."
                viewModel.submitReport(target, cat, details, msg)
            }
        )
    }

    if (showHelpDialog) {
        HelpSafetyDialog(language = lang, onDismiss = { showHelpDialog = false })
    }
    if (showAboutDialog) {
        AboutAppDialog(language = lang, onDismiss = { showAboutDialog = false })
    }
}
