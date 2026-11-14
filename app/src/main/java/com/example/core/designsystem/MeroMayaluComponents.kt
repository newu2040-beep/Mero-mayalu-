package com.example.core.designsystem

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    testTag: String = "mero_primary_button"
) {
    val colors = LocalMeroColors.current
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(MeroRadius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surface2,
            disabledContentColor = colors.textMuted
        ),
        contentPadding = PaddingValues(horizontal = MeroSpacing.lg),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(MeroSpacing.sm))
            }
            Text(
                text = text,
                style = MeroType.Body.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MeroSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    isDanger: Boolean = false,
    testTag: String = "mero_secondary_button"
) {
    val colors = LocalMeroColors.current
    val fg = if (isDanger) colors.danger else colors.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(MeroRadius.pill),
        border = BorderStroke(1.dp, if (enabled) fg.copy(alpha = 0.5f) else colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = fg,
            disabledContentColor = colors.textMuted
        ),
        contentPadding = PaddingValues(horizontal = MeroSpacing.lg),
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(MeroSpacing.sm))
        }
        Text(
            text = text,
            style = MeroType.Body.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalMeroColors.current.text,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
    badgeCount: Int = 0,
    testTag: String = "mero_icon_button"
) {
    val colors = LocalMeroColors.current
    Box(modifier = modifier) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(containerColor)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else colors.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }
        if (badgeCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    style = MeroType.Caption.copy(fontWeight = FontWeight.Bold, color = colors.onPrimary),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = MeroSpacing.md,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMeroColors.current
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(MeroRadius.card))
        .background(colors.surface)
        .border(1.dp, colors.border, RoundedCornerShape(MeroRadius.card))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(contentPadding)
    Column(
        verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
        modifier = baseModifier,
        content = content
    )
}

@Composable
fun MeroListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    isDanger: Boolean = false,
    testTag: String = "mero_list_item"
) {
    val colors = LocalMeroColors.current
    val fg = if (isDanger) colors.danger else colors.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(MeroRadius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.sm)
            .testTag(testTag)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isDanger) colors.danger else colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MeroType.Body.copy(fontWeight = FontWeight.Medium, color = fg),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MeroType.Caption.copy(color = colors.textMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MeroType.Caption.copy(color = colors.textMuted),
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MeroInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String = "mero_input"
) {
    val colors = LocalMeroColors.current
    val fontFamily = LocalMeroFontFamily.current
    Column(
        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MeroType.Caption.copy(fontWeight = FontWeight.Medium, color = colors.text),
            maxLines = 2
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            textStyle = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = colors.text
            ),
            singleLine = singleLine,
            maxLines = maxLines,
            isError = error != null,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(MeroRadius.small),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                errorBorderColor = colors.danger,
                cursorColor = colors.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag(testTag)
        )
        if (error != null) {
            Text(
                text = error,
                style = MeroType.Caption.copy(color = colors.danger),
                maxLines = 2
            )
        }
    }
}

