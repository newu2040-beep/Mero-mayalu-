package com.example.feature.culture

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.Conversation
import com.example.core.domain.HangoutItinerary
import com.example.core.domain.HangoutStop
import com.example.core.domain.SavedHangoutPlan
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.util.LocationServiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HangoutPlannerModal(
    language: AppLanguage,
    currentUid: String,
    defaultCity: String,
    plannerState: UiState<HangoutItinerary>?,
    savedPlans: List<SavedHangoutPlan>,
    activeConversations: List<Conversation>,
    onGeneratePlan: (city: String, budgetNpr: Int, groupSize: Int, durationLabel: String, activities: List<String>, forceFresh: Boolean) -> Unit,
    isAiEnhancing: Boolean = false,
    onSwapStopWithBackup: (Int) -> Unit,
    onSavePlan: (HangoutItinerary) -> Unit,
    onDeleteSavedPlan: (String) -> Unit,
    onSendMeetupInvite: (Conversation, HangoutItinerary, String) -> Unit,
    onAddPlanToTodo: ((title: String, category: String, notes: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNe = language == AppLanguage.NEPALI

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Planner, 1 = Saved Plans
    var selectedCity by rememberSaveable { mutableStateOf(defaultCity.ifBlank { "Kathmandu" }) }
    var budgetNpr by rememberSaveable { mutableIntStateOf(1500) }
    var customBudgetInput by rememberSaveable { mutableStateOf("1500") }
    var groupSize by rememberSaveable { mutableIntStateOf(2) }
    var durationIdx by rememberSaveable { mutableIntStateOf(1) }
    val selectedActivities = remember {
        mutableStateListOf("Heritage Walk", "Chiya & Cafe", "Momo & Local Food")
    }
    var customActivityInput by rememberSaveable { mutableStateOf("") }
    val completedSteps = remember { mutableStateListOf<Int>() }
    var proposedDateText by rememberSaveable {
        mutableStateOf(if (isNe) "यो शनिबार दिउँसो २:०० बजे" else "This Saturday at 2:00 PM")
    }
    var selectedInviteConvId by rememberSaveable {
        mutableStateOf(activeConversations.firstOrNull()?.id.orEmpty())
    }

    val durationOptions = if (isNe) {
        listOf("२ घण्टा" to "2 Hours", "४ घण्टा" to "Half Day (4 Hours)", "पूरा दिन" to "Full Day (6+ Hours)")
    } else {
        listOf("2 Hours" to "2 Hours", "Half Day" to "Half Day (4 Hours)", "Full Day" to "Full Day (6+ Hours)")
    }

    val activityChoices = listOf(
        "Heritage Walk" to if (isNe) "🏛️ सम्पदा पदयात्रा" else "🏛️ Heritage Walk",
        "Chiya & Cafe" to if (isNe) "☕ चिया र क्याफे" else "☕ Chiya & Cafe",
        "Momo & Local Food" to if (isNe) "🥟 मःमः र नेवारी खाजा" else "🥟 Momo & Local Food",
        "Nature & Park" to if (isNe) "🌿 प्रकृति र उद्यान" else "🌿 Nature & Park",
        "Temple & Peace" to if (isNe) "🔔 मन्दिर र शान्ति" else "🔔 Temple & Peace",
        "Photography" to if (isNe) "📸 फोटोग्राफी" else "📸 Photography"
    )

    val locPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            scope.launch {
                val detected = LocationServiceHelper.fetchCoarseDeviceLocation(context)
                if (detected != null) {
                    selectedCity = detected.matchedCity.district
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (plannerState == null) {
            onGeneratePlan(
                selectedCity,
                budgetNpr,
                groupSize,
                durationOptions[durationIdx].second,
                selectedActivities.toList(),
                false
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
                .testTag("mayalu_hangout_planner_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNe) "मायालु AI भेटघाट योजनाकार ❤️" else "Mayalu AI Hangout Planner ❤️",
                        style = MeroType.Title.copy(color = colors.text)
                    )
                    Text(
                        text = if (isNe) {
                            "नेपालका शहरहरूमा बजेट, समय र सुरक्षा अनुसार स्मार्ट यात्रा योजना"
                        } else {
                            "Smart Nepal meetup & outing itineraries with budget, safety & Maps"
                        },
                        style = MeroType.Caption.copy(color = colors.textMuted)
                    )
                }
            }

            // Sub-tabs: Create Plan vs Saved Plans
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroChip(
                    label = if (isNe) "नयाँ योजना बनाउनुहोस्" else "Plan Outing",
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f),
                    testTag = "planner_tab_create"
                )
                MeroChip(
                    label = if (isNe) "सुरक्षित योजना (${savedPlans.size})" else "Saved Plans (${savedPlans.size})",
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f),
                    testTag = "planner_tab_saved"
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { w -> if (forward) w / 6 else -w / 6 })
                        .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { w -> if (forward) -w / 8 else w / 8 })
                },
                label = "planner_tab_transition"
            ) { tab ->
                if (tab == 1) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (savedPlans.isEmpty()) {
                            MeroCard(modifier = Modifier.animateContentSize(tween(200))) {
                                Text(
                                    text = if (isNe) "अहिलेसम्म कुनै भेटघाट योजना सुरक्षित गरिएको छैन।" else "No saved hangout plans yet. Generate a plan and tap Save!",
                                    style = MeroType.Body.copy(color = colors.textMuted)
                                )
                            }
                        } else {
                            savedPlans.forEach { saved ->
                                var expanded by rememberSaveable(saved.id) { mutableStateOf(true) }
                                MeroCard(
                                    modifier = Modifier
                                        .animateContentSize(animationSpec = tween(220))
                                        .testTag("saved_plan_card_${saved.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded }
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = saved.title,
                                                style = MeroType.Heading.copy(color = colors.text)
                                            )
                                            Text(
                                                text = "${saved.city} · ${saved.durationLabel} · NPR ${saved.totalBudgetNpr}",
                                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MeroIconButton(
                                                icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = if (expanded) "Collapse" else "Expand",
                                                tint = colors.textMuted,
                                                onClick = { expanded = !expanded },
                                                testTag = "toggle_saved_plan_${saved.id}"
                                            )
                                            MeroIconButton(
                                                icon = Icons.Filled.Delete,
                                                contentDescription = "Delete plan",
                                                tint = colors.danger,
                                                onClick = { onDeleteSavedPlan(saved.id) },
                                                testTag = "delete_saved_plan_${saved.id}"
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn(tween(180)),
                                        exit = fadeOut(tween(150))
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (saved.summaryNotes.isNotBlank()) {
                                                Text(
                                                    text = saved.summaryNotes,
                                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                                )
                                            }
                                            saved.stopsSummary.forEach { stopLine ->
                                                Text(
                                                    text = "• $stopLine",
                                                    style = MeroType.Body.copy(color = colors.text)
                                                )
                                            }
                                            if (onAddPlanToTodo != null) {
                                                MeroSecondaryButton(
                                                    text = if (isNe) "कार्यसूचीमा थप्नुहोस्" else "Add to To-Do List",
                                                    leadingIcon = Icons.Filled.Add,
                                                    onClick = {
                                                        onAddPlanToTodo(
                                                            "${saved.city}: ${saved.title}",
                                                            "Meetup Prep",
                                                            saved.stopsSummary.take(4).joinToString("\n• ", prefix = "• ")
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    testTag = "saved_plan_add_todo_${saved.id}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                        modifier = Modifier.fillMaxWidth()
                    ) {

            // 1. City & GPS Selection
            MeroCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isNe) "१. शहर वा स्थान रोज्नुहोस्" else "1. Select City or Area",
                        style = MeroType.Heading.copy(color = colors.text)
                    )
                    MeroChip(
                        label = if (isNe) "📍 मेरो स्थान" else "📍 Use GPS",
                        selected = false,
                        leadingIcon = Icons.Filled.MyLocation,
                        onClick = {
                            if (LocationServiceHelper.hasLocationPermission(context)) {
                                scope.launch {
                                    val loc = LocationServiceHelper.fetchCoarseDeviceLocation(context)
                                    if (loc != null) selectedCity = loc.matchedCity.district
                                }
                            } else {
                                locPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        testTag = "planner_use_gps_button"
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    val quickCities = listOf("Kathmandu", "Patan", "Bhaktapur", "Pokhara", "Chitwan", "Itahari", "Dharan", "Lumbini", "Ilam", "Janakpur")
                    quickCities.forEach { cityOpt ->
                        MeroChip(
                            label = cityOpt,
                            selected = selectedCity.equals(cityOpt, ignoreCase = true),
                            onClick = { selectedCity = cityOpt },
                            testTag = "planner_city_chip_$cityOpt"
                        )
                    }
                }

                MeroInput(
                    label = if (isNe) "शहर / क्षेत्र" else "City or Landmark",
                    value = selectedCity,
                    onValueChange = { selectedCity = it },
                    placeholder = "Kathmandu, Itahari, Pokhara...",
                    testTag = "planner_city_input"
                )
            }

            // 2. Budget (NPR), Group Size & Duration
            MeroCard {
                Text(
                    text = if (isNe) "२. बजेट (NPR), समूह र अवधि" else "2. Budget (NPR), Group Size & Duration",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                Text(
                    text = if (isNe) "अनुमानित कुल बजेट: रु. $budgetNpr" else "Total Budget: NPR $budgetNpr",
                    style = MeroType.Body.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    listOf(500, 1000, 1500, 2500, 4000, 6000).forEach { amt ->
                        MeroChip(
                            label = "NPR $amt",
                            selected = budgetNpr == amt,
                            onClick = {
                                budgetNpr = amt
                                customBudgetInput = amt.toString()
                            },
                            testTag = "planner_budget_chip_$amt"
                        )
                    }
                }
                MeroInput(
                    label = if (isNe) "आफ्नै बजेट रकम (NPR)" else "Custom Budget (NPR)",
                    value = customBudgetInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(6)
                        customBudgetInput = digits
                        val parsed = digits.toIntOrNull()
                        if (parsed != null && parsed >= 300) {
                            budgetNpr = parsed.coerceIn(300, 50000)
                        }
                    },
                    placeholder = "e.g., 1850",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "planner_custom_budget_input"
                )

                Text(
                    text = if (isNe) "सहभागी संख्या: $groupSize जना" else "Group Size: $groupSize ${if (groupSize == 1) "Person" else "People"}",
                    style = MeroType.Caption.copy(color = colors.textMuted)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        2 to if (isNe) "२ जना" else "2 People",
                        3 to if (isNe) "३-४ साथी" else "3-4 Friends",
                        6 to if (isNe) "५+ समूह" else "5+ Group"
                    ).forEach { (cnt, lbl) ->
                        MeroChip(
                            label = lbl,
                            selected = groupSize == cnt,
                            onClick = { groupSize = cnt },
                            testTag = "planner_group_chip_$cnt"
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durationOptions.forEachIndexed { idx, (lbl, _) ->
                        MeroChip(
                            label = lbl,
                            selected = durationIdx == idx,
                            onClick = { durationIdx = idx },
                            testTag = "planner_duration_chip_$idx"
                        )
                    }
                }
            }

            // 3. Preferred Activities
            MeroCard {
                Text(
                    text = if (isNe) "३. मनपर्ने गतिविधिहरू" else "3. Preferred Activities",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    val presetKeys = activityChoices.map { it.first }.toSet()
                    activityChoices.forEach { (key, label) ->
                        val isSelected = key in selectedActivities
                        MeroChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                if (isSelected && selectedActivities.size > 1) {
                                    selectedActivities.remove(key)
                                } else if (!isSelected) {
                                    selectedActivities.add(key)
                                }
                            }
                        )
                    }
                    selectedActivities.filter { it !in presetKeys }.forEach { customAct ->
                        MeroChip(
                            label = "✨ $customAct",
                            selected = true,
                            leadingIcon = Icons.Filled.Close,
                            onClick = {
                                if (selectedActivities.size > 1) {
                                    selectedActivities.remove(customAct)
                                }
                            },
                            testTag = "planner_custom_activity_chip_$customAct"
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MeroInput(
                        label = if (isNe) "आफ्नै गतिविधि थप्नुहोस्" else "Add Custom Activity",
                        value = customActivityInput,
                        onValueChange = { if (it.length <= 36) customActivityInput = it },
                        placeholder = if (isNe) "जस्तै: बोटिङ, लाइभ संगीत" else "e.g., Boating, Pottery, Live Music",
                        modifier = Modifier.weight(1f),
                        testTag = "planner_custom_activity_input"
                    )
                    MeroSecondaryButton(
                        text = if (isNe) "थप्नुहोस्" else "Add",
                        leadingIcon = Icons.Filled.Add,
                        enabled = customActivityInput.trim().isNotBlank(),
                        onClick = {
                            val clean = customActivityInput.trim()
                            if (clean.isNotBlank() && selectedActivities.none { it.equals(clean, ignoreCase = true) }) {
                                selectedActivities.add(clean)
                            }
                            customActivityInput = ""
                        },
                        testTag = "planner_add_custom_activity_button"
                    )
                }

                MeroButton(
                    text = if (isNe) "AI स्मार्ट भेटघाट योजना बनाउनुहोस्" else "Generate Smart Hangout Plan",
                    leadingIcon = Icons.Filled.AutoAwesome,
                    isLoading = false,
                    onClick = {
                        completedSteps.clear()
                        onGeneratePlan(
                            selectedCity,
                            budgetNpr,
                            groupSize,
                            durationOptions[durationIdx].second,
                            selectedActivities.toList(),
                            false
                        )
                        scope.launch {
                            delay(90L)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    testTag = "planner_generate_button"
                )
            }

            // 4. Generated Itinerary Output (Zero-blocking instant render + subtle live AI badge)
            when (plannerState) {
                is UiState.Loading -> {
                    MeroLoadingState(
                        message = if (isNe) "योजना तयार हुँदैछ..." else "Preparing itinerary..."
                    )
                }
                is UiState.Error -> {
                    MeroErrorState(
                        message = plannerState.message,
                        retryLabel = if (isNe) "फेरि प्रयास गर्नुहोस्" else "Retry",
                        onRetry = {
                            onGeneratePlan(
                                selectedCity,
                                budgetNpr,
                                groupSize,
                                durationOptions[durationIdx].second,
                                selectedActivities.toList(),
                                true
                            )
                        }
                    )
                }
                is UiState.Success -> {
                    val plan = plannerState.data
                    ItineraryResultSection(
                        plan = plan,
                        isNe = isNe,
                        isAiEnhancing = isAiEnhancing,
                        currentUid = currentUid,
                        completedSteps = completedSteps,
                        activeConversations = activeConversations,
                        selectedInviteConvId = selectedInviteConvId,
                        proposedDateText = proposedDateText,
                        onSelectInviteConvId = { selectedInviteConvId = it },
                        onProposedDateChange = { proposedDateText = it },
                        onToggleStepCompleted = { step ->
                            if (step in completedSteps) completedSteps.remove(step) else completedSteps.add(step)
                        },
                        onSwapWithBackup = onSwapStopWithBackup,
                        onSavePlan = { onSavePlan(plan) },
                        onAddPlanToTodo = onAddPlanToTodo?.let { addFn ->
                            {
                                addFn(
                                    "${plan.city}: ${plan.title}",
                                    "Meetup Prep",
                                    plan.stops.joinToString("\n") { "${it.stepNumber}. ${it.placeName} (${it.timeSlot})" }
                                )
                            }
                        },
                        onRegenerate = {
                            completedSteps.clear()
                            onGeneratePlan(
                                selectedCity,
                                budgetNpr,
                                groupSize,
                                durationOptions[durationIdx].second,
                                selectedActivities.toList(),
                                true
                            )
                        },
                        onSendMeetupInvite = { conv ->
                            onSendMeetupInvite(conv, plan, proposedDateText)
                        }
                    )
                }
                null -> Unit
            }
                    }
                }
            }

            Spacer(Modifier.height(MeroSpacing.xl))
        }
    }
}

@Composable
private fun ItineraryResultSection(
    plan: HangoutItinerary,
    isNe: Boolean,
    isAiEnhancing: Boolean,
    currentUid: String,
    completedSteps: List<Int>,
    activeConversations: List<Conversation>,
    selectedInviteConvId: String,
    proposedDateText: String,
    onSelectInviteConvId: (String) -> Unit,
    onProposedDateChange: (String) -> Unit,
    onToggleStepCompleted: (Int) -> Unit,
    onSwapWithBackup: (Int) -> Unit,
    onSavePlan: () -> Unit,
    onAddPlanToTodo: (() -> Unit)? = null,
    onRegenerate: () -> Unit,
    onSendMeetupInvite: (Conversation) -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current

    // Overview Card
    MeroCard {
        AnimatedVisibility(
            visible = isAiEnhancing,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.pill),
                color = colors.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (isNe) "✨ Gemini AI ले प्रत्यक्ष स्थान विवरण परिष्कृत गर्दैछ..."
                    else "✨ Refining live Gemini AI venue details...",
                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                )
            }
        }
        Text(
            text = plan.title,
            style = MeroType.Title.copy(color = colors.text)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.pill),
                color = colors.primary
            ) {
                Text(
                    text = "NPR ${plan.totalEstimatedBudgetNpr}",
                    style = MeroType.Caption.copy(color = colors.onPrimary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                )
            }
            Text(
                text = "${plan.city} · ${plan.durationLabel} · ${plan.groupSize} ${if (isNe) "जना" else "People"}",
                style = MeroType.Caption.copy(color = colors.textMuted, fontWeight = FontWeight.Medium)
            )
        }
        Text(
            text = if (isNe) "🚩 सुरुवात बिन्दु: ${plan.startingLocation}" else "🚩 Starting Point: ${plan.startingLocation}",
            style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium)
        )
        Text(
            text = "🌤️ ${plan.weatherAndTimingNote}",
            style = MeroType.Caption.copy(color = colors.textMuted)
        )
    }

    // Ordered Timeline Stops with Interactive Checklist & Backup Swap
    plan.stops.forEach { stop ->
        val isDone = stop.stepNumber in completedSteps
        StopTimelineCard(
            stop = stop,
            isNe = isNe,
            isCompleted = isDone,
            onToggleCompleted = { onToggleStepCompleted(stop.stepNumber) },
            onSwapBackup = { onSwapWithBackup(stop.stepNumber) },
            onOpenMaps = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(stop.mapsUri)))
                }
            }
        )
    }

    // Places / Areas to Avoid (with verified reasons)
    if (plan.placesToAvoid.isNotEmpty()) {
        MeroCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isNe) "ध्यान दिनुपर्ने र छल्नुपर्ने क्षेत्रहरू" else "Places & Peak Hours to Avoid",
                    style = MeroType.Heading.copy(color = colors.danger)
                )
            }
            plan.placesToAvoid.forEach { avoid ->
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.danger.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        Text(
                            text = "⚠️ ${avoid.placeOrArea}",
                            style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = avoid.reason,
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                }
            }
        }
    }

    // Safety Precautions & Travel Tips
    MeroCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = if (isNe) "सुरक्षा सुझाव र यात्रा तयारी" else "Safety Precautions & Travel Advice",
                style = MeroType.Heading.copy(color = colors.text)
            )
        }
        plan.safetyPrecautions.forEach { tip ->
            Text(
                text = "🛡️ $tip",
                style = MeroType.Body.copy(color = colors.text)
            )
        }
        plan.packingAndTravelTips.forEach { tip ->
            Text(
                text = "🎒 $tip",
                style = MeroType.Caption.copy(color = colors.textMuted)
            )
        }
    }

    // Save & Regenerate buttons
    Row(
        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        MeroSecondaryButton(
            text = if (isNe) "फेरि बनाउनुहोस्" else "Regenerate",
            leadingIcon = Icons.Filled.Refresh,
            onClick = onRegenerate,
            modifier = Modifier.weight(1f),
            testTag = "planner_regenerate_button"
        )
        MeroSecondaryButton(
            text = if (isNe) "योजना सुरक्षित गर्नुहोस्" else "Save Plan",
            leadingIcon = Icons.Filled.BookmarkAdd,
            onClick = onSavePlan,
            modifier = Modifier.weight(1f),
            testTag = "planner_save_plan_button"
        )
    }

    if (onAddPlanToTodo != null) {
        MeroSecondaryButton(
            text = if (isNe) "कार्यसूचीमा थप्नुहोस्" else "Add Outing to To-Do List",
            leadingIcon = Icons.Filled.Add,
            onClick = onAddPlanToTodo,
            modifier = Modifier.fillMaxWidth(),
            testTag = "planner_add_to_todo_button"
        )
    }

    // Virtual Meetup Planner: Invite a Connected Match
    if (activeConversations.isNotEmpty()) {
        MeroCard {
            Text(
                text = if (isNe) "भर्चुअल भेटघाट निमन्त्रणा पठाउनुहोस्" else "Invite Connection to This Meetup",
                style = MeroType.Heading.copy(color = colors.text)
            )
            Text(
                text = if (isNe) "आफ्नो मिल्ने साथीलाई यो यात्रा योजना पठाएर समय निश्चित गर्नुहोस्।"
                else "Share this itinerary with a mutual connection so they can accept and confirm attendance.",
                style = MeroType.Caption.copy(color = colors.textMuted)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                activeConversations.take(3).forEach { conv ->
                    val partnerName = if (conv.userAId == currentUid) conv.userBName else conv.userAName
                    MeroChip(
                        label = partnerName.ifBlank { "Connection" },
                        selected = selectedInviteConvId == conv.id,
                        onClick = { onSelectInviteConvId(conv.id) }
                    )
                }
            }
            MeroInput(
                label = if (isNe) "प्रस्तावित मिति र समय" else "Proposed Date & Time",
                value = proposedDateText,
                onValueChange = onProposedDateChange,
                testTag = "planner_invite_date_input"
            )
            val chosenConv = activeConversations.firstOrNull { it.id == selectedInviteConvId }
                ?: activeConversations.first()
            MeroSecondaryButton(
                text = if (isNe) "भेटघाट निमन्त्रणा पठाउनुहोस्" else "Send Meetup Invitation",
                leadingIcon = Icons.AutoMirrored.Filled.Send,
                onClick = { onSendMeetupInvite(chosenConv) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "planner_send_invite_button"
            )
        }
    }
}

