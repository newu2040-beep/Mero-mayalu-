package com.example.feature.moments

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroErrorState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.TodoItem
import com.example.core.domain.UiState
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodosTabContent(
    todosState: UiState<List<TodoItem>>,
    language: AppLanguage,
    onAddNew: () -> Unit,
    onEdit: (TodoItem) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    val categoryOptions = listOf(
        "ALL" to if (isNe) "सबै" else "All",
        "Personal" to if (isNe) "व्यक्तिगत" else "Personal",
        "Meetup Prep" to if (isNe) "भेटघाट तयारी" else "Meetup Prep",
        "Cultural" to if (isNe) "सांस्कृतिक" else "Cultural",
        "Shopping" to if (isNe) "किनमेल" else "Shopping"
    )

    val filteredTodos = remember(todosState, selectedCategory) {
        val list = (todosState as? UiState.Success)?.data.orEmpty()
        if (selectedCategory == "ALL") list
        else list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

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
            MeroCard(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .animateContentSize(tween(200))
            ) {
                MeroButton(
                    text = s.addTodoCta,
                    leadingIcon = Icons.Filled.Add,
                    onClick = onAddNew,
                    testTag = "todos_add_new_button"
                )

                val allItems = (todosState as? UiState.Success)?.data.orEmpty()
                if (allItems.isNotEmpty()) {
                    val doneCount = allItems.count { it.completed }
                    val ratio = doneCount.toFloat() / allItems.size.coerceAtLeast(1).toFloat()
                    val animatedProgress by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(240),
                        label = "todo_progress"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isNe) "पूर्ण भएका कार्यहरू" else "Completed Tasks",
                                style = MeroType.Caption.copy(color = colors.textMuted, fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "$doneCount / ${allItems.size}",
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = colors.success,
                            trackColor = colors.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(MeroRadius.pill))
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    categoryOptions.forEach { (catKey, catLabel) ->
                        MeroChip(
                            label = catLabel,
                            selected = selectedCategory == catKey,
                            onClick = { selectedCategory = catKey },
                            testTag = "todo_filter_${catKey.lowercase().replace(" ", "_")}"
                        )
                    }
                }
            }
        }

        when (todosState) {
            is UiState.Loading -> item { MeroLoadingState() }
            is UiState.Error -> item {
                MeroErrorState(
                    message = todosState.message,
                    retryLabel = s.retry,
                    onRetry = {},
                    modifier = Modifier.widthIn(max = 560.dp)
                )
            }
            is UiState.Success -> {
                if (filteredTodos.isEmpty()) {
                    item {
                        MeroEmptyState(
                            icon = Icons.Filled.CheckCircle,
                            title = s.emptyTodosTitle,
                            body = s.emptyTodosBody,
                            actionLabel = s.addTodoCta,
                            onAction = onAddNew,
                            modifier = Modifier.widthIn(max = 560.dp)
                        )
                    }
                } else {
                    items(filteredTodos, key = { it.id }) { todo ->
                        TodoItemCard(
                            item = todo,
                            language = language,
                            onToggleCompleted = { onToggleCompleted(todo.id, !todo.completed) },
                            onEdit = { onEdit(todo) },
                            onDelete = { onDelete(todo.id) },
                            modifier = Modifier.widthIn(max = 560.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoItemCard(
    item: TodoItem,
    language: AppLanguage,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    val priorityColor = when (item.priority.uppercase()) {
        "HIGH" -> colors.danger
        "LOW" -> colors.success
        else -> colors.primary
    }
    val priorityLabel = when (item.priority.uppercase()) {
        "HIGH" -> if (isNe) "उच्च" else "High"
        "LOW" -> if (isNe) "कम" else "Low"
        else -> if (isNe) "मध्यम" else "Medium"
    }

    MeroCard(
        modifier = modifier
            .animateContentSize(tween(200))
            .testTag("todo_card_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            MeroIconButton(
                icon = Icons.Filled.CheckCircle,
                contentDescription = if (item.completed) "Mark incomplete" else "Mark completed",
                tint = if (item.completed) colors.onPrimary else colors.textMuted,
                containerColor = if (item.completed) colors.success else colors.surface2,
                onClick = onToggleCompleted,
                testTag = "todo_toggle_${item.id}"
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleCompleted)
            ) {
                Text(
                    text = item.title,
                    style = MeroType.Heading.copy(
                        color = if (item.completed) colors.textMuted else colors.text,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = priorityColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = MeroType.Caption.copy(color = priorityColor, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.xs)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.surface2
                    ) {
                        Text(
                            text = item.category.ifBlank { "Personal" },
                            style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.xs)
                        )
                    }
                    if (item.dueDateLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.pill),
                            color = colors.surface2
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                                modifier = Modifier.padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.dueDateLabel,
                                    style = MeroType.Caption.copy(color = colors.textMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Row {
                MeroIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = s.editProfile,
                    tint = colors.primary,
                    onClick = onEdit,
                    testTag = "todo_edit_${item.id}"
                )
                MeroIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = s.confirm,
                    tint = colors.danger,
                    onClick = onDelete,
                    testTag = "todo_delete_${item.id}"
                )
            }
        }

        if (item.notes.isNotBlank()) {
            Text(
                text = item.notes,
                style = MeroType.Body.copy(
                    color = if (item.completed) colors.textMuted else colors.text
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaveTodoSheet(
    language: AppLanguage,
    editingTodo: TodoItem? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        notes: String,
        category: String,
        priority: String,
        dueDateLabel: String,
        completed: Boolean
    ) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var title by remember(editingTodo) { mutableStateOf(editingTodo?.title.orEmpty()) }
    var notes by remember(editingTodo) { mutableStateOf(editingTodo?.notes.orEmpty()) }
    var category by remember(editingTodo) { mutableStateOf(editingTodo?.category?.ifBlank { "Personal" } ?: "Personal") }
    var priority by remember(editingTodo) { mutableStateOf(editingTodo?.priority?.ifBlank { "MEDIUM" } ?: "MEDIUM") }
    var dueDateLabel by remember(editingTodo) { mutableStateOf(editingTodo?.dueDateLabel.orEmpty()) }

    val categories = listOf(
        "Personal" to if (isNe) "व्यक्तिगत" else "Personal",
        "Meetup Prep" to if (isNe) "भेटघाट तयारी" else "Meetup Prep",
        "Cultural" to if (isNe) "सांस्कृतिक" else "Cultural",
        "Shopping" to if (isNe) "किनमेल" else "Shopping"
    )
    val priorities = listOf(
        "HIGH" to if (isNe) "उच्च" else "High",
        "MEDIUM" to if (isNe) "मध्यम" else "Medium",
        "LOW" to if (isNe) "कम" else "Low"
    )

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
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    text = if (editingTodo != null) {
                        if (isNe) "कार्य सम्पादन गर्नुहोस्" else "Edit To-Do Task"
                    } else {
                        s.addTodoCta
                    },
                    style = MeroType.Heading.copy(color = colors.text)
                )
            }

            MeroInput(
                label = if (isNe) "कार्यको शीर्षक" else "Task Title",
                value = title,
                onValueChange = { if (it.length <= 140) title = it },
                placeholder = if (isNe) "जस्तै: पाटन चिया भेटको तयारी" else "e.g., Book table at Patan courtyard cafe",
                testTag = "todo_title_input"
            )

            Text(
                text = if (isNe) "श्रेणी (Category)" else "Category",
                style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                categories.forEach { (catKey, catLabel) ->
                    MeroChip(
                        label = catLabel,
                        selected = category == catKey,
                        onClick = { category = catKey },
                        testTag = "todo_sheet_cat_${catKey.lowercase().replace(" ", "_")}"
                    )
                }
            }

            Text(
                text = if (isNe) "प्राथमिकता (Priority)" else "Priority",
                style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                priorities.forEach { (prioKey, prioLabel) ->
                    MeroChip(
                        label = prioLabel,
                        selected = priority == prioKey,
                        onClick = { priority = prioKey },
                        modifier = Modifier.weight(1f),
                        testTag = "todo_sheet_priority_${prioKey.lowercase()}"
                    )
                }
            }

            MeroInput(
                label = if (isNe) "समय वा मिति (ऐच्छिक)" else "Due Date / Reminder (Optional)",
                value = dueDateLabel,
                onValueChange = { if (it.length <= 60) dueDateLabel = it },
                placeholder = if (isNe) "जस्तै: भोलि साँझ ५ बजे" else "e.g., Tomorrow 5 PM, This Saturday",
                testTag = "todo_due_date_input"
            )

            MeroInput(
                label = if (isNe) "थप विवरण (ऐच्छिक)" else "Notes (Optional)",
                value = notes,
                onValueChange = { if (it.length <= 600) notes = it },
                placeholder = if (isNe) "साथमा लग्नुपर्ने कुरा वा सम्झना..." else "Details, items to pack, or meetup checklist notes...",
                singleLine = false,
                maxLines = 4,
                testTag = "todo_notes_input"
            )

            MeroButton(
                text = s.saveChanges,
                leadingIcon = Icons.Filled.CheckCircle,
                enabled = title.trim().isNotEmpty() && !isBusy,
                isLoading = isBusy,
                onClick = {
                    onSave(
                        editingTodo?.id,
                        title.trim(),
                        notes.trim(),
                        category,
                        priority,
                        dueDateLabel.trim(),
                        editingTodo?.completed ?: false
                    )
                },
                testTag = "todo_save_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}
