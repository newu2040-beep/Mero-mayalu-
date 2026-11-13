package com.example.feature.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.domain.CulturePost
import com.example.core.domain.SavedNote
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.BikramSambatConverter
import com.example.core.util.NepaliCultureCatalog
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class MomentsSubTab { FEED, NOTES }

@Composable
fun MomentsScreen(
    currentUid: String,
    language: AppLanguage,
    useBikramSambat: Boolean,
    requestedSubTab: MomentsSubTab?,
    onConsumedRequestedTab: () -> Unit,
    postsState: UiState<List<CulturePost>>,
    notesState: UiState<List<SavedNote>>,
    isBusy: Boolean,
    observeReactions: (String) -> Flow<List<String>>,
    onToggleReaction: (String, Boolean) -> Unit,
    onCreatePost: (thoughtText: String, imageData: String, culturalTag: String, onDone: () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onSaveNote: (
        existingId: String?,
        title: String,
        noteText: String,
        sourcePostId: String,
        sourceAuthor: String,
        sourceExcerpt: String,
        imageData: String,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteNote: (String) -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onShowError: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var activeTab by rememberSaveable { mutableStateOf(MomentsSubTab.FEED) }
    LaunchedEffect(requestedSubTab) {
        if (requestedSubTab != null) {
            activeTab = requestedSubTab
            onConsumedRequestedTab()
        }
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var composerInitialThought by remember { mutableStateOf("") }
    var icebreakerIdx by remember { mutableIntStateOf(0) }
    var noteTargetPost by remember { mutableStateOf<CulturePost?>(null) }
    var editingNote by remember { mutableStateOf<SavedNote?>(null) }
    var showBlankNoteSheet by remember { mutableStateOf(false) }

    val nextFestival = remember { BikramSambatConverter.upcomingFestivalsWithCountdown().firstOrNull() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        MeroTopBar(
            title = s.momentsTitle,
            showLogo = true,
            trailingActions = {
                MeroIconButton(
                    icon = if (activeTab == MomentsSubTab.FEED) Icons.Filled.Add else Icons.Filled.NoteAdd,
                    contentDescription = if (activeTab == MomentsSubTab.FEED) s.shareThoughtCta else s.addPersonalNote,
                    tint = colors.primary,
                    containerColor = colors.surface2,
                    onClick = {
                        if (activeTab == MomentsSubTab.FEED) {
                            composerInitialThought = ""
                            showCreateSheet = true
                        } else {
                            editingNote = null
                            noteTargetPost = null
                            showBlankNoteSheet = true
                        }
                    },
                    testTag = "moments_topbar_add_button"
                )
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            MeroChip(
                label = s.momentsFeedTab,
                selected = activeTab == MomentsSubTab.FEED,
                onClick = { activeTab = MomentsSubTab.FEED },
                modifier = Modifier.weight(1f),
                testTag = "moments_tab_feed"
            )
            MeroChip(
                label = s.savedNotesTab,
                selected = activeTab == MomentsSubTab.NOTES,
                onClick = { activeTab = MomentsSubTab.NOTES },
                modifier = Modifier.weight(1f),
                testTag = "moments_tab_notes"
            )
        }

        if (nextFestival != null) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.small),
                color = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Celebration,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    val festName = if (isNe) nextFestival.festival.nameNe else nextFestival.festival.nameEn
                    val countdownText = if (nextFestival.daysRemaining == 0L) {
                        if (isNe) "$festName आज!" else "$festName is today!"
                    } else if (isNe) {
                        "$festName आउन ${nextFestival.daysRemaining} दिन बाँकी"
                    } else {
                        "$festName in ${nextFestival.daysRemaining} days"
                    }
                    Text(
                        text = countdownText,
                        style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when (activeTab) {
            MomentsSubTab.FEED -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MeroSpacing.lg,
                        end = MeroSpacing.lg,
                        top = MeroSpacing.sm,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                            MeroButton(
                                text = s.shareThoughtCta,
                                leadingIcon = Icons.Filled.LocalFlorist,
                                onClick = {
                                    composerInitialThought = ""
                                    showCreateSheet = true
                                },
                                testTag = "moments_open_composer"
                            )
                            MeroSecondaryButton(
                                text = s.icebreakerButton,
                                leadingIcon = Icons.Filled.AutoAwesome,
                                onClick = {
                                    val idea = NepaliCultureCatalog.icebreakers[icebreakerIdx % NepaliCultureCatalog.icebreakers.size]
                                    icebreakerIdx += 1
                                    composerInitialThought = if (isNe) idea.second else idea.first
                                    showCreateSheet = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "moments_try_icebreaker"
                            )
                        }
                    }

                    when (postsState) {
                        is UiState.Loading -> item { MeroLoadingState() }
                        is UiState.Error -> item {
                            MeroErrorState(
                                message = postsState.message,
                                retryLabel = s.retry,
                                onRetry = {},
                                modifier = Modifier.widthIn(max = 560.dp)
                            )
                        }
                        is UiState.Success -> {
                            if (postsState.data.isEmpty()) {
                                item {
                                    MeroEmptyState(
                                        icon = Icons.Filled.LocalFlorist,
                                        title = s.emptyMomentsTitle,
                                        body = s.emptyMomentsBody,
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    )
                                }
                            } else {
                                items(postsState.data, key = { it.id }) { post ->
                                    val reactions by remember(post.id) { observeReactions(post.id) }
                                        .collectAsStateWithLifecycle(initialValue = emptyList())
                                    CulturePostCard(
                                        post = post,
                                        isOwner = post.authorUid == currentUid,
                                        reactionCount = reactions.size,
                                        isLikedByMe = currentUid in reactions,
                                        language = language,
                                        useBikramSambat = useBikramSambat,
                                        onToggleLike = { onToggleReaction(post.id, currentUid in reactions) },
                                        onSaveToNote = { noteTargetPost = post },
                                        onDelete = { onDeletePost(post.id) },
                                        onAuthorClick = {
                                            if (post.authorUid != currentUid) onOpenAuthorProfile(post.authorUid)
                                        },
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MomentsSubTab.NOTES -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MeroSpacing.lg,
                        end = MeroSpacing.lg,
                        top = MeroSpacing.sm,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(modifier = Modifier.widthIn(max = 560.dp)) {
                            MeroButton(
                                text = s.addPersonalNote,
                                leadingIcon = Icons.Filled.NoteAdd,
                                onClick = {
                                    editingNote = null
                                    noteTargetPost = null
                                    showBlankNoteSheet = true
                                },
                                testTag = "notes_add_new_button"
                            )
                        }
                    }

                    when (notesState) {
                        is UiState.Loading -> item { MeroLoadingState() }
                        is UiState.Error -> item {
                            MeroErrorState(
                                message = notesState.message,
                                retryLabel = s.retry,
                                onRetry = {},
                                modifier = Modifier.widthIn(max = 560.dp)
                            )
                        }
                        is UiState.Success -> {
                            if (notesState.data.isEmpty()) {
                                item {
                                    MeroEmptyState(
                                        icon = Icons.Filled.BookmarkAdd,
                                        title = s.emptyNotesTitle,
                                        body = s.emptyNotesBody,
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    )
                                }
                            } else {
                                items(notesState.data, key = { it.id }) { note ->
                                    SavedNoteCard(
                                        note = note,
                                        language = language,
                                        useBikramSambat = useBikramSambat,
                                        onEdit = { editingNote = note },
                                        onDelete = { onDeleteNote(note.id) },
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateMomentSheet(
            language = language,
            initialThought = composerInitialThought,
            isBusy = isBusy,
            onDismiss = { showCreateSheet = false },
            onPublish = { thought, image, tag ->
                onCreatePost(thought, image, tag) { showCreateSheet = false }
            },
            onShowError = onShowError
        )
    }

    if (noteTargetPost != null || editingNote != null || showBlankNoteSheet) {
        SaveNoteSheet(
            language = language,
            sourcePost = noteTargetPost,
            editingNote = editingNote,
            isBusy = isBusy,
            onDismiss = {
                noteTargetPost = null
                editingNote = null
                showBlankNoteSheet = false
            },
            onSave = { existingId, title, body, postId, author, excerpt, img ->
                onSaveNote(existingId, title, body, postId, author, excerpt, img) {
                    noteTargetPost = null
                    editingNote = null
                    showBlankNoteSheet = false
                }
            }
        )
    }
}

@Composable
private fun CulturePostCard(
    post: CulturePost,
    isOwner: Boolean,
    reactionCount: Int,
    isLikedByMe: Boolean,
    language: AppLanguage,
    useBikramSambat: Boolean,
    onToggleLike: () -> Unit,
    onSaveToNote: () -> Unit,
    onDelete: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val dateLabel = remember(post.createdAt, useBikramSambat, isNe) {
        val date = post.createdAt?.toDate()
        if (date == null) "" else if (useBikramSambat) {
            val localDate = Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
            BikramSambatConverter.fromLocalDate(localDate)?.format(isNe)
                ?: SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
        }
    }

    MeroCard(modifier = modifier.testTag("moment_card_${post.id}")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick)
            ) {
                MeroAvatarImage(
                    photoData = post.authorPhoto,
                    displayName = post.authorName,
                    size = 44.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOfNotNull(
                        post.authorLocation.takeIf { it.isNotBlank() },
                        dateLabel.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MeroType.Caption.copy(color = colors.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (isOwner) {
                MeroIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = s.confirm,
                    tint = colors.danger,
                    onClick = onDelete,
                    testTag = "moment_delete_${post.id}"
                )
            }
        }

        if (post.culturalTag.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.pill),
                color = colors.surface2
            ) {
                Text(
                    text = post.culturalTag,
                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                )
            }
        }

        if (post.thoughtText.isNotBlank()) {
            Text(
                text = post.thoughtText,
                style = MeroType.Body.copy(color = colors.text)
            )
        }

        if (post.imageData.isNotBlank()) {
            MeroPhotoBox(
                photoData = post.imageData,
                contentDescription = post.thoughtText.ifBlank { post.authorName },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(MeroRadius.card))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MeroIconButton(
                    icon = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = s.interested,
                    tint = if (isLikedByMe) colors.primary else colors.textMuted,
                    onClick = onToggleLike,
                    testTag = "moment_like_${post.id}"
                )
                Text(
                    text = if (reactionCount > 0) "$reactionCount" else "",
                    style = MeroType.Caption.copy(
                        color = if (isLikedByMe) colors.primary else colors.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            MeroSecondaryButton(
                text = s.savePostNote,
                leadingIcon = Icons.Filled.BookmarkAdd,
                onClick = onSaveToNote,
                testTag = "moment_save_note_${post.id}"
            )
        }
    }
}

@Composable
private fun SavedNoteCard(
    note: SavedNote,
    language: AppLanguage,
    useBikramSambat: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val dateLabel = remember(note.updatedAt, useBikramSambat, isNe) {
        val date = (note.updatedAt ?: note.createdAt)?.toDate()
        if (date == null) "" else if (useBikramSambat) {
            val localDate = Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
            BikramSambatConverter.fromLocalDate(localDate)?.format(isNe)
                ?: SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    }

    MeroCard(modifier = modifier.testTag("saved_note_card_${note.id}")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { if (isNe) "सांस्कृतिक नोट" else "Cultural Note" },
                    style = MeroType.Heading.copy(color = colors.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dateLabel.isNotBlank()) {
                    Text(
                        text = dateLabel,
                        style = MeroType.Caption.copy(color = colors.textMuted)
                    )
                }
            }
            Row {
                MeroIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = s.editProfile,
                    tint = colors.primary,
                    onClick = onEdit,
                    testTag = "note_edit_${note.id}"
                )
                MeroIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = s.confirm,
                    tint = colors.danger,
                    onClick = onDelete,
                    testTag = "note_delete_${note.id}"
                )
            }
        }

        if (note.sourceAuthorName.isNotBlank() || note.sourceExcerpt.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.small),
                color = colors.surface2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    if (note.sourceAuthorName.isNotBlank()) {
                        Text(
                            text = if (isNe) "${note.sourceAuthorName} को पोस्टबाट" else "From ${note.sourceAuthorName}'s post",
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                        )
                    }
                    if (note.sourceExcerpt.isNotBlank()) {
                        Text(
                            text = note.sourceExcerpt,
                            style = MeroType.Caption.copy(color = colors.textMuted),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (note.imageData.isNotBlank()) {
            MeroPhotoBox(
                photoData = note.imageData,
                contentDescription = note.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(MeroRadius.card))
            )
        }

        Text(
            text = note.noteText,
            style = MeroType.Body.copy(color = colors.text)
        )
    }
}
