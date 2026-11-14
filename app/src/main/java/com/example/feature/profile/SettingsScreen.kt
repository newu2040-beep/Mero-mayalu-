package com.example.feature.profile

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.data.LocalAppPreferences
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroListItem
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.designsystem.MeroThemeMode
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.designsystem.tokensFor
import com.example.core.domain.BlockRecord
import com.example.core.domain.PrivateUser
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.notifications.MeroNotificationHelper
import com.example.core.security.BiometricLockHelper
import com.example.core.util.DataExportHelper
import com.example.core.util.ExportFormat
import com.example.core.util.ExportUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    language: AppLanguage,
    localPrefs: LocalAppPreferences,
    privateUser: PrivateUser?,
    blockedUsers: List<BlockRecord>,
    buildExportData: () -> ExportUserData,
    onBack: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeCatalog: (MeroThemeCatalog) -> Unit,
    onSelectFontCatalog: (MeroFontCatalog) -> Unit,
    onSelectThemeMode: (MeroThemeMode) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onToggleBoolean: (String, Boolean) -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onExportSuccessToast: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNe = language == AppLanguage.NEPALI
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var permissionRefreshKey by remember { mutableIntStateOf(0) }

    fun saveExportToUri(uri: android.net.Uri?, format: ExportFormat) {
        if (uri == null) return
        scope.launch {
            val data = buildExportData()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        DataExportHelper.writeToStream(format, data, out)
                    } ?: error("OutputStream unavailable")
                    true
                }.getOrDefault(false)
            }
            onExportSuccessToast(
                if (ok) {
                    if (isNe) "तपाईंको डेटा (${format.extension.uppercase()}) निर्यात गरियो।"
                    else "Your ${format.extension.uppercase()} export is saved!"
                } else {
                    if (isNe) "फाइल सुरक्षित गर्न सकिएन।"
                    else "Could not write export file."
                }
            )
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)
    ) { uri -> saveExportToUri(uri, ExportFormat.PDF) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)
    ) { uri -> saveExportToUri(uri, ExportFormat.CSV) }

    val txtExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.TXT.mimeType)
    ) { uri -> saveExportToUri(uri, ExportFormat.TXT) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.JSON.mimeType)
    ) { uri -> saveExportToUri(uri, ExportFormat.JSON) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRefreshKey += 1
        onToggleBoolean("notificationsEnabled", granted)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permissionRefreshKey += 1 }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permissionRefreshKey += 1 }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permissionRefreshKey += 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        MeroTopBar(
            title = s.settings,
            leadingAction = {
                MeroIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.back,
                    onClick = onBack,
                    testTag = "settings_back_button"
                )
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(MeroSpacing.lg)
        ) {
            // Themes, Fonts & Language
            MeroCard {
                Text(text = s.languageLabel, style = MeroType.Heading.copy(color = colors.text))
                Row(horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEach { lang ->
                        MeroChip(
                            label = lang.label,
                            selected = language == lang,
                            onClick = { onSelectLanguage(lang) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_lang_${lang.code}"
                        )
                    }
                }

                Text(text = s.themeCatalogLabel, style = MeroType.Heading.copy(color = colors.text))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                ) {
                    MeroThemeCatalog.entries.forEach { cat ->
                        val selected = localPrefs.themeCatalog == cat
                        val previewPrimary = tokensFor(cat, dark = colors.isDark).primary
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(MeroRadius.pill))
                                .background(if (selected) colors.primary else colors.surface2)
                                .border(
                                    1.dp,
                                    if (selected) colors.primary else colors.border,
                                    RoundedCornerShape(MeroRadius.pill)
                                )
                                .clickable { onSelectThemeCatalog(cat) }
                                .padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.onPrimary else previewPrimary)
                            )
                            Spacer(Modifier.width(MeroSpacing.xs))
                            Text(
                                text = if (isNe) cat.labelNe else cat.labelEn,
                                style = MeroType.Caption.copy(
                                    color = if (selected) colors.onPrimary else colors.text,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(text = s.fontCatalogLabel, style = MeroType.Heading.copy(color = colors.text))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                ) {
                    MeroFontCatalog.entries.forEach { font ->
                        val selected = localPrefs.fontCatalog == font
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.pill),
                            color = if (selected) colors.primary else colors.surface2,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(MeroRadius.pill))
                                .clickable { onSelectFontCatalog(font) }
                        ) {
                            Text(
                                text = if (isNe) font.labelNe else font.labelEn,
                                style = MeroType.Caption.copy(
                                    fontFamily = font.fontFamily,
                                    color = if (selected) colors.onPrimary else colors.text,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                            )
                        }
                    }
                }

                Text(text = s.themeModeLabel, style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium))
                Row(horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    MeroChip(
                        label = s.modeSystem,
                        selected = localPrefs.themeMode == MeroThemeMode.SYSTEM,
                        onClick = { onSelectThemeMode(MeroThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    MeroChip(
                        label = s.modeLight,
                        selected = localPrefs.themeMode == MeroThemeMode.LIGHT,
                        onClick = { onSelectThemeMode(MeroThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    MeroChip(
                        label = s.modeDark,
                        selected = localPrefs.themeMode == MeroThemeMode.DARK,
                        onClick = { onSelectThemeMode(MeroThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Native Android System Biometric / Device PIN App Lock
            MeroCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(text = s.appLockTitle, style = MeroType.Heading.copy(color = colors.text))
                }
                ToggleRow(
                    label = s.appLockToggle,
                    checked = localPrefs.appLockEnabled,
                    onCheckedChange = { enable ->
                        val activity = context.findFragmentActivity()
                        if (activity == null) {
                            onExportSuccessToast(
                                if (isNe) "यन्त्र प्रमाणीकरण उपलब्ध छैन।" else "Device authentication is unavailable here."
                            )
                            return@ToggleRow
                        }
                        if (!BiometricLockHelper.canAuthenticate(context)) {
                            onExportSuccessToast(
                                if (isNe) "पहिले फोनको सेटिङमा PIN, Pattern वा Fingerprint सेट गर्नुहोस्।"
                                else "Set up fingerprint, face, or device PIN in Android Settings first."
                            )
                            return@ToggleRow
                        }
                        BiometricLockHelper.authenticate(
                            activity = activity,
                            title = s.appLockTitle,
                            subtitle = if (enable) s.appLockToggle else s.unlockAppButton,
                            description = s.appLockSubtitle,
                            onSuccess = { onToggleAppLock(enable) },
                            onError = { err -> onExportSuccessToast(err) }
                        )
                    }
                )
                Text(
                    text = s.appLockSubtitle,
                    style = MeroType.Caption.copy(color = colors.textMuted)
                )
            }

            // Preferences & Privacy Controls
            MeroCard {
                ToggleRow(
                    label = s.bikramSambatToggle,
                    checked = localPrefs.useBikramSambat,
                    onCheckedChange = { onToggleBoolean("useBikramSambat", it) }
                )
                ToggleRow(
                    label = s.reducedMotionToggle,
                    checked = localPrefs.reducedMotion,
                    onCheckedChange = { onToggleBoolean("reducedMotion", it) }
                )
                ToggleRow(
                    label = s.visibilityToggle,
                    checked = privateUser?.visibility != "hidden",
                    onCheckedChange = onToggleVisibility
                )
                ToggleRow(
                    label = s.notificationsToggle,
                    checked = privateUser?.notificationsEnabled ?: true,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onToggleBoolean("notificationsEnabled", true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onToggleBoolean("notificationsEnabled", enable)
                        }
                    }
                )
                ToggleRow(
                    label = s.discreetNotificationsToggle,
                    checked = privateUser?.discreetNotifications ?: true,
                    onCheckedChange = { onToggleBoolean("discreetNotifications", it) }
                )
                ToggleRow(
                    label = s.quietHoursToggle,
                    checked = privateUser?.quietHoursEnabled ?: false,
                    onCheckedChange = { onToggleBoolean("quietHoursEnabled", it) }
                )
            }

            // Permissions & System Access Hub
            MeroCard {
                Text(text = s.permissionsHubTitle, style = MeroType.Heading.copy(color = colors.text))
                val key = permissionRefreshKey
                val camGranted = remember(key) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                }
                val micGranted = remember(key) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                }
                val locGranted = remember(key) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                }
                val notifGranted = remember(key) {
                    MeroNotificationHelper.hasNotificationPermission(context)
                }

                PermissionStatusRow(
                    icon = Icons.Filled.CameraAlt,
                    label = if (isNe) "क्यामेरा (फोटो र विचार)" else "Camera (Photos & Moments)",
                    granted = camGranted,
                    grantedText = s.permissionGranted,
                    grantText = s.permissionRequest,
                    onGrant = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
                PermissionStatusRow(
                    icon = Icons.Filled.Mic,
                    label = if (isNe) "माइक्रोफोन (आवाज सन्देश)" else "Microphone (Voice Notes)",
                    granted = micGranted,
                    grantedText = s.permissionGranted,
                    grantText = s.permissionRequest,
                    onGrant = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
                PermissionStatusRow(
                    icon = Icons.Filled.LocationOn,
                    label = if (isNe) "शहर पत्ता (अनुमानात्मक स्थान)" else "Coarse City Location",
                    granted = locGranted,
                    grantedText = s.permissionGranted,
                    grantText = s.permissionRequest,
                    onGrant = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                )
                PermissionStatusRow(
                    icon = Icons.Filled.Notifications,
                    label = if (isNe) "पुश सूचनाहरू" else "Push Notifications",
                    granted = notifGranted,
                    grantedText = s.permissionGranted,
                    grantText = s.permissionRequest,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onToggleBoolean("notificationsEnabled", true)
                        }
                    }
                )
                PermissionStatusRow(
                    icon = Icons.Filled.FolderOpen,
                    label = if (isNe) "फोटो र ग्यालरी फोल्डर (Zero-Permission Picker)"
                    else "Gallery & All Folders (System Photo & Folder Picker)",
                    granted = true,
                    grantedText = s.permissionGranted,
                    grantText = s.permissionGranted,
                    onGrant = {}
                )
            }

            // Blocked Users
            if (blockedUsers.isNotEmpty()) {
                MeroCard {
                    Text(text = s.blockedUsersLabel, style = MeroType.Heading.copy(color = colors.text))
                    blockedUsers.forEach { block ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = block.blockedUid.take(12) + "…",
                                style = MeroType.Body.copy(color = colors.textMuted),
                                modifier = Modifier.weight(1f)
                            )
                            MeroSecondaryButton(
                                text = s.unblock,
                                onClick = { onUnblockUser(block.blockedUid) }
                            )
                        }
                    }
                }
            }

            // Account Actions (Export, Sign Out, Delete)
            MeroCard {
                MeroListItem(
                    title = s.exportDataLabel,
                    leadingIcon = Icons.Filled.Download,
                    onClick = { showExportDialog = true },
                    testTag = "settings_export_data"
                )
                MeroListItem(
                    title = s.signOutLabel,
                    leadingIcon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = onSignOut,
                    testTag = "settings_sign_out"
                )
                MeroListItem(
                    title = s.deleteAccountLabel,
                    leadingIcon = Icons.Filled.DeleteForever,
                    isDanger = true,
                    onClick = { showDeleteConfirm = true },
                    testTag = "settings_delete_account"
                )
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = s.exportDataLabel,
                    style = MeroType.Heading.copy(color = colors.text)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                    Text(
                        text = if (isNe) "आफ्नो डेटा निर्यात गर्न फाइल ढाँचा छान्नुहोस्:" else "Choose a file format for your personal data export:",
                        style = MeroType.Body.copy(color = colors.textMuted)
                    )
                    ExportFormat.entries.forEach { fmt ->
                        MeroSecondaryButton(
                            text = if (isNe) fmt.labelNe else fmt.labelEn,
                            leadingIcon = Icons.Filled.Download,
                            onClick = {
                                showExportDialog = false
                                val fileName = "mero-mayalu-export.${fmt.extension}"
                                when (fmt) {
                                    ExportFormat.PDF -> pdfExportLauncher.launch(fileName)
                                    ExportFormat.CSV -> csvExportLauncher.launch(fileName)
                                    ExportFormat.TXT -> txtExportLauncher.launch(fileName)
                                    ExportFormat.JSON -> jsonExportLauncher.launch(fileName)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "export_format_${fmt.extension}"
                        )
                    }
                }
            },
            confirmButton = {
                MeroButton(
                    text = s.cancel,
                    onClick = { showExportDialog = false }
                )
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            title = { Text(s.deleteAccountLabel, style = MeroType.Heading.copy(color = colors.danger)) },
            text = { Text(s.deleteAccountConfirm, style = MeroType.Body.copy(color = colors.text)) },
            confirmButton = {
                MeroButton(
                    text = s.confirm,
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    testTag = "confirm_delete_account"
                )
            },
            dismissButton = {
                MeroSecondaryButton(
                    text = s.cancel,
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalMeroColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MeroType.Body.copy(color = colors.text),
            modifier = Modifier
                .weight(1f)
                .padding(end = MeroSpacing.sm)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    grantedText: String,
    grantText: String,
    onGrant: () -> Unit
) {
    val colors = LocalMeroColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MeroType.Caption.copy(color = colors.text),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(MeroRadius.pill),
            color = if (granted) colors.success.copy(alpha = 0.16f) else colors.primary,
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(RoundedCornerShape(MeroRadius.pill))
                .clickable(enabled = !granted, onClick = onGrant)
        ) {
            Text(
                text = if (granted) grantedText else grantText,
                style = MeroType.Caption.copy(
                    color = if (granted) colors.success else colors.onPrimary,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
            )
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
