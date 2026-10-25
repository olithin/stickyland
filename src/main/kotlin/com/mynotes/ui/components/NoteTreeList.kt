package com.mynotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    fun titleOf(id: String): String =
        treeItems.firstOrNull { it.note.id == id }?.note?.title?.ifBlank { "Untitled" } ?: "note"

    fun updateNoteHover(y: Float) {
        val match = rowLayouts.entries.firstOrNull { (id, layout) ->
            id != draggedNoteId && y in layout.top..layout.bottom
        }
        hoverTarget = match?.let { (id, layout) ->
            val relativeY = y - layout.top
            val height = (layout.bottom - layout.top).coerceAtLeast(1f)
            val zone = when {
                relativeY < height * 0.28f -> NoteDropZone.BEFORE
                relativeY > height * 0.72f -> NoteDropZone.AFTER
                else -> NoteDropZone.INSIDE
            }
            id to zone
        }
        val target = hoverTarget
        val hint = when {
            target == null -> "Drop to reorder"
            target.second == NoteDropZone.BEFORE -> "Place above «${titleOf(target.first)}»"
            target.second == NoteDropZone.AFTER -> "Place below «${titleOf(target.first)}»"
            else -> "Nest inside «${titleOf(target.first)}»"
        }
        viewModel.setNoteDragHint(hint)
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
                    viewModel.updateNoteDragPointer(x, y)
                },
                onDrag = { dx, dy ->
                    pointerX += dx
                    pointerY += dy
                    viewModel.updateNoteDragPointer(pointerX, pointerY)
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
            .alpha(if (isDragging) 0.35f else 1f)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onLayout(pos.y, pos.y + coords.size.height)
            }
    ) {
        if (dropZone == NoteDropZone.BEFORE) {
            DropLine(label = "above")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (dropZone == NoteDropZone.INSIDE) {
                        Modifier
                            .border(2.dp, AppColors.Accent, RoundedCornerShape(6.dp))
                            .background(AppColors.Accent.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                    } else {
                        Modifier.background(
                            if (selected) AppColors.SidebarSelected else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                    }
                )
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DragHandle(
                active = isDragging,
                modifier = Modifier
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
                    }
            )

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
                if (dropZone == NoteDropZone.INSIDE) {
                    Text(
                        "↳ nest inside",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Accent,
                        modifier = Modifier.padding(start = 28.dp, bottom = 2.dp)
                    )
                } else {
                    Text(
                        note.createdAt.format(dateFormatter),
                        fontSize = 11.sp,
                        color = AppColors.TextMuted,
                        modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                    )
                }
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
            DropLine(label = "below")
        }
    }
}

