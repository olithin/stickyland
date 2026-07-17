package com.mynotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynotes.data.NoteDropZone
import com.mynotes.data.NoteTreeItem
import com.mynotes.ui.AppViewModel
import com.mynotes.ui.theme.AppColors
import java.time.format.DateTimeFormatter

private data class RowLayout(
    val id: String,
    val top: Float,
    val bottom: Float
)

@Composable
fun NoteTreeList(
    viewModel: AppViewModel,
    treeItems: List<NoteTreeItem>,
    selectedNoteId: String?,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    var draggedNoteId by remember { mutableStateOf<String?>(null) }
    var rowLayouts by remember { mutableStateOf<Map<String, RowLayout>>(emptyMap()) }
    var hoverTarget by remember { mutableStateOf<Pair<String, NoteDropZone>?>(null) }
    var dragAnchorY by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun updateHover(pointerY: Float) {
        val match = rowLayouts.entries.firstOrNull { (_, layout) ->
            pointerY in layout.top..layout.bottom
        }
        hoverTarget = match?.let { (id, layout) ->
            val relativeY = pointerY - layout.top
            val height = (layout.bottom - layout.top).coerceAtLeast(1f)
            val zone = when {
                relativeY < height * 0.25f -> NoteDropZone.BEFORE
                relativeY > height * 0.75f -> NoteDropZone.AFTER
                else -> NoteDropZone.INSIDE
            }
            id to zone
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        if (treeItems.isEmpty()) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📄", fontSize = 32.sp)
                Text(
                    "No notes",
                    modifier = Modifier.padding(top = 8.dp),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp
                )
                NotionTextButton(
                    text = "Create first note",
                    onClick = { viewModel.createNote() },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        treeItems.forEach { item ->
            val note = item.note
            val isDragging = draggedNoteId == note.id
            val isHovered = hoverTarget?.first == note.id
            val dropZone = if (isHovered) hoverTarget?.second else null

            NoteTreeRow(
                item = item,
                selected = selectedNoteId == note.id,
                dateFormatter = dateFormatter,
                isDragging = isDragging,
                dropZone = dropZone,
                onSelect = { viewModel.selectNote(note.id) },
                onToggleExpand = { viewModel.toggleNoteExpanded(note.id) },
                onCreateChild = { viewModel.createNote(parentId = note.id) },
                onLayout = { top, bottom ->
                    rowLayouts = rowLayouts + (note.id to RowLayout(note.id, top, bottom))
                },
                onDragStart = { anchorY ->
                    draggedNoteId = note.id
                    dragAnchorY = anchorY
                    dragOffsetY = 0f
                },
                onDrag = { deltaY ->
                    dragOffsetY += deltaY
                    updateHover(dragAnchorY + dragOffsetY)
                },
                onDragEnd = {
                    val dragId = draggedNoteId
                    val target = hoverTarget
                    if (dragId != null && target != null && dragId != target.first) {
                        viewModel.moveNote(dragId, target.first, target.second)
                    }
                    draggedNoteId = null
                    hoverTarget = null
                    dragOffsetY = 0f
                }
            )
        }
    }
}

@Composable
private fun NoteTreeRow(
    item: NoteTreeItem,
    selected: Boolean,
    dateFormatter: DateTimeFormatter,
    isDragging: Boolean,
    dropZone: NoteDropZone?,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onCreateChild: () -> Unit,
    onLayout: (top: Float, bottom: Float) -> Unit,
    onDragStart: (anchorY: Float) -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val note = item.note
    val indent = (item.depth * 16).dp
    var handleCenterY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onLayout(pos.y, pos.y + coords.size.height)
            }
    ) {
        if (dropZone == NoteDropZone.BEFORE) {
            DropIndicator()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        dropZone == NoteDropZone.INSIDE -> AppColors.Accent.copy(alpha = 0.12f)
                        selected -> AppColors.SidebarSelected
                        else -> Color.Transparent
                    },
                    RoundedCornerShape(4.dp)
                )
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        handleCenterY = pos.y + coords.size.height / 2f
                    }
                    .pointerInput(note.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(handleCenterY) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⠿",
                    fontSize = 16.sp,
                    color = if (isDragging) AppColors.Accent else AppColors.TextMuted
                )
            }

            if (item.hasChildren) {
                NotionIconButton(onClick = onToggleExpand) {
                    Text(
                        text = if (item.isExpanded) "▼" else "▶",
                        fontSize = 12.sp,
                        color = AppColors.TextMuted
                    )
                }
            } else {
                Spacer(Modifier.width(32.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                SidebarItem(
                    icon = if (item.hasChildren) "📁" else "📄",
                    title = note.title,
                    selected = selected,
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    note.createdAt.format(dateFormatter),
                    fontSize = 11.sp,
                    color = AppColors.TextMuted,
                    modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                )
            }

            NotionIconButton(onClick = onCreateChild) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Sub-note",
                    tint = AppColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (dropZone == NoteDropZone.AFTER) {
            DropIndicator()
        }
    }
}