@Composable
fun MeroChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String = "mero_chip"
) {
    val colors = LocalMeroColors.current
    val bg = if (selected) colors.primary else colors.surface2
    val fg = if (selected) colors.onPrimary else colors.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(MeroRadius.pill))
            .background(bg)
            .border(1.dp, if (selected) colors.primary else colors.border, RoundedCornerShape(MeroRadius.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
            .testTag(testTag)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MeroSpacing.xs))
        }
        Text(
            text = label,
            style = MeroType.Caption.copy(fontWeight = FontWeight.Medium, color = fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MeroTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    leadingAction: (@Composable () -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalMeroColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = MeroSpacing.lg)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier.weight(1f)
        ) {
            if (leadingAction != null) {
                leadingAction()
            } else if (showLogo) {
                MeroLotusEmblem(size = 28.dp, tint = colors.primary)
            }
            Text(
                text = title,
                style = MeroType.Title.copy(color = if (showLogo) colors.primary else colors.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
            content = trailingActions
        )
    }
}

enum class MeroTab(val route: String) {
    HOME("home"),
    LIKES("likes"),
    EXPLORE("explore"),
    CULTURE("culture"),
    PROFILE("profile")
}

@Composable
fun MeroBottomNav(
    currentTab: MeroTab,
    onSelectTab: (MeroTab) -> Unit,
    labels: Map<MeroTab, String>,
    pendingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
    ) {
        Surface(
            shape = RoundedCornerShape(MeroRadius.pill),
            color = colors.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(60.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MeroSpacing.sm)
            ) {
                BottomNavItem(
                    label = labels[MeroTab.HOME] ?: "Home",
                    icon = if (currentTab == MeroTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    selected = currentTab == MeroTab.HOME,
                    onClick = { onSelectTab(MeroTab.HOME) },
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = labels[MeroTab.LIKES] ?: "Likes",
                    icon = if (currentTab == MeroTab.LIKES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    selected = currentTab == MeroTab.LIKES,
                    badge = pendingCount,
                    onClick = { onSelectTab(MeroTab.LIKES) },
                    testTag = "nav_likes",
                    modifier = Modifier.weight(1f)
                )

                // Center distinctive circular action (Lotus / Discovery & Culture quick lens)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colors.primary, colors.primary.copy(alpha = 0.78f))
                            )
                        )
                        .clickable { onSelectTab(MeroTab.EXPLORE) }
                        .testTag("nav_center_lotus")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFlorist,
                        contentDescription = labels[MeroTab.EXPLORE] ?: "Explore",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                BottomNavItem(
                    label = labels[MeroTab.CULTURE] ?: "Culture",
                    icon = if (currentTab == MeroTab.CULTURE) Icons.Filled.Spa else Icons.Outlined.Spa,
                    selected = currentTab == MeroTab.CULTURE,
                    onClick = { onSelectTab(MeroTab.CULTURE) },
                    testTag = "nav_culture",
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = labels[MeroTab.PROFILE] ?: "Profile",
                    icon = if (currentTab == MeroTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    selected = currentTab == MeroTab.PROFILE,
                    onClick = { onSelectTab(MeroTab.PROFILE) },
                    testTag = "nav_profile",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    val colors = LocalMeroColors.current
    val fg = if (selected) colors.primary else colors.textMuted
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(MeroRadius.small))
            .clickable(onClick = onClick)
            .padding(vertical = MeroSpacing.xs)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }
            Text(
                text = label,
                style = MeroType.Caption.copy(
                    color = fg,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MeroLotusEmblem(size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.68f, h * 0.35f, w * 0.68f, h * 0.72f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.32f, h * 0.72f, w * 0.32f, h * 0.35f, w * 0.5f, h * 0.12f)
            close()
        }
        val left = Path().apply {
            moveTo(w * 0.5f, h * 0.86f)
            cubicTo(w * 0.18f, h * 0.78f, w * 0.08f, h * 0.45f, w * 0.22f, h * 0.28f)
            cubicTo(w * 0.38f, h * 0.42f, w * 0.44f, h * 0.64f, w * 0.5f, h * 0.86f)
            close()
        }
        val right = Path().apply {
            moveTo(w * 0.5f, h * 0.86f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.92f, h * 0.45f, w * 0.78f, h * 0.28f)
            cubicTo(w * 0.62f, h * 0.42f, w * 0.56f, h * 0.64f, w * 0.5f, h * 0.86f)
            close()
        }
        drawPath(left, tint.copy(alpha = 0.75f))
        drawPath(right, tint.copy(alpha = 0.75f))
        drawPath(center, tint)
        drawCircle(tint.copy(alpha = 0.9f), radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.08f))
    }
}

private fun extractInitials(rawName: String): String {
    val parts = rawName.trim()
        .substringBefore(",")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "मे"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
fun MeroAvatarImage(
    photoData: String?,
    displayName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val bitmap = remember(photoData) {
        if (photoData.isNullOrBlank()) null
        else runCatching {
            val clean = photoData.substringAfter("base64,", photoData)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    val initials = remember(displayName) { extractInitials(displayName) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.24f),
                        colors.surface2,
                        colors.primary.copy(alpha = 0.14f)
                    )
                )
            )
            .border(2.dp, colors.surface, CircleShape)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MeroLotusEmblem(
                size = size * 0.68f,
                tint = colors.primary.copy(alpha = 0.16f)
            )
            Text(
                text = initials,
                style = if (size >= 80.dp) {
                    MeroType.Title.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                } else {
                    MeroType.Heading.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                },
                maxLines = 1
            )
        }
    }
}

@Composable
fun MeroPhotoBox(
    photoData: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val bitmap = remember(photoData) {
        if (photoData.isNullOrBlank()) null
        else runCatching {
            val clean = photoData.substringAfter("base64,", photoData)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    val initials = remember(contentDescription) { extractInitials(contentDescription) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    colors.surface2,
                    colors.primary.copy(alpha = 0.22f),
                    colors.surface
                )
            )
        )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.padding(MeroSpacing.lg)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.85f))
                        .border(2.dp, colors.primary.copy(alpha = 0.35f), CircleShape)
                ) {
                    MeroLotusEmblem(
                        size = 56.dp,
                        tint = colors.primary.copy(alpha = 0.22f)
                    )
                    Text(
                        text = initials,
                        style = MeroType.Title.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun MeroBannerImage(
    bannerData: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val preset = ProfileBannerPreset.fromId(bannerData)
    val customBitmap = remember(bannerData) {
        if (bannerData.isNullOrBlank() || preset != null || bannerData.startsWith("preset:")) null
        else runCatching {
            val clean = bannerData.substringAfter("base64,", bannerData)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Box(modifier = modifier) {
        when {
            customBitmap != null -> {
                Image(
                    bitmap = customBitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            preset?.drawableRes != null -> {
                Image(
                    painter = painterResource(id = preset.drawableRes),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            preset == ProfileBannerPreset.THEME_SILK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colors.primary, colors.primary.copy(alpha = 0.55f), colors.surface2)
                            )
                        )
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_profile_cover_dusk),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MeroLoadingState(
    message: String = "",
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(MeroSpacing.xxl)
    ) {
        CircularProgressIndicator(color = colors.primary)
        if (message.isNotBlank()) {
            Spacer(Modifier.height(MeroSpacing.md))
            Text(
                text = message,
                style = MeroType.Body.copy(color = colors.textMuted),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MeroEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    MeroCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MeroSpacing.lg)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.surface2)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MeroType.Heading.copy(color = colors.text),
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = MeroType.Body.copy(color = colors.textMuted),
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                MeroSecondaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    testTag = "empty_state_action"
                )
            }
        }
    }
}

@Composable
fun MeroErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    MeroCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MeroType.Body.copy(color = colors.text),
                textAlign = TextAlign.Center
            )
            MeroSecondaryButton(
                text = retryLabel,
                onClick = onRetry,
                testTag = "error_retry_button"
            )
        }
    }
}

@Composable
fun MeroToast(
    message: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(MeroRadius.pill),
            color = colors.text,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier.padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.md)
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MeroType.Body.copy(color = colors.bg),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (actionLabel != null && onAction != null) {
                    Text(
                        text = actionLabel,
                        style = MeroType.Body.copy(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onAction)
                            .padding(horizontal = MeroSpacing.xs)
                    )
                }
            }
        }
    }
}
