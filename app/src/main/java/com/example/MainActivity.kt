package com.example

import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.example.core.notifications.SoulmateReminderScheduler
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
import com.example.feature.profile.FirstDayCommunityDisclaimerDialog
import com.example.feature.profile.HelpSafetyDialog
import com.example.feature.profile.ProfileDetailSheet
import com.example.feature.profile.ProfileScreen
import com.example.feature.profile.ReportUserDialog
import com.example.feature.profile.SearchByIdDialog
import com.example.feature.profile.SettingsScreen
import android.content.Intent
import com.example.core.domain.Conversation
import com.example.core.util.LocationServiceHelper
import com.example.feature.call.MutualConsentCallOverlay
import com.example.feature.culture.CompatibilityBreakdownSheet
import com.example.feature.culture.HangoutPlannerModal
import com.example.feature.culture.LocationPrivacySheet
import com.example.feature.culture.TogetherMemoriesSheet
import com.example.feature.culture.VibeAndActivitySheet
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
        enableHighestDisplayRefreshRate()
        enableEdgeToEdge()
        setContent {
            MeroMayaluRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    private fun enableHighestDisplayRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val displayObj: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display ?: dm?.getDisplay(Display.DEFAULT_DISPLAY)
            } else {
                dm?.getDisplay(Display.DEFAULT_DISPLAY)
            }
            val currentMode = displayObj?.mode ?: return
            val bestMode = displayObj.supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
            if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = bestMode.modeId
                }
            }
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
    val todosState by viewModel.todoItems.collectAsStateWithLifecycle()
    val myInviteCode by viewModel.myInviteCode.collectAsStateWithLifecycle()
    val savedPlansList by viewModel.savedHangoutPlans.collectAsStateWithLifecycle()
    val meetupInvitesList by viewModel.meetupInvites.collectAsStateWithLifecycle()
    val receivedComplimentsList by viewModel.receivedCompliments.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val selectedActivityFilter by viewModel.selectedActivityFilter.collectAsStateWithLifecycle()
    val hangoutPlannerState by viewModel.hangoutPlannerState.collectAsStateWithLifecycle()
    val isHangoutAiEnhancing by viewModel.isHangoutAiEnhancing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shellLifecycleOwner = LocalLifecycleOwner.current

    val hasCompletedProfile = session.publicProfile != null
    LaunchedEffect(localPrefs.dailyRemindersEnabled) {
        SoulmateReminderScheduler.syncReminders(context.applicationContext, localPrefs.dailyRemindersEnabled)
    }
    LaunchedEffect(hasCompletedProfile, localPrefs.showOnlineStatus) {
        if (hasCompletedProfile) {
            viewModel.setOnlinePresence(localPrefs.showOnlineStatus)
        }
    }
    DisposableEffect(shellLifecycleOwner, hasCompletedProfile, localPrefs.showOnlineStatus) {
        val observer = LifecycleEventObserver { _, event ->
            if (!hasCompletedProfile) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setOnlinePresence(localPrefs.showOnlineStatus)
                Lifecycle.Event.ON_STOP -> viewModel.setOnlinePresence(false)
                else -> Unit
            }
        }
        shellLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (hasCompletedProfile) viewModel.setOnlinePresence(false)
            shellLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val lang = localPrefs.language
    val s = AppStrings.forLanguage(lang)
    val isNe = lang == AppLanguage.NEPALI

    LaunchedEffect(viewModel) {
        viewModel.pushEvents.collect { event ->
            val priv = session.privateUser
            val prefsNow = localPrefs
            if (priv?.notificationsEnabled != false) {
                when (event) {
                    is MeroPushEvent.NewIncomingRequest -> {
                        MeroNotificationHelper.showInterestNotification(
                            context = context,
                            senderName = event.senderName,
                            discreet = priv?.discreetNotifications ?: true,
                            quietHoursEnabled = priv?.quietHoursEnabled ?: false,
                            isNepali = prefsNow.language == AppLanguage.NEPALI,
                            ringtoneId = prefsNow.notificationRingtoneId,
                            customRingtoneUri = prefsNow.notificationCustomRingtoneUri
                        )
                    }
                    is MeroPushEvent.NewMessage -> {
                        MeroNotificationHelper.showMessageNotification(
                            context = context,
                            senderName = event.senderName,
                            preview = event.preview,
                            discreet = priv?.discreetNotifications ?: true,
                            quietHoursEnabled = priv?.quietHoursEnabled ?: false,
                            isNepali = prefsNow.language == AppLanguage.NEPALI,
                            ringtoneId = prefsNow.messageRingtoneId,
                            customRingtoneUri = prefsNow.messageCustomRingtoneUri
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
    var showHangoutPlanner by rememberSaveable { mutableStateOf(false) }
    var showLocationPrivacySheet by rememberSaveable { mutableStateOf(false) }
    var showVibeCheckSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchByIdDialog by rememberSaveable { mutableStateOf(false) }
    var disclaimerDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var compatibilityTargetProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var memoriesTargetConversation by remember { mutableStateOf<Conversation?>(null) }

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

    val outgoingList = (outgoingState as? UiState.Success)?.data.orEmpty()
    val incomingList = (incomingState as? UiState.Success)?.data.orEmpty()
    val conversationsList = (conversationsState as? UiState.Success)?.data.orEmpty()
    val hasPendingOutgoing = outgoingList.any { it.status == "PENDING" }
    val pendingIncomingCount = incomingList.count { it.status == "PENDING" }

    val animDuration = if (localPrefs.reducedMotion) 0 else 210

    AnimatedContent(
        targetState = subRoute,
        transitionSpec = {
            val enteringSub = targetState != SubScreenRoute.NONE
            if (animDuration == 0) {
                fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            } else if (enteringSub) {
                (fadeIn(tween(animDuration)) + slideInHorizontally(tween(animDuration)) { w -> w / 5 })
                    .togetherWith(fadeOut(tween(animDuration - 30)) + slideOutHorizontally(tween(animDuration)) { w -> -w / 8 })
            } else {
                (fadeIn(tween(animDuration)) + slideInHorizontally(tween(animDuration)) { w -> -w / 6 })
                    .togetherWith(fadeOut(tween(animDuration - 30)) + slideOutHorizontally(tween(animDuration)) { w -> w / 5 })
            }
        },
        label = "subroute_transition"
    ) { activeSubRoute ->
        when (activeSubRoute) {
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
            }
            SubScreenRoute.SETTINGS -> {
                SettingsScreen(
                    language = lang,
                    localPrefs = localPrefs,
                    privateUser = session.privateUser,
                    blockedUsers = blockedUsers,
                    buildExportData = viewModel::buildExportData,
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
                    },
                    onToggleOnlineStatus = { enabled ->
                        viewModel.setShowOnlineStatus(enabled)
                        viewModel.setOnlinePresence(enabled)
                    },
                    onToggleHaptics = viewModel::setHapticFeedbackEnabled,
                    onToggleDailyReminders = { enabled ->
                        viewModel.setDailyRemindersEnabled(enabled)
                        SoulmateReminderScheduler.syncReminders(context.applicationContext, enabled)
                    },
                    onSelectRingtone = viewModel::setRingtone
                )
            }
            SubScreenRoute.CHAT -> {
                val convFlow = remember(activeConversationId) { viewModel.observeConversation(activeConversationId) }
                val msgsFlow = remember(activeConversationId) { viewModel.observeMessages(activeConversationId) }
                Box(modifier = Modifier.fillMaxSize()) {
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
                        onDeleteMessage = { conv, msg ->
                            val toast = if (isNe) "सन्देश स्थायी रूपमा मेटियो।" else "Message permanently deleted."
                            viewModel.deleteChatMessage(conv.id, msg, toast)
                        },
                        onToggleMute = { conv ->
                            viewModel.toggleMuteConversation(conv.id, conv.status == "MUTED")
                        },
                        onBlockPartner = { uid ->
                            val msg = if (isNe) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                            viewModel.blockUser(uid, msg)
                            subRoute = SubScreenRoute.NONE
                        },
                        onReportPartner = { uid -> reportingTargetUid = uid },
                        onShowError = { msg -> viewModel.showToast(msg) },
                        meetupInvites = meetupInvitesList,
                        observePartnerProfile = viewModel::observePublicProfileFlow,
                        onStartAudioCall = { conv ->
                            val isUserA = conv.userAId == viewModel.currentUserId
                            val calleeUid = if (isUserA) conv.userBId else conv.userAId
                            val calleeName = if (isUserA) conv.userBName else conv.userAName
                            val calleePhoto = if (isUserA) conv.userBPhoto else conv.userAPhoto
                            viewModel.startAudioCall(
                                conversationId = conv.id,
                                calleeUid = calleeUid,
                                calleeName = calleeName,
                                calleePhoto = calleePhoto
                            )
                        },
                        onOpenTogetherMemories = { conv ->
                            memoriesTargetConversation = conv
                        },
                        onRespondMeetupInvite = { inviteId, accept ->
                            val toast = if (accept) {
                                if (isNe) "भेटघाट निमन्त्रणा स्वीकार गरियो!" else "Meetup invitation accepted!"
                            } else {
                                if (isNe) "भेटघाट निमन्त्रणा अस्वीकार गरियो।" else "Meetup invitation declined."
                            }
                            viewModel.respondToMeetupInvitation(inviteId, accept, toast)
                        },
                        myGender = session.privateUser?.gender.orEmpty(),
                        hapticEnabled = localPrefs.hapticFeedbackEnabled,
                        onSendVideo = { conv, mediaData, durSec ->
                            viewModel.sendChatMessage(
                                conv,
                                "VIDEO",
                                mediaData = mediaData,
                                audioDurationSec = durSec
                            )
                        }
                    )

                    activeCallSession?.let { call ->
                        MutualConsentCallOverlay(
                            callSession = call,
                            currentUid = viewModel.currentUserId,
                            language = lang,
                            callRingtoneId = localPrefs.callRingtoneId,
                            callCustomRingtoneUri = localPrefs.callCustomRingtoneUri,
                            hapticEnabled = localPrefs.hapticFeedbackEnabled,
                            onAcceptCall = viewModel::acceptIncomingCall,
                            onDeclineOrEndCall = viewModel::declineOrEndCall,
                            onTransmitAudioChunk = viewModel::transmitCallAudioChunk,
                            onShowError = viewModel::showToast
                        )
                    }
                }
            }
            SubScreenRoute.NONE -> {
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
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    if (animDuration == 0) {
                                        fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                                    } else {
                                        val forward = targetState.ordinal > initialState.ordinal
                                        (fadeIn(tween(190)) + slideInHorizontally(tween(190)) { w -> if (forward) w / 10 else -w / 10 })
                                            .togetherWith(fadeOut(tween(150)))
                                    }
                                },
                                label = "bottom_tab_transition"
                            ) { tab ->
                                when (tab) {
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
                                                val msg = if (isNe) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                                                viewModel.blockUser(profile.uid, msg)
                                            },
                                            onRetry = {
                                                viewModel.retryDiscovery()
                                                val msg = if (isNe) "गृह फिड ताजा गरियो।" else "Feed refreshed!"
                                                viewModel.showToast(msg)
                                            },
                                            bottomPadding = bottomContentPadding,
                                            myProfile = session.publicProfile,
                                            selectedActivityFilter = selectedActivityFilter,
                                            onSelectActivityFilter = viewModel::setActivityFilter,
                                            onOpenLocationPrivacy = { showLocationPrivacySheet = true },
                                            onOpenVibeCheck = { showVibeCheckSheet = true },
                                            onOpenCompatibility = { compatibilityTargetProfile = it },
                                            onOpenSearchById = { showSearchByIdDialog = true }
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
                                            todosState = todosState,
                                            isBusy = isActionBusy,
                                            observeReactions = viewModel::observePostReactions,
                                            observePollVotes = viewModel::observePollVotes,
                                            onToggleReaction = viewModel::togglePostReaction,
                                            onVotePoll = viewModel::voteOnPoll,
                                            onCreatePost = { thought, img, tag, postType, pollOptions, scheduledAt, onDone ->
                                                val isFutureSchedule = scheduledAt != null &&
                                                    scheduledAt.toDate().time > System.currentTimeMillis()
                                                val toast = if (isFutureSchedule) {
                                                    if (isNe) "तपाईंको पोस्ट तालिका गरियो!" else "Your post has been scheduled!"
                                                } else {
                                                    s.postPublishedToast
                                                }
                                                viewModel.createCulturePost(
                                                    thoughtText = thought,
                                                    imageData = img,
                                                    culturalTag = tag,
                                                    postType = postType,
                                                    pollOptions = pollOptions,
                                                    scheduledAt = scheduledAt,
                                                    successToast = toast,
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
                                            onSaveTodo = { existingId, title, notes, category, priority, dueDate, completed, onDone ->
                                                val toast = if (isNe) "कार्य सुरक्षित गरियो!" else "To-Do task saved!"
                                                viewModel.saveOrUpdateTodoItem(
                                                    existingTodoId = existingId,
                                                    title = title,
                                                    notes = notes,
                                                    category = category,
                                                    priority = priority,
                                                    dueDateLabel = dueDate,
                                                    completed = completed,
                                                    successToast = toast,
                                                    onDone = onDone
                                                )
                                            },
                                            onToggleTodo = viewModel::toggleTodoCompleted,
                                            onDeleteTodo = viewModel::deleteTodoItem,
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
                                            myInviteCode = myInviteCode,
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
                                            onGenerateInviteCode = {
                                                val toast = if (isNe) "नयाँ गोप्य कोड बन्यो" else "Secret invite code created"
                                                viewModel.generateOrRotateInviteCode(toast)
                                            },
                                            onRedeemInviteCode = { code, onDone ->
                                                viewModel.redeemInviteCode(code, s.activeRequestBanner, onDone)
                                            },
                                            onShowToast = viewModel::showToast,
                                            observePublicProfile = viewModel::observePublicProfileFlow,
                                            onOpenSearchById = { showSearchByIdDialog = true },
                                            bottomPadding = bottomContentPadding
                                        )
                                    }
                                    MeroTab.CULTURE -> {
                                        CultureScreen(
                                            language = lang,
                                            useBikramSambat = localPrefs.useBikramSambat,
                                            placesState = culturePlacesState,
                                            onSearchPlaces = { q ->
                                                viewModel.searchHeritagePlaces(q, isNepali = isNe)
                                            },
                                            bottomPadding = bottomContentPadding,
                                            myProfile = session.publicProfile,
                                            receivedCompliments = receivedComplimentsList,
                                            onOpenHangoutPlanner = { showHangoutPlanner = true },
                                            onSaveDailyQuestionAnswer = { ans ->
                                                val pub = session.publicProfile ?: return@CultureScreen
                                                val toast = if (isNe) "दैनिक जवाफ प्रोफाइलमा सुरक्षित गरियो!" else "Daily answer saved to your profile!"
                                                viewModel.updateVibeActivitiesAndQuestion(
                                                    currentVibeId = pub.currentVibeId,
                                                    favoriteActivities = pub.favoriteActivities,
                                                    dailyQuestionAnswer = ans,
                                                    successToast = toast
                                                )
                                            }
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
                                                val msg = if (isNe) "ब्यानर अद्यावधिक गरियो।" else "Profile banner updated!"
                                                viewModel.updateBannerData(bannerData, msg)
                                            },
                                            onUpdateProfilePhotos = { photos ->
                                                val msg = if (photos.isEmpty()) {
                                                    if (isNe) "प्रोफाइल फोटो हटाइयो (डिफल्ट अवतार सक्रिय)।" else "Profile photo removed (using default avatar)."
                                                } else {
                                                    if (isNe) "प्रोफाइल फोटो सुरक्षित गरियो!" else "Profile photo saved!"
                                                }
                                                viewModel.updateProfilePhotos(photos, msg)
                                            },
                                            onAddCustomInterest = { tag ->
                                                val msg = if (isNe) "रुचि थपियो।" else "Interest added!"
                                                viewModel.addCustomInterest(tag, msg)
                                            },
                                            onRemoveInterest = viewModel::removeInterest,
                                            onOpenSavedNotes = {
                                                requestedMomentsTab = MomentsSubTab.NOTES
                                                currentTab = MeroTab.EXPLORE
                                            },
                                            onOpenTodos = {
                                                requestedMomentsTab = MomentsSubTab.TODOS
                                                currentTab = MeroTab.EXPLORE
                                            },
                                            onEditProfile = { subRoute = SubScreenRoute.EDIT_PROFILE },
                                            onOpenSettings = { subRoute = SubScreenRoute.SETTINGS },
                                            onOpenHelpSafety = { showHelpDialog = true },
                                            onOpenAbout = { showAboutDialog = true },
                                            onShowError = viewModel::showToast,
                                            currentUid = viewModel.currentUserId,
                                            showOnlineStatus = localPrefs.showOnlineStatus,
                                            onToggleOnlineStatus = { enabled ->
                                                viewModel.setShowOnlineStatus(enabled)
                                                viewModel.setOnlinePresence(enabled)
                                            },
                                            hapticEnabled = localPrefs.hapticFeedbackEnabled,
                                            onShowToast = viewModel::showToast,
                                            bottomPadding = bottomContentPadding
                                        )
                                    }
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

                    activeCallSession?.let { call ->
                        MutualConsentCallOverlay(
                            callSession = call,
                            currentUid = viewModel.currentUserId,
                            language = lang,
                            callRingtoneId = localPrefs.callRingtoneId,
                            callCustomRingtoneUri = localPrefs.callCustomRingtoneUri,
                            hapticEnabled = localPrefs.hapticFeedbackEnabled,
                            onAcceptCall = viewModel::acceptIncomingCall,
                            onDeclineOrEndCall = viewModel::declineOrEndCall,
                            onTransmitAudioChunk = viewModel::transmitCallAudioChunk,
                            onShowError = viewModel::showToast
                        )
                    }
                }
            }
        }
    }

    // Screen 3: Interest Sent confirmation overlay
    val sentTarget = interestSentTarget
    if (sentTarget != null && subRoute == SubScreenRoute.NONE) {
        BackHandler { viewModel.clearInterestSentOverlay() }
        InterestSentScreen(
            target = sentTarget,
            language = lang,
            onViewProfile = { inspectingProfile = sentTarget },
            onKeepExploring = { viewModel.clearInterestSentOverlay() },
            onJumpToTab = { tab ->
                viewModel.clearInterestSentOverlay()
                currentTab = if (tab == MeroTab.EXPLORE) MeroTab.CULTURE else tab
            }
        )
    }

    memoriesTargetConversation?.let { conv ->
        val isUserA = conv.userAId == viewModel.currentUserId
        val partnerName = if (isUserA) conv.userBName else conv.userAName
        val memoriesFlow = remember(conv.id) { viewModel.observeTogetherMemories(conv.id) }
        val memoriesList by memoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        TogetherMemoriesSheet(
            partnerName = partnerName,
            memories = memoriesList,
            language = lang,
            isBusy = isActionBusy,
            onAddMemory = { title, noteText, img, milestoneTag ->
                val toast = if (isNe) "साझा सम्झना सुरक्षित गरियो!" else "Shared memory saved!"
                viewModel.addTogetherMemory(
                    conversationId = conv.id,
                    memberUids = conv.memberUids,
                    title = title,
                    noteText = noteText,
                    imageData = img,
                    milestoneTag = milestoneTag,
                    successToast = toast
                ) {}
            },
            onDeleteMemory = { memId ->
                viewModel.deleteTogetherMemory(conv.id, memId)
            },
            onDismiss = { memoriesTargetConversation = null }
        )
    }

    if (showHangoutPlanner) {
        HangoutPlannerModal(
            language = lang,
            currentUid = viewModel.currentUserId,
            defaultCity = session.publicProfile?.district ?: "Kathmandu",
            plannerState = hangoutPlannerState,
            savedPlans = savedPlansList,
            activeConversations = conversationsList,
            onGeneratePlan = { city, budget, groupSize, durationLabel, activities, forceFresh ->
                viewModel.generateHangoutPlan(
                    city = city,
                    budgetNpr = budget,
                    groupSize = groupSize,
                    durationLabel = durationLabel,
                    preferredActivities = activities,
                    isNepali = isNe,
                    forceFreshVariation = forceFresh
                )
            },
            isAiEnhancing = isHangoutAiEnhancing,
            onSwapStopWithBackup = viewModel::swapHangoutStopWithAlternative,
            onSavePlan = { plan ->
                val toast = if (isNe) "ह्याङआउट योजना सुरक्षित गरियो!" else "Hangout plan saved!"
                viewModel.saveCurrentHangoutPlan(plan, toast)
            },
            onDeleteSavedPlan = viewModel::deleteSavedHangoutPlan,
            onSendMeetupInvite = { conv, plan, dateLabel ->
                val isUserA = conv.userAId == viewModel.currentUserId
                val recipientUid = if (isUserA) conv.userBId else conv.userAId
                val recipientName = if (isUserA) conv.userBName else conv.userAName
                val toast = if (isNe) "भेटघाट निमन्त्रणा पठाइयो!" else "Meetup invitation sent!"
                viewModel.sendMeetupInvitation(
                    conversationId = conv.id,
                    recipientUid = recipientUid,
                    recipientName = recipientName,
                    planTitle = plan.title,
                    city = plan.city,
                    proposedDateLabel = dateLabel,
                    estimatedBudgetNpr = plan.totalEstimatedBudgetNpr,
                    stopsPreview = plan.stops.map { it.placeName }.take(4),
                    successToast = toast
                )
            },
            onAddPlanToTodo = { title, category, notes ->
                val toast = if (isNe) "ह्याङआउट योजना कार्यसूचीमा थपियो!" else "Added to your To-Do List!"
                viewModel.saveOrUpdateTodoItem(
                    existingTodoId = null,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = "MEDIUM",
                    dueDateLabel = if (isNe) "यो हप्ता" else "This week",
                    completed = false,
                    successToast = toast
                )
            },
            onDismiss = { showHangoutPlanner = false }
        )
    }

    if (showLocationPrivacySheet) {
        val pub = session.publicProfile
        if (pub != null) {
            LocationPrivacySheet(
                myProfile = pub,
                language = lang,
                onSaveLocationAndPrivacy = { enabled, coarseLat, coarseLng, province, district, publicLabel ->
                    val toast = if (isNe) "स्थान र गोपनीयता सेटिङ सुरक्षित गरियो!" else "Location & privacy settings saved!"
                    viewModel.updateLocationAndPrivacy(
                        locationSharingEnabled = enabled,
                        coarseLat = coarseLat,
                        coarseLng = coarseLng,
                        province = province,
                        district = district,
                        publicLocationLabel = publicLabel,
                        successToast = toast
                    )
                },
                onDismiss = { showLocationPrivacySheet = false }
            )
        }
    }

    if (showVibeCheckSheet) {
        val pub = session.publicProfile
        if (pub != null) {
            VibeAndActivitySheet(
                myProfile = pub,
                language = lang,
                onSave = { vibeId, acts ->
                    val toast = if (isNe) "नेपाली भाइब र गतिविधि अद्यावधिक गरियो!" else "Nepali Vibe & Activities updated!"
                    viewModel.updateVibeActivitiesAndQuestion(
                        currentVibeId = vibeId,
                        favoriteActivities = acts,
                        dailyQuestionAnswer = pub.dailyQuestionAnswer,
                        successToast = toast
                    )
                },
                onDismiss = { showVibeCheckSheet = false }
            )
        }
    }

    val meProf = session.publicProfile
    val compTarget = compatibilityTargetProfile
    if (meProf != null && compTarget != null) {
        val breakdown = remember(meProf, compTarget) {
            LocationServiceHelper.computeCompatibility(meProf, compTarget)
        }
        CompatibilityBreakdownSheet(
            targetName = compTarget.displayName,
            breakdown = breakdown,
            language = lang,
            onDismiss = { compatibilityTargetProfile = null }
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
                val msg = if (isNe) "प्रयोगकर्ता ब्लक गरियो।" else "User blocked."
                viewModel.blockUser(p.uid, msg)
            },
            myProfile = session.publicProfile,
            onSendCompliment = { compKey, compText ->
                val toast = if (isNe) "🌸 गोप्य प्रशंसा पठाइयो!" else "🌸 Secret compliment sent!"
                viewModel.sendSecretCompliment(p.uid, compKey, compText, toast)
            },
            onOpenCompatibility = {
                compatibilityTargetProfile = p
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
                val msg = if (isNe) "रिपोर्ट पठाइयो।" else "Report submitted."
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
    if (showSearchByIdDialog) {
        SearchByIdDialog(
            language = lang,
            onDismiss = { showSearchByIdDialog = false },
            onSearch = { rawUid -> viewModel.searchUserById(rawUid, isNe) },
            onProfileFound = { found -> inspectingProfile = found }
        )
    }
    if (!disclaimerDismissedThisSession && viewModel.currentUserId !in localPrefs.acceptedDisclaimerUids) {
        FirstDayCommunityDisclaimerDialog(
            language = lang,
            onAccept = {
                disclaimerDismissedThisSession = true
                viewModel.markDisclaimerAccepted()
            }
        )
    }
}
