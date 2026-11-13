package com.example.feature.interests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.domain.Conversation
import com.example.core.domain.InterestRequest
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

@Composable
fun LikesConnectionsScreen(
    currentUid: String,
    language: AppLanguage,
    incomingState: UiState<List<InterestRequest>>,
    outgoingState: UiState<List<InterestRequest>>,
    conversationsState: UiState<List<Conversation>>,
    isBusy: Boolean,
    celebrationPair: Pair<String, String>?,
    onDismissCelebration: () -> Unit,
    onApprove: (InterestRequest) -> Unit,
    onDecline: (String) -> Unit,
    onWithdraw: (String) -> Unit,
    onOpenProfileUid: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        MeroTopBar(title = s.navLikes)

        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            val pendingIncoming = (incomingState as? UiState.Success)?.data?.count { it.status == "PENDING" } ?: 0
            MeroChip(
                label = if (pendingIncoming > 0) "${s.incomingTab} ($pendingIncoming)" else s.incomingTab,
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f),
                testTag = "likes_tab_incoming"
            )
            MeroChip(
                label = s.outgoingTab,
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f),
                testTag = "likes_tab_outgoing"
            )
            MeroChip(
                label = s.chatsTab,
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f),
                testTag = "likes_tab_chats"
            )
        }

        when (selectedTab) {
            0 -> IncomingTabContent(
                state = incomingState,
                language = language,
                isBusy = isBusy,
                onApprove = onApprove,
                onDecline = onDecline,
                onOpenProfileUid = onOpenProfileUid,
                bottomPadding = bottomPadding
            )
            1 -> OutgoingTabContent(
                state = outgoingState,
                language = language,
                isBusy = isBusy,
                onWithdraw = onWithdraw,
                onOpenProfileUid = onOpenProfileUid,
                bottomPadding = bottomPadding
            )
            else -> ChatsTabContent(
                currentUid = currentUid,
                state = conversationsState,
                language = language,
                onOpenChat = onOpenChat,
                bottomPadding = bottomPadding
            )
        }
    }

    if (celebrationPair != null) {
        val (partnerName, convId) = celebrationPair
        AlertDialog(
            onDismissRequest = onDismissCelebration,
            containerColor = colors.surface,
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = s.mutualConnectionTitle,
                        style = MeroType.Title.copy(color = colors.text),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = String.format(s.mutualConnectionBody, partnerName),
                    style = MeroType.Body.copy(color = colors.textMuted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                MeroButton(
                    text = s.sayHello,
                    onClick = {
                        onDismissCelebration()
                        onOpenChat(convId)
                    },
                    testTag = "celebration_say_hello"
                )
            },
            dismissButton = {
                MeroSecondaryButton(
                    text = s.close,
                    onClick = onDismissCelebration,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "celebration_close"
                )
            }
        )
    }
}

