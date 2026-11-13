package com.example.feature.moments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.CulturePost
import com.example.core.domain.SavedNote
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.MediaHelper
import com.example.core.util.NepaliCultureCatalog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMomentSheet(
    language: AppLanguage,
    initialThought: String = "",
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onPublish: (thoughtText: String, imageData: String, culturalTag: String) -> Unit,
    onShowError: (String) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var thoughtText by remember(initialThought) { mutableStateOf(initialThought) }
    var selectedTag by remember {
        val first = NepaliCultureCatalog.thoughtTags.first()
        mutableStateOf(if (isNe) first.second else first.first)
    }
    var customTag by remember { mutableStateOf("") }
    var imageData by remember { mutableStateOf("") }
    var icebreakerIdx by remember { mutableIntStateOf(0) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) imageData = compressed
                else onShowError(if (isNe) "फोटो लोड गर्न सकिएन।" else "Could not process image.")
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) imageData = compressed
                else onShowError(if (isNe) "फोटो लोड गर्न सकिएन।" else "Could not read selected file.")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) imageData = compressed
            }
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "moment_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else onShowError(if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is needed to take a photo.")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Text(
                text = s.shareThoughtCta,
                style = MeroType.Heading.copy(color = colors.text)
            )

            MeroInput(
                label = if (isNe) "तपाईंको विचार वा सन्देश" else "Your thought or cultural story",
                value = thoughtText,
                onValueChange = { if (it.length <= 600) thoughtText = it },
                placeholder = s.thoughtPlaceholder,
                singleLine = false,
                maxLines = 5,
                testTag = "moment_thought_input"
            )

            MeroSecondaryButton(
                text = s.icebreakerButton,
                leadingIcon = Icons.Filled.AutoAwesome,
                onClick = {
                    val item = NepaliCultureCatalog.icebreakers[icebreakerIdx % NepaliCultureCatalog.icebreakers.size]
                    icebreakerIdx += 1
                    thoughtText = if (isNe) item.second else item.first
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "moment_icebreaker_button"
            )

            Text(
                text = if (isNe) "सांस्कृतिक ट्याग" else "Cultural Tag",
                style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                NepaliCultureCatalog.thoughtTags.forEach { (en, ne) ->
                    val label = if (isNe) ne else en
                    MeroChip(
                        label = label,
                        selected = selectedTag == label && customTag.isBlank(),
                        onClick = {
                            selectedTag = label
                            customTag = ""
                        }
                    )
                }
            }
            MeroInput(
                label = if (isNe) "वा आफ्नै ट्याग लेख्नुहोस्" else "Or write a custom tag",
                value = customTag,
                onValueChange = { if (it.length <= 40) customTag = it },
                placeholder = if (isNe) "जस्तै: पोखरा साँझ" else "e.g., Kathmandu Chiya",
                testTag = "moment_custom_tag_input"
            )

            if (imageData.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .clip(RoundedCornerShape(MeroRadius.card))
                ) {
                    MeroPhotoBox(
                        photoData = imageData,
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MeroSpacing.sm)
                            .clip(RoundedCornerShape(MeroRadius.pill))
                            .background(colors.bg.copy(alpha = 0.85f))
                    ) {
                        MeroIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = s.cancel,
                            tint = colors.danger,
                            onClick = { imageData = "" }
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroSecondaryButton(
                    text = s.selectPhoto,
                    leadingIcon = Icons.Filled.AddAPhoto,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_pick_photo"
                )
                MeroSecondaryButton(
                    text = s.browseFoldersPhoto,
                    leadingIcon = Icons.Filled.FolderOpen,
                    onClick = { folderPickerLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_browse_folders"
                )
                MeroSecondaryButton(
                    text = s.takePhoto,
                    leadingIcon = Icons.Filled.CameraAlt,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera()
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_take_photo"
                )
            }

            val resolvedTag = customTag.trim().ifBlank { selectedTag }
            val canPublish = thoughtText.trim().isNotBlank() || imageData.isNotBlank()
            MeroButton(
                text = if (isNe) "पोस्ट प्रकाशित गर्नुहोस्" else "Publish Cultural Moment",
                enabled = canPublish && !isBusy,
                isLoading = isBusy,
                onClick = {
                    onPublish(thoughtText.trim(), imageData, resolvedTag)
                },
                testTag = "moment_publish_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveNoteSheet(
    language: AppLanguage,
    sourcePost: CulturePost? = null,
    editingNote: SavedNote? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        noteText: String,
        sourcePostId: String,
        sourceAuthor: String,
        sourceExcerpt: String,
        imageData: String
    ) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var title by remember(sourcePost, editingNote) {
        mutableStateOf(
            editingNote?.title
                ?: sourcePost?.culturalTag?.ifBlank { sourcePost.authorName }.orEmpty()
        )
    }
    var noteText by remember(sourcePost, editingNote) {
        mutableStateOf(
            editingNote?.noteText
                ?: if (sourcePost != null && sourcePost.thoughtText.isBlank()) ""
                else editingNote?.noteText.orEmpty()
        )
    }

    val authorName = editingNote?.sourceAuthorName ?: sourcePost?.authorName.orEmpty()
    val excerpt = editingNote?.sourceExcerpt ?: sourcePost?.thoughtText.orEmpty()
    val postId = editingNote?.sourcePostId ?: sourcePost?.id.orEmpty()
    val img = editingNote?.imageData ?: sourcePost?.imageData.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    text = if (editingNote != null) {
                        if (isNe) "नोट सम्पादन गर्नुहोस्" else "Edit Saved Note"
                    } else if (sourcePost != null) {
                        s.savePostNote
                    } else {
                        s.addPersonalNote
                    },
                    style = MeroType.Heading.copy(color = colors.text)
                )
            }

            if (excerpt.isNotBlank() || authorName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        if (authorName.isNotBlank()) {
                            Text(
                                text = authorName,
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                        }
                        if (excerpt.isNotBlank()) {
                            Text(
                                text = excerpt,
                                style = MeroType.Caption.copy(color = colors.textMuted),
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            MeroInput(
                label = if (isNe) "नोट शीर्षक" else "Note Title",
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                placeholder = if (isNe) "जस्तै: तिहारको सम्झना" else "e.g., Favorite Chiya Spot Note",
                testTag = "note_title_input"
            )

            MeroInput(
                label = if (isNe) "तपाईंको व्यक्तिगत नोट" else "Your Personal Note",
                value = noteText,
                onValueChange = { if (it.length <= 1000) noteText = it },
                placeholder = if (isNe) "आफूलाई मन परेको कुरा वा विचार लेख्नुहोस्..." else "Write your thoughts, memories, or cultural reflection...",
                singleLine = false,
                maxLines = 6,
                testTag = "note_body_input"
            )

            MeroButton(
                text = s.saveChanges,
                leadingIcon = Icons.Filled.BookmarkAdd,
                enabled = noteText.trim().isNotEmpty() && !isBusy,
                isLoading = isBusy,
                onClick = {
                    onSave(
                        editingNote?.id,
                        title.trim().ifBlank { if (isNe) "मेरो नोट" else "Cultural Note" },
                        noteText.trim(),
                        postId,
                        authorName,
                        excerpt,
                        img
                    )
                },
                testTag = "note_save_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}
