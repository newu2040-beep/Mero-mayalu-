package com.example.feature.culture

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TempleBuddhist
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.domain.HeritagePlacesResult
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.BikramSambatConverter
import com.example.core.util.NepaliCultureCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CultureScreen(
    language: AppLanguage,
    useBikramSambat: Boolean,
    placesState: UiState<HeritagePlacesResult>?,
    onSearchPlaces: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val context = LocalContext.current
    val isNe = language == AppLanguage.NEPALI

    var placeQuery by rememberSaveable { mutableStateOf("") }
    var icebreakerIdx by rememberSaveable { mutableIntStateOf(0) }
    val todayAd = remember { LocalDate.now() }
    val todayBs = remember(todayAd) { BikramSambatConverter.fromLocalDate(todayAd) }
    val countdowns = remember(todayAd) { BikramSambatConverter.upcomingFestivalsWithCountdown(todayAd).take(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        MeroTopBar(title = s.cultureTitle)

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
            // Today in Bikram Sambat / AD
            item {
                MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (useBikramSambat && todayBs != null) {
                                    todayBs.format(isNe)
                                } else {
                                    todayAd.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                                },
                                style = MeroType.Heading.copy(color = colors.text)
                            )
                            if (useBikramSambat && todayBs != null) {
                                Text(
                                    text = todayAd.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming Nepali Festival Countdown + Icebreaker Starter
            if (countdowns.isNotEmpty()) {
                item {
                    MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Celebration,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = s.festivalCountdownTitle,
                                style = MeroType.Heading.copy(color = colors.text)
                            )
                        }
                        countdowns.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MeroRadius.small))
                                    .background(colors.surface2)
                                    .padding(MeroSpacing.md)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isNe) item.festival.nameNe else item.festival.nameEn,
                                        style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = item.targetBsDate.format(isNe),
                                        style = MeroType.Caption.copy(color = colors.textMuted)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(MeroRadius.pill),
                                    color = colors.primary
                                ) {
                                    Text(
                                        text = if (item.daysRemaining == 0L) {
                                            if (isNe) "आज" else "Today"
                                        } else if (isNe) {
                                            "${item.daysRemaining} दिन"
                                        } else {
                                            "${item.daysRemaining}d"
                                        },
                                        style = MeroType.Caption.copy(color = colors.onPrimary, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                                    )
                                }
                            }
                        }
                        val icebreaker = NepaliCultureCatalog.icebreakers[icebreakerIdx % NepaliCultureCatalog.icebreakers.size]
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.small),
                            color = colors.surface2,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isNe) icebreaker.second else icebreaker.first,
                                style = MeroType.Body.copy(color = colors.text),
                                modifier = Modifier.padding(MeroSpacing.md)
                            )
                        }
                        MeroSecondaryButton(
                            text = s.icebreakerButton,
                            leadingIcon = Icons.Filled.AutoAwesome,
                            onClick = { icebreakerIdx += 1 },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "culture_next_icebreaker"
                        )
                    }
                }
            }

            // Live Heritage & Meetup Spot Finder (Google Maps Grounding via Gemini)
            item {
                MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                    Text(
                        text = s.dateSpotsTitle,
                        style = MeroType.Heading.copy(color = colors.text)
                    )
                    Text(
                        text = s.dateSpotsSubtitle,
                        style = MeroType.Body.copy(color = colors.textMuted)
                    )
                    MeroInput(
                        label = s.searchPlacesHint,
                        value = placeQuery,
                        onValueChange = { placeQuery = it },
                        placeholder = if (isNe) "जस्तै: पाटनको शान्त चिया पसल" else "Quiet chiya cafe in Patan",
                        testTag = "culture_places_input"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                    ) {
                        val quickQueries = if (isNe) {
                            listOf("पाटन चिया र सम्पदा", "पोखरा ताल किनार कफी", "भक्तपुर दरबार मःमः")
                        } else {
                            listOf("Patan Heritage Chiya", "Pokhara Lakeside Cafe", "Bhaktapur Pottery & Momo")
                        }
                        quickQueries.forEach { preset ->
                            MeroChip(
                                label = preset,
                                selected = placeQuery == preset,
                                onClick = {
                                    placeQuery = preset
                                    onSearchPlaces(preset)
                                }
                            )
                        }
                    }
                    MeroButton(
                        text = s.findPlacesButton,
                        leadingIcon = Icons.Filled.Search,
                        enabled = placeQuery.trim().length >= 2 && placesState !is UiState.Loading,
                        isLoading = placesState is UiState.Loading,
                        onClick = { onSearchPlaces(placeQuery.trim()) },
                        testTag = "culture_find_places_button"
                    )

                    when (placesState) {
                        is UiState.Loading -> MeroLoadingState()
                        is UiState.Error -> MeroErrorState(
                            message = placesState.message,
                            retryLabel = s.retry,
                            onRetry = {
                                if (placeQuery.isNotBlank()) onSearchPlaces(placeQuery.trim())
                            }
                        )
                        is UiState.Success -> {
                            val result = placesState.data
                            if (result.summaryText.isNotBlank()) {
                                Text(
                                    text = result.summaryText,
                                    style = MeroType.Body.copy(color = colors.text)
                                )
                            }
                            if (result.places.isNotEmpty()) {
                                // Mandatory Google Maps Grounding Attribution & Sources immediately below generated summary
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = s.mapsSourcesTitle,
                                            style = MeroType.Caption.copy(
                                                color = colors.textMuted,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        // Exact non-localized Google Maps attribution per grounding guidelines (12sp, Normal, >=4.5:1)
                                        Text(
                                            text = "Google Maps",
                                            style = MeroType.Caption.copy(
                                                color = colors.text,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    result.places.forEach { place ->
                                        Surface(
                                            shape = RoundedCornerShape(MeroRadius.small),
                                            color = colors.surface2,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(MeroRadius.small))
                                                .clickable {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(place.uri))
                                                        )
                                                    }
                                                }
                                                .testTag("maps_place_${place.placeId ?: place.title}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(MeroSpacing.md)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = place.title,
                                                    style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = s.openInMaps,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        null -> Unit
                    }
                }
            }

            // Festival Calendar Section
            item {
                MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                    Text(
                        text = s.festivalsSection,
                        style = MeroType.Heading.copy(color = colors.text)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.img_culture_festival_tihar),
                        contentDescription = s.festivalsSection,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(MeroRadius.card))
                    )
                    NepaliCultureCatalog.festivals.forEach { fest ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MeroRadius.small))
                                .background(colors.surface2)
                                .padding(MeroSpacing.md)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isNe) fest.nameNe else fest.nameEn,
                                    style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (isNe) fest.bsPeriodNe else fest.bsPeriodEn,
                                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                                )
                            }
                            Text(
                                text = if (isNe) fest.descriptionNe else fest.descriptionEn,
                                style = MeroType.Body.copy(color = colors.textMuted)
                            )
                        }
                    }
                }
            }

            // Newari & Nepali Heritage Spotlight
            item {
                MeroCard(modifier = Modifier.widthIn(max = 560.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TempleBuddhist,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = s.heritageSection,
                            style = MeroType.Heading.copy(color = colors.text)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_culture_heritage_patan),
                        contentDescription = s.heritageSection,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(MeroRadius.card))
                    )
                    NepaliCultureCatalog.heritageSpots.forEach { spot ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MeroRadius.small))
                                .background(colors.surface2)
                                .padding(MeroSpacing.md)
                        ) {
                            Text(
                                text = "${if (isNe) spot.titleNe else spot.titleEn} · ${spot.city}",
                                style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isNe) spot.summaryNe else spot.summaryEn,
                                style = MeroType.Body.copy(color = colors.textMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}