@Composable
private fun IncomingTabContent(
    state: UiState<List<InterestRequest>>,
    language: AppLanguage,
    isBusy: Boolean,
    onApprove: (InterestRequest) -> Unit,
    onDecline: (String) -> Unit,
    onOpenProfileUid: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    when (state) {
        is UiState.Loading -> MeroLoadingState()
        is UiState.Error -> Box(Modifier.padding(MeroSpacing.lg)) {
            MeroErrorState(state.message, s.retry, {})
        }
        is UiState.Success -> {
            val pending = state.data.filter { it.status == "PENDING" }
            if (pending.isEmpty()) {
                Box(Modifier.padding(MeroSpacing.lg)) {
                    MeroEmptyState(
                        icon = Icons.Filled.Favorite,
                        title = s.emptyIncomingTitle,
                        body = s.emptyIncomingBody
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MeroSpacing.lg,
                        end = MeroSpacing.lg,
                        top = MeroSpacing.sm,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pending, key = { it.id }) { req ->
                        MeroCard(
                            onClick = { onOpenProfileUid(req.fromUid) },
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .testTag("incoming_request_${req.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroAvatarImage(req.fromPhoto, req.fromName, 56.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.fromName,
                                        style = MeroType.Heading.copy(color = colors.text),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = req.fromLocation,
                                        style = MeroType.Caption.copy(color = colors.textMuted),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroSecondaryButton(
                                    text = s.decline,
                                    leadingIcon = Icons.Filled.Close,
                                    isDanger = true,
                                    enabled = !isBusy,
                                    onClick = { onDecline(req.id) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "decline_request_${req.id}"
                                )
                                MeroButton(
                                    text = s.approve,
                                    leadingIcon = Icons.Filled.Check,
                                    enabled = !isBusy,
                                    onClick = { onApprove(req) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "approve_request_${req.id}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutgoingTabContent(
    state: UiState<List<InterestRequest>>,
    language: AppLanguage,
    isBusy: Boolean,
    onWithdraw: (String) -> Unit,
    onOpenProfileUid: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    when (state) {
        is UiState.Loading -> MeroLoadingState()
        is UiState.Error -> Box(Modifier.padding(MeroSpacing.lg)) {
            MeroErrorState(state.message, s.retry, {})
        }
        is UiState.Success -> {
            val itemsList = state.data
            if (itemsList.isEmpty()) {
                Box(Modifier.padding(MeroSpacing.lg)) {
                    MeroEmptyState(
                        icon = Icons.Filled.HourglassTop,
                        title = s.emptyOutgoingTitle,
                        body = s.emptyOutgoingBody
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MeroSpacing.lg,
                        end = MeroSpacing.lg,
                        top = MeroSpacing.sm,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(itemsList, key = { it.id }) { req ->
                        MeroCard(
                            onClick = { onOpenProfileUid(req.toUid) },
                            modifier = Modifier.widthIn(max = 560.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroAvatarImage(req.toPhoto, req.toName, 56.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.toName,
                                        style = MeroType.Heading.copy(color = colors.text),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${req.toLocation} · ${req.status}",
                                        style = MeroType.Caption.copy(
                                            color = if (req.status == "PENDING") colors.primary else colors.textMuted
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (req.status == "PENDING") {
                                Text(
                                    text = s.waitingApproval,
                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                )
                                MeroSecondaryButton(
                                    text = s.withdraw,
                                    enabled = !isBusy,
                                    onClick = { onWithdraw(req.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "withdraw_request_${req.id}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatsTabContent(
    currentUid: String,
    state: UiState<List<Conversation>>,
    language: AppLanguage,
    onOpenChat: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    when (state) {
        is UiState.Loading -> MeroLoadingState()
        is UiState.Error -> Box(Modifier.padding(MeroSpacing.lg)) {
            MeroErrorState(state.message, s.retry, {})
        }
        is UiState.Success -> {
            val chats = state.data
            if (chats.isEmpty()) {
                Box(Modifier.padding(MeroSpacing.lg)) {
                    MeroEmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = s.emptyChatsTitle,
                        body = s.emptyChatsBody
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MeroSpacing.lg,
                        end = MeroSpacing.lg,
                        top = MeroSpacing.sm,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(chats, key = { it.id }) { conv ->
                        val isA = conv.userAId == currentUid
                        val partnerName = if (isA) conv.userBName else conv.userAName
                        val partnerPhoto = if (isA) conv.userBPhoto else conv.userAPhoto
                        MeroCard(
                            onClick = { onOpenChat(conv.id) },
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .testTag("chat_thread_${conv.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroAvatarImage(partnerPhoto, partnerName, 52.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                                    ) {
                                        Text(
                                            text = partnerName,
                                            style = MeroType.Heading.copy(color = colors.text),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (conv.status == "MUTED") {
                                            Text(
                                                text = s.chatMutedBadge,
                                                style = MeroType.Caption.copy(color = colors.textMuted)
                                            )
                                        }
                                    }
                                    Text(
                                        text = conv.lastMessageText.ifBlank { s.emptyMessages },
                                        style = MeroType.Body.copy(color = colors.textMuted),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