@Composable
private fun DropIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(2.dp)
            .background(AppColors.Accent, RoundedCornerShape(1.dp))
    )
}

@Composable
fun SuiteTreeList(
    suites: List<com.mynotes.data.Suite>,
    selectedSuiteId: String?,
    onSelect: (String) -> Unit,
    onRename: (com.mynotes.data.Suite) -> Unit,
    onDelete: (com.mynotes.data.Suite) -> Unit,
    onMove: (draggedId: String, targetId: String, insertBefore: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedSuiteId by remember { mutableStateOf<String?>(null) }
    var rowLayouts by remember { mutableStateOf<Map<String, RowLayout>>(emptyMap()) }
    var hoverTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var dragAnchorY by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun updateHover(pointerY: Float) {
        val match = rowLayouts.entries.firstOrNull { (_, layout) ->
            pointerY in layout.top..layout.bottom
        }
        hoverTarget = match?.let { (id, layout) ->
            val relativeY = pointerY - layout.top
            val height = (layout.bottom - layout.top).coerceAtLeast(1f)
            id to (relativeY < height / 2f)
        }
    }

    Column(modifier = modifier) {
        suites.forEach { suite ->
            val isDragging = draggedSuiteId == suite.id
            val isHovered = hoverTarget?.first == suite.id
            val insertBefore = hoverTarget?.second ?: true
            var handleCenterY by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        rowLayouts = rowLayouts + (suite.id to RowLayout(suite.id, pos.y, pos.y + coords.size.height))
                    }
            ) {
                if (isHovered && insertBefore) DropIndicator()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isHovered && !insertBefore -> AppColors.Accent.copy(alpha = 0.08f)
                                selectedSuiteId == suite.id -> AppColors.SidebarSelected
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(4.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                handleCenterY = pos.y + coords.size.height / 2f
                            }
                            .pointerInput(suite.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedSuiteId = suite.id
                                        dragAnchorY = handleCenterY
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        val drag = draggedSuiteId
                                        val target = hoverTarget
                                        if (drag != null && target != null && drag != target.first) {
                                            onMove(drag, target.first, target.second)
                                        }
                                        draggedSuiteId = null
                                        hoverTarget = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedSuiteId = null
                                        hoverTarget = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        updateHover(dragAnchorY + dragOffsetY)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⠿",
                            fontSize = 14.sp,
                            color = if (isDragging) AppColors.Accent else AppColors.TextMuted
                        )
                    }

                    SidebarItem(
                        icon = suite.icon,
                        title = suite.name,
                        selected = selectedSuiteId == suite.id,
                        onClick = { onSelect(suite.id) },
                        modifier = Modifier.weight(1f),
                        trailing = {
                            Row {
                                NotionIconButton(onClick = { onRename(suite) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = AppColors.TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (suites.size > 1) {
                                    NotionIconButton(onClick = { onDelete(suite) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = AppColors.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (isHovered && !insertBefore) DropIndicator()
            }
        }
    }
}
