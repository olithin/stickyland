package com.mynotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynotes.data.NoteDropZone
import com.mynotes.data.NoteTreeItem
import com.mynotes.ui.AppViewModel
import com.mynotes.ui.DropRect
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
    var pointerX by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }

    fun updateNoteHover(y: Float) {
        val match = rowLayouts.entries.firstOrNull { (_, layout) ->
            y in layout.top..layout.bottom
        }
        hoverTarget = match?.let { (id, layout) ->
            val relativeY = y - layout.top
            val height = (layout.bottom - layout.top).coerceAtLeast(1f)
            val zone = when {
                relativeY < height * 0.25f -> NoteDropZone.BEFORE
                relativeY > height * 0.75f -> NoteDropZone.AFTER
                else -> NoteDropZone.INSIDE
            }
            id to zone
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(vertical = 4.dp)
    ) {
        if (treeItems.isEmpty()) {
            item(key = "empty-notes") {
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
        }

        items(treeItems, key = { it.note.id }) { item ->
            val note = item.note
            val isDragging = draggedNoteId == note.id
            // Prefer suite drop highlight when pointer is over another topic.
            val suiteHover = viewModel.state.noteDragHoverSuiteId
            val isHovered = suiteHover == null && hoverTarget?.first == note.id
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
                onDragStart = { x, y ->
                    draggedNoteId = note.id
                    pointerX = x
                    pointerY = y
                    viewModel.beginNoteDrag(note.id)
                    updateNoteHover(y)
                },
                onDrag = { dx, dy ->
                    pointerX += dx
                    pointerY += dy
                    viewModel.updateNoteDragPointer(pointerX, pointerY)
                    // Only show in-list zones when not over another suite.
                    if (viewModel.state.noteDragHoverSuiteId == null) {
                        updateNoteHover(pointerY)
                    } else {
                        hoverTarget = null
                    }
                },
                onDragEnd = {
                    val dragId = draggedNoteId
                    val targetSuiteId = viewModel.state.noteDragHoverSuiteId
                    val target = hoverTarget
                    when {
                        dragId != null && targetSuiteId != null -> {
                            viewModel.dropNoteOnSuite(dragId, targetSuiteId)
                        }
                        dragId != null && target != null && dragId != target.first -> {
                            viewModel.moveNote(dragId, target.first, target.second)
                            viewModel.clearNoteDrag()
                        }
                        else -> viewModel.clearNoteDrag()
                    }
                    draggedNoteId = null
                    hoverTarget = null
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
    onDragStart: (x: Float, y: Float) -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val note = item.note
    val indent = (item.depth * 16).dp
    var handleCenterX by remember { mutableFloatStateOf(0f) }
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
                        handleCenterX = pos.x + coords.size.width / 2f
                        handleCenterY = pos.y + coords.size.height / 2f
                    }
                    .pointerInput(note.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart(handleCenterX, handleCenterY) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
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
    noteDragHoverSuiteId: String?,
    onSelect: (String) -> Unit,
    onRename: (com.mynotes.data.Suite) -> Unit,
    onDelete: (com.mynotes.data.Suite) -> Unit,
    onMove: (draggedId: String, targetId: String, insertBefore: Boolean) -> Unit,
    onRegisterDropTarget: (suiteId: String, rect: DropRect) -> Unit,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        items(suites, key = { it.id }) { suite ->
            val isDragging = draggedSuiteId == suite.id
            val isNoteDropTarget = noteDragHoverSuiteId == suite.id
            val isHovered = hoverTarget?.first == suite.id
            val insertBefore = hoverTarget?.second ?: true
            var handleCenterY by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val left = pos.x
                        val top = pos.y
                        val right = pos.x + coords.size.width
                        val bottom = pos.y + coords.size.height
                        rowLayouts = rowLayouts + (suite.id to RowLayout(suite.id, top, bottom))
                        onRegisterDropTarget(suite.id, DropRect(left, top, right, bottom))
                    }
            ) {
                if (isHovered && !isNoteDropTarget && insertBefore) DropIndicator()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isNoteDropTarget -> AppColors.Accent.copy(alpha = 0.18f)
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
                                detectDragGestures(
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
                            color = if (isDragging || isNoteDropTarget) AppColors.Accent else AppColors.TextMuted
                        )
                    }

                    SidebarItem(
                        icon = suite.icon,
                        title = suite.name,
                        selected = selectedSuiteId == suite.id || isNoteDropTarget,
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

                if (isHovered && !isNoteDropTarget && !insertBefore) DropIndicator()
            }
        }
    }
}
