package com.example.feature.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroLotusEmblem
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroTab
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.domain.PublicProfile
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.feature.main.DiscoverySubTab

@Composable
fun DiscoveryScreen(
    language: AppLanguage,
    subTab: DiscoverySubTab,
    feedState: UiState<List<PublicProfile>>,
    hasPendingOutgoing: Boolean,
    incomingCount: Int,
    onSelectSubTab: (DiscoverySubTab) -> Unit,
    onOpenNotifications: () -> Unit,
    onViewProfile: (PublicProfile) -> Unit,
    onNotInterested: (PublicProfile) -> Unit,
    onInterested: (PublicProfile) -> Unit,
    onReportUser: (PublicProfile) -> Unit,
    onBlockUser: (PublicProfile) -> Unit,
    onRetry: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        MeroTopBar(
            title = s.appName,
            showLogo = true,
            trailingActions = {
                MeroIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = s.navLikes,
                    badgeCount = incomingCount,
                    onClick = onOpenNotifications,
                    testTag = "discovery_notifications_button"
                )
            }
        )

        // Pill tabs: Nearby, For You, Heritage
        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            MeroChip(
                label = s.tabNearby,
                selected = subTab == DiscoverySubTab.NEARBY,
                onClick = { onSelectSubTab(DiscoverySubTab.NEARBY) },
                modifier = Modifier.weight(1f),
                testTag = "subtab_nearby"
            )
            MeroChip(
                label = s.tabForYou,
                selected = subTab == DiscoverySubTab.FOR_YOU,
                onClick = { onSelectSubTab(DiscoverySubTab.FOR_YOU) },
                modifier = Modifier.weight(1f),
                testTag = "subtab_for_you"
            )
            MeroChip(
                label = s.tabAllNepal,
                selected = subTab == DiscoverySubTab.HERITAGE,
                onClick = { onSelectSubTab(DiscoverySubTab.HERITAGE) },
                modifier = Modifier.weight(1f),
                testTag = "subtab_heritage"
            )
        }

        AnimatedVisibility(
            visible = hasPendingOutgoing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.small),
                color = colors.surface2,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Text(
                    text = s.activeRequestBanner,
                    style = MeroType.Caption.copy(color = colors.text),
                    modifier = Modifier.padding(MeroSpacing.md)
                )
            }
        }

        when (feedState) {
            is UiState.Loading -> {
                MeroLoadingState(
                    message = if (language == AppLanguage.NEPALI) "प्रोफाइलहरू लोड हुँदैछन्..." else "Loading eligible profiles..."
                )
            }
            is UiState.Error -> {
                Box(modifier = Modifier.padding(MeroSpacing.lg)) {
                    MeroErrorState(
                        message = feedState.message,
                        retryLabel = s.retry,
                        onRetry = onRetry
                    )
                }
            }
            is UiState.Success -> {
                val profiles = feedState.data
                if (profiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MeroSpacing.lg)
                    ) {
                        MeroEmptyState(
                            icon = Icons.Filled.Explore,
                            title = s.emptyDiscoveryTitle,
                            body = s.emptyDiscoveryBody,
                            actionLabel = if (subTab != DiscoverySubTab.FOR_YOU) s.resetFilters else null,
                            onAction = if (subTab != DiscoverySubTab.FOR_YOU) {
                                { onSelectSubTab(DiscoverySubTab.FOR_YOU) }
                            } else null
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
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(profiles, key = { it.uid }) { profile ->
                            DiscoveryProfileCard(
                                profile = profile,
                                language = language,
                                canSendInterest = !hasPendingOutgoing,
                                onViewProfile = { onViewProfile(profile) },
                                onNotInterested = { onNotInterested(profile) },
                                onInterested = { onInterested(profile) },
                                onReport = { onReportUser(profile) },
                                onBlock = { onBlockUser(profile) },
                                modifier = Modifier.widthIn(max = 560.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryProfileCard(
    profile: PublicProfile,
    language: AppLanguage,
    canSendInterest: Boolean,
    onViewProfile: () -> Unit,
    onNotInterested: () -> Unit,
    onInterested: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    var photoIndex by remember(profile.uid) { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val photos = profile.photoDataList
    val activePhoto = photos.getOrNull(photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))

    MeroCard(
        contentPadding = MeroSpacing.md,
        modifier = modifier.testTag("discovery_card_${profile.uid}")
    ) {
        // Photo Hero Box matching Screen 2 card layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.84f)
                .clip(RoundedCornerShape(MeroRadius.card))
                .clickable {
                    if (photos.size > 1) {
                        photoIndex = (photoIndex + 1) % photos.size
                    } else {
                        onViewProfile()
                    }
                }
        ) {
            MeroPhotoBox(
                photoData = activePhoto,
                contentDescription = profile.displayName,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient scrim for text legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xD91A0C13))
                        )
                    )
            )

            // Top right controls: photo counter pill + overflow menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MeroSpacing.sm)
            ) {
                if (photos.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "${photoIndex + 1}/${photos.size}",
                            style = MeroType.Caption.copy(color = Color.White),
                            modifier = Modifier.padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.xs)
                        )
                    }
                }
                Box {
                    MeroIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = s.reportUser,
                        tint = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        onClick = { menuExpanded = true },
                        testTag = "card_menu_${profile.uid}"
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.viewProfile, style = MeroType.Body) },
                            onClick = { menuExpanded = false; onViewProfile() }
                        )
                        DropdownMenuItem(
                            text = { Text(s.reportUser, style = MeroType.Body) },
                            onClick = { menuExpanded = false; onReport() }
                        )
                        DropdownMenuItem(
                            text = { Text(s.blockUser, style = MeroType.Body.copy(color = colors.danger)) },
                            onClick = { menuExpanded = false; onBlock() }
                        )
                    }
                }
            }

            // Overlay Name, Age, Location, Badge & Bio
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(MeroSpacing.md)
                    .clickable(onClick = onViewProfile)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    Text(
                        text = "${profile.displayName}, ${profile.age}",
                        style = MeroType.Title.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.ageVerified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = s.verifiedBadge,
                            tint = Color(0xFF66B2FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFFD6E4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = profile.publicLocationLabel,
                        style = MeroType.Caption.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MeroType.Body.copy(color = Color.White.copy(alpha = 0.94f)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Cultural badge & interest pills below hero
        if (profile.culturalBadge.isNotBlank() || profile.interests.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (profile.culturalBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.surface2
                    ) {
                        Text(
                            text = profile.culturalBadge,
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                        )
                    }
                }
                profile.interests.take(4).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.surface2
                    ) {
                        Text(
                            text = tag,
                            style = MeroType.Caption.copy(color = colors.text),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                        )
                    }
                }
            }
        }

        // Explicit Not Interested (X) & Interested (Heart) Buttons matching Screen 2
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MeroSpacing.xs)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                MeroIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = s.notInterested,
                    tint = colors.text,
                    containerColor = colors.surface2,
                    onClick = onNotInterested,
                    testTag = "not_interested_${profile.uid}"
                )
                Text(
                    text = s.notInterested,
                    style = MeroType.Caption.copy(color = colors.textMuted)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                MeroIconButton(
                    icon = Icons.Filled.Favorite,
                    contentDescription = s.interested,
                    tint = if (canSendInterest) colors.onPrimary else colors.textMuted,
                    containerColor = if (canSendInterest) colors.primary else colors.surface2,
                    enabled = canSendInterest,
                    onClick = onInterested,
                    testTag = "interested_${profile.uid}"
                )
                Text(
                    text = s.interested,
                    style = MeroType.Caption.copy(
                        color = if (canSendInterest) colors.primary else colors.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun InterestSentScreen(
    target: PublicProfile,
    language: AppLanguage,
    onViewProfile: () -> Unit,
    onKeepExploring: () -> Unit,
    onJumpToTab: (MeroTab) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F0C18), Color(0xFF341427), Color(0xFF1A0A12))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xl),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xl)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroLotusEmblem(size = 28.dp, tint = Color(0xFFFFB2C8))
                Text(
                    text = s.appName,
                    style = MeroType.Heading.copy(color = Color.White),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(MeroSpacing.md))

            // Central dark plum glass card matching Screen 3 mockup
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = Color(0xFF3E192F),
                border = BorderStroke(1.dp, Color(0xFF6B3152)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.lg),
                    modifier = Modifier.padding(MeroSpacing.xl)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        MeroAvatarImage(
                            photoData = target.photoDataList.firstOrNull(),
                            displayName = target.displayName,
                            size = 112.dp
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .border(2.dp, Color(0xFF3E192F), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = s.interestSentTitle,
                        style = MeroType.Title.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = String.format(s.interestSentSubtitle, target.displayName),
                        style = MeroType.Body.copy(color = Color(0xFFE7CED8)),
                        textAlign = TextAlign.Center
                    )

                    MeroButton(
                        text = s.viewProfile,
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onViewProfile,
                        testTag = "interest_sent_view_profile"
                    )

                    MeroSecondaryButton(
                        text = s.keepExploring,
                        onClick = onKeepExploring,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "interest_sent_keep_exploring"
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Text(
                    text = s.exploreMore,
                    style = MeroType.Heading.copy(color = Color.White)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExploreMoreTile(
                        label = s.tabNearby,
                        icon = Icons.Filled.LocationOn,
                        onClick = { onJumpToTab(MeroTab.HOME) }
                    )
                    ExploreMoreTile(
                        label = s.festivalsSection.split(" ").firstOrNull() ?: "Festivals",
                        icon = Icons.Filled.Spa,
                        onClick = { onJumpToTab(MeroTab.CULTURE) }
                    )
                    ExploreMoreTile(
                        label = s.navLikes,
                        icon = Icons.Filled.Favorite,
                        onClick = { onJumpToTab(MeroTab.LIKES) }
                    )
                    ExploreMoreTile(
                        label = s.dateSpotsTitle.split(" ").firstOrNull() ?: "Spots",
                        icon = Icons.Filled.Restaurant,
                        onClick = { onJumpToTab(MeroTab.CULTURE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreMoreTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
        modifier = Modifier
            .clip(RoundedCornerShape(MeroRadius.small))
            .clickable(onClick = onClick)
            .padding(MeroSpacing.xs)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF4D223B))
                .border(1.dp, Color(0xFF8C466D), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFFFD6E4),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MeroType.Caption.copy(color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