@Composable
private fun StopTimelineCard(
    stop: HangoutStop,
    isNe: Boolean,
    isCompleted: Boolean,
    onToggleCompleted: () -> Unit,
    onSwapBackup: () -> Unit,
    onOpenMaps: () -> Unit
) {
    val colors = LocalMeroColors.current
    MeroCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            MeroIconButton(
                icon = Icons.Filled.CheckCircle,
                contentDescription = "Mark step completed",
                tint = if (isCompleted) colors.onPrimary else colors.textMuted,
                containerColor = if (isCompleted) colors.success else colors.surface2,
                onClick = onToggleCompleted,
                testTag = "planner_step_check_${stop.stepNumber}"
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stop.stepNumber}. ${stop.placeName}",
                    style = MeroType.Heading.copy(
                        color = colors.text,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                Text(
                    text = "${stop.category} · NPR ${stop.estimatedCostNpr}",
                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${stop.timeSlot} (${stop.travelTimeFromPrevious}) · ${if (isNe) "खुल्ने समय:" else "Hours:"} ${stop.openingHours}",
                style = MeroType.Caption.copy(color = colors.textMuted)
            )
        }

        if (stop.recommendedActivity.isNotBlank()) {
            Text(
                text = stop.recommendedActivity,
                style = MeroType.Body.copy(color = colors.text)
            )
        }

        if (stop.foodSuggestion.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stop.foodSuggestion,
                    style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
                )
            }
        }

        // Open in Google Maps + Swap Backup Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.small),
                color = colors.surface2,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MeroRadius.small))
                    .clickable(onClick = onOpenMaps)
                    .testTag("planner_stop_maps_${stop.stepNumber}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Google Maps",
                        style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Normal),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (stop.backupAlternative.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.surface2,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(MeroRadius.small))
                        .clickable(onClick = onSwapBackup)
                        .testTag("planner_stop_swap_${stop.stepNumber}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isNe) "वैकल्पिक स्थान" else "Swap Backup",
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}