@Composable
fun DragHandle(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .size(width = 28.dp, height = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (active) AppColors.Accent.copy(alpha = 0.18f) else AppColors.SidebarHover
            )
            .border(
                width = 1.dp,
                color = if (active) AppColors.Accent else AppColors.Border,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) {
                        Box(
                            Modifier
                                .size(3.dp)
                                .background(
                                    if (active) AppColors.Accent else AppColors.TextMuted,
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DropLine(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(AppColors.Accent, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Accent,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .background(AppColors.Accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(AppColors.Accent, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Floating card that follows the pointer (Notion / Linear style):
 * grab handle stays visible near the cursor, soft shadow, live drop hint.
 */
@Composable
fun FloatingDragGhost(
    title: String,
    hint: String?,
    leadingIcon: String = "📄",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(min = 180.dp, max = 280.dp)
            .graphicsLayer {
                scaleX = 1.03f
                scaleY = 1.03f
                alpha = 0.97f
            }
            .shadow(16.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.18f))
            .border(1.dp, AppColors.Accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .background(AppColors.Card, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DragHandle(active = true)
            Spacer(Modifier.width(8.dp))
            Text(leadingIcon, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (!hint.isNullOrBlank()) {
            Text(
                hint,
                fontSize = 11.sp,
                color = AppColors.Accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 36.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** @deprecated Use [FloatingDragGhost] — kept name for call sites during migration. */
@Composable
fun NoteDragGhost(
    title: String,
    hint: String?,
    modifier: Modifier = Modifier
) {
    FloatingDragGhost(title = title, hint = hint, leadingIcon = "📄", modifier = modifier)
}

@Composable
fun SuiteTreeList(
    suites: List<com.mynotes.data.Suite>,
    selectedSuiteId: String?,
    noteDragHoverSuiteId: String?,
    draggingSuiteId: String?,
    onSelect: (String) -> Unit,
    onRename: (com.mynotes.data.Suite) -> Unit,
    onDelete: (com.mynotes.data.Suite) -> Unit,
    onMove: (draggedId: String, targetId: String, insertBefore: Boolean) -> Unit,
    onRegisterDropTarget: (suiteId: String, rect: DropRect) -> Unit,
    onDragStart: (suiteId: String, x: Float, y: Float) -> Unit,
    onDrag: (x: Float, y: Float) -> Unit,
    onDragHint: (hint: String?) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rowLayouts by remember { mutableStateOf<Map<String, RowLayout>>(emptyMap()) }
    var hoverTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pointerX by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }

    fun nameOf(id: String): String =
        suites.firstOrNull { it.id == id }?.name ?: "topic"

    fun updateHover(y: Float) {
        val match = rowLayouts.entries.firstOrNull { (id, layout) ->
            id != draggingSuiteId && y in layout.top..layout.bottom
        }
        hoverTarget = match?.let { (id, layout) ->
            val relativeY = y - layout.top
            val height = (layout.bottom - layout.top).coerceAtLeast(1f)
            id to (relativeY < height / 2f)
        }
        val target = hoverTarget
        onDragHint(
            when {
                target == null -> "Release to cancel"
                target.second -> "Place above «${nameOf(target.first)}»"
                else -> "Place below «${nameOf(target.first)}»"
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        items(suites, key = { it.id }) { suite ->
            val isDragging = draggingSuiteId == suite.id
            val isNoteDropTarget = noteDragHoverSuiteId == suite.id
            val isHovered = !isDragging && hoverTarget?.first == suite.id
            val insertBefore = hoverTarget?.second ?: true
            var handleCenterX by remember { mutableFloatStateOf(0f) }
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
                // Insertion line between rows (modern list reorder pattern).
                if (isHovered && !isNoteDropTarget && insertBefore) {
                    InsertionLine()
                }

                // Keep the row composed while dragging so the gesture isn't cancelled;
                // original stays dimmed (Fiori), floating ghost follows the cursor.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isDragging) 0.32f else 1f)
                        .then(
                            when {
                                isDragging -> Modifier
                                    .border(
                                        1.5.dp,
                                        AppColors.Accent.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        AppColors.Accent.copy(alpha = 0.06f),
                                        RoundedCornerShape(8.dp)
                                    )
                                isNoteDropTarget -> Modifier
                                    .border(2.dp, AppColors.Accent, RoundedCornerShape(8.dp))
                                    .background(
                                        AppColors.Accent.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    )
                                else -> Modifier.background(
                                    if (selectedSuiteId == suite.id) {
                                        AppColors.SidebarSelected
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DragHandle(
                        active = isDragging || isNoteDropTarget,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                handleCenterX = pos.x + coords.size.width / 2f
                                handleCenterY = pos.y + coords.size.height / 2f
                            }
                            .pointerInput(suite.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        pointerX = handleCenterX
                                        pointerY = handleCenterY
                                        hoverTarget = null
                                        onDragStart(suite.id, pointerX, pointerY)
                                    },
                                    onDragEnd = {
                                        val target = hoverTarget
                                        val dragId = suite.id
                                        if (target != null && dragId != target.first) {
                                            onMove(dragId, target.first, target.second)
                                        }
                                        hoverTarget = null
                                        onDragEnd()
                                    },
                                    onDragCancel = {
                                        hoverTarget = null
                                        onDragEnd()
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        pointerX += dragAmount.x
                                        pointerY += dragAmount.y
                                        onDrag(pointerX, pointerY)
                                        updateHover(pointerY)
                                    }
                                )
                            }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        SidebarItem(
                            icon = suite.icon,
                            title = suite.name,
                            selected = selectedSuiteId == suite.id || isNoteDropTarget,
                            onClick = { onSelect(suite.id) },
                            modifier = Modifier.fillMaxWidth(),
                            trailing = {
                                if (!isDragging) {
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
                            }
                        )
                        if (isNoteDropTarget) {
                            Text(
                                "↓ Move note here",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Accent,
                                modifier = Modifier.padding(start = 36.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                if (isHovered && !isNoteDropTarget && !insertBefore) {
                    InsertionLine()
                }
            }
        }
    }
}

@Composable
private fun InsertionLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .height(3.dp)
            .background(AppColors.Accent, RoundedCornerShape(2.dp))
    )
}
