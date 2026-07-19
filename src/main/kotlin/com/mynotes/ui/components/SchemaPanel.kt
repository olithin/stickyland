package com.mynotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.mynotes.data.SchemaEdge
import com.mynotes.data.SchemaNode
import com.mynotes.data.SchemaPort
import com.mynotes.ui.AppViewModel
import com.mynotes.ui.theme.AppColors
import com.mynotes.ui.theme.Brand
import com.mynotes.ui.theme.OlithinMark
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private const val EDGE_HIT_PX = 14f

private data class WireDrag(
    val fromNodeId: String,
    val fromPort: SchemaPort,
    val pointerScreen: Offset
)

@Composable
fun SchemaPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    var showHintEditor by remember { mutableStateOf<SchemaNode?>(null) }
    var showRebuildConfirm by remember { mutableStateOf(false) }
    var editingTitleId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        SchemaToolbar(
            hasSelection = state.selectedSchemaNodeId != null || state.selectedSchemaEdgeId != null,
            edgeSelected = state.selectedSchemaEdgeId != null,
            onAddCard = { viewModel.createSchemaCard() },
            onAddChild = {
                state.selectedSchemaNodeId?.let { viewModel.createSchemaCard(parentId = it) }
            },
            onDelete = viewModel::deleteSelectedSchema,
            onAutoBuild = { showRebuildConfirm = true },
            canAddChild = state.selectedSchemaNodeId != null
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF3F2EE))
        ) {
            SchemaCanvas(
                nodes = state.schemaGraph.nodes,
                edges = state.schemaGraph.edges,
                selectedNodeId = state.selectedSchemaNodeId,
                selectedEdgeId = state.selectedSchemaEdgeId,
                panX = state.schemaPanX,
                panY = state.schemaPanY,
                zoom = state.schemaZoom,
                editingTitleId = editingTitleId,
                onPanZoom = viewModel::setSchemaPanZoom,
                onSelectNode = { id ->
                    editingTitleId = null
                    viewModel.selectSchemaNode(id)
                },
                onSelectEdge = viewModel::selectSchemaEdge,
                onMoveNode = viewModel::moveSchemaNode,
                onResizeNode = viewModel::resizeSchemaNode,
                onConnect = { fromId, toId, fox, foy, tox, toy ->
                    viewModel.connectSchemaNodes(fromId, toId, fox, foy, tox, toy)
                },
                onDeleteSelection = viewModel::deleteSelectedSchema,
                onToggleCollapse = viewModel::toggleSchemaNodeCollapsed,
                onOpenHint = { showHintEditor = it },
                onTitleChange = viewModel::updateSchemaNodeTitle,
                onStartEditTitle = { editingTitleId = it },
                onEndEditTitle = { editingTitleId = null },
                onOpenNote = viewModel::openLinkedNote
            )

            if (state.schemaGraph.nodes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Project Schema",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "Hover a card · drag a port (·) to another card",
                        modifier = Modifier.padding(top = 8.dp),
                        color = AppColors.TextMuted,
                        fontSize = 14.sp
                    )
                    OlithinMark(modifier = Modifier.padding(top = 12.dp))
                }
            }

            Text(
                "Drag corner ▢ to resize · port → port · Delete removes selection",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                fontSize = 11.sp,
                color = AppColors.TextMuted
            )
            OlithinMark(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }

    showHintEditor?.let { node ->
        InputDialog(
            title = "Hint · ${node.title}",
            initialValue = node.hint,
            placeholder = "Why this block exists…",
            confirmText = "Save",
            allowBlank = true,
            onConfirm = { text ->
                viewModel.updateSchemaNodeHint(node.id, text)
                showHintEditor = null
            },
            onDismiss = { showHintEditor = null }
        )
    }

    if (showRebuildConfirm) {
        ConfirmDialog(
            title = "Build schema from notes?",
            message = "This replaces the current schema for this suite with a map built from your notes tree.",
            confirmText = "Build",
            onConfirm = {
                viewModel.autoBuildSchemaFromNotes()
                showRebuildConfirm = false
            },
            onDismiss = { showRebuildConfirm = false }
        )
    }
}

@Composable
private fun SchemaToolbar(
    hasSelection: Boolean,
    edgeSelected: Boolean,
    onAddCard: () -> Unit,
    onAddChild: () -> Unit,
    onDelete: () -> Unit,
    onAutoBuild: () -> Unit,
    canAddChild: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Card)
            .border(1.dp, AppColors.Border)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Schema", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppColors.TextPrimary)
        Text("· ${Brand.STUDIO}", fontSize = 12.sp, color = AppColors.TextMuted)
        Spacer(Modifier.weight(1f))
        NotionButton(
            text = "+ Card",
            onClick = onAddCard,
            backgroundColor = AppColors.InputBackground,
            contentColor = AppColors.TextPrimary
        )
        NotionButton(
            text = "+ Child",
            onClick = onAddChild,
            enabled = canAddChild,
            backgroundColor = AppColors.InputBackground,
            contentColor = AppColors.TextPrimary
        )
        NotionButton(
            text = "Build from notes",
            onClick = onAutoBuild,
            backgroundColor = AppColors.Accent,
            contentColor = Color.White
        )
        NotionButton(
            text = if (edgeSelected) "Delete arrow" else "Delete",
            onClick = onDelete,
            enabled = hasSelection,
            backgroundColor = AppColors.Danger.copy(alpha = 0.1f),
            contentColor = AppColors.Danger
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SchemaCanvas(
    nodes: List<SchemaNode>,
    edges: List<SchemaEdge>,
    selectedNodeId: String?,
    selectedEdgeId: String?,
    panX: Float,
    panY: Float,
    zoom: Float,
    editingTitleId: String?,
    onPanZoom: (Float, Float, Float) -> Unit,
    onSelectNode: (String?) -> Unit,
    onSelectEdge: (String?) -> Unit,
    onMoveNode: (String, Double, Double) -> Unit,
    onResizeNode: (String, Double, Double) -> Unit,
    onConnect: (fromId: String, toId: String, fromOx: Double, fromOy: Double, toOx: Double, toOy: Double) -> Unit,
    onDeleteSelection: () -> Unit,
    onToggleCollapse: (String) -> Unit,
    onOpenHint: (SchemaNode) -> Unit,
    onTitleChange: (String, String) -> Unit,
    onStartEditTitle: (String) -> Unit,
    onEndEditTitle: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val visible = remember(nodes) { visibleNodes(nodes) }
    val visibleIds = remember(visible) { visible.map { it.id }.toSet() }
    val visibleEdges = remember(edges, visibleIds) {
        edges.filter { it.fromNodeId in visibleIds && it.toNodeId in visibleIds }
    }
    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    var dragPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var resizeSizes by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var wireDrag by remember { mutableStateOf<WireDrag?>(null) }
    var canvasOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    fun displayPos(node: SchemaNode): Offset =
        dragPositions[node.id] ?: Offset(node.posX.toFloat(), node.posY.toFloat())

    fun displaySize(node: SchemaNode): Offset =
        resizeSizes[node.id] ?: Offset(node.w(), node.h())

    fun portPoint(
        node: SchemaNode,
        offsetX: Float,
        offsetY: Float,
        size: Offset = displaySize(node)
    ): Offset {
        val port = SchemaPort.nearest(offsetX, offsetY, node.w(), node.h())
        val o = port.offset(size.x, size.y)
        return Offset(o.x, o.y)
    }

    val snapTargetId = wireDrag?.let { wire ->
        hitTestNode(wire.pointerScreen, visible, dragPositions, resizeSizes, panX, panY, zoom)
            ?.takeIf { it.id != wire.fromNodeId }
            ?.id
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(selectedEdgeId, selectedNodeId, editingTitleId) {
        if (editingTitleId == null && (selectedEdgeId != null || selectedNodeId != null)) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (editingTitleId != null) return@onPreviewKeyEvent false
                val isDelete = event.key == Key.Delete || event.key == Key.Backspace
                if (isDelete && (selectedEdgeId != null || selectedNodeId != null)) {
                    onDeleteSelection()
                    true
                } else {
                    false
                }
            }
            .onGloballyPositioned { coords ->
                canvasOriginInRoot = coords.positionInRoot()
            }
            .pointerInput(panX, panY, zoom, visibleEdges, nodeMap, dragPositions, resizeSizes) {
                detectTapGestures { tap ->
                    val hitEdge = hitTestEdge(
                        tap, visibleEdges, nodeMap, dragPositions, resizeSizes, panX, panY, zoom
                    )
                    if (hitEdge != null) {
                        onSelectEdge(hitEdge.id)
                        focusRequester.requestFocus()
                    } else {
                        onSelectNode(null)
                        onSelectEdge(null)
                    }
                }
            }
            .pointerInput(panX, panY, zoom, wireDrag) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        if (wireDrag == null) {
                            change.consume()
                            onPanZoom(panX + drag.x, panY + drag.y, zoom)
                        }
                    }
                )
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (scroll != 0f) {
                    val factor = if (scroll < 0) 1.08f else 0.92f
                    onPanZoom(panX, panY, zoom * factor)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            visibleEdges.forEach { edge ->
                val from = nodeMap[edge.fromNodeId] ?: return@forEach
                val to = nodeMap[edge.toNodeId] ?: return@forEach
                val fromPos = displayPos(from)
                val toPos = displayPos(to)
                val fromPort = portPoint(from, edge.fromOffsetX.toFloat(), edge.fromOffsetY.toFloat())
                val toPort = portPoint(to, edge.toOffsetX.toFloat(), edge.toOffsetY.toFloat())
                val start = worldToScreen(
                    fromPos.x + fromPort.x,
                    fromPos.y + fromPort.y,
                    panX, panY, zoom
                )
                val end = worldToScreen(
                    toPos.x + toPort.x,
                    toPos.y + toPort.y,
                    panX, panY, zoom
                )
                val selected = edge.id == selectedEdgeId
                drawArrow(
                    start,
                    end,
                    if (selected) AppColors.Danger else AppColors.Accent.copy(alpha = 0.8f),
                    if (selected) 3.5f * zoom else 2.5f * zoom
                )
            }

            wireDrag?.let { wire ->
                val from = nodeMap[wire.fromNodeId] ?: return@let
                val fromPos = displayPos(from)
                val fromSize = displaySize(from)
                val fromOff = wire.fromPort.offset(fromSize.x, fromSize.y)
                val start = worldToScreen(
                    fromPos.x + fromOff.x,
                    fromPos.y + fromOff.y,
                    panX, panY, zoom
                )
                val hover = hitTestNode(wire.pointerScreen, visible, dragPositions, resizeSizes, panX, panY, zoom)
                val end = if (hover != null && hover.id != wire.fromNodeId) {
                    val hoverPos = displayPos(hover)
                    val hoverSize = displaySize(hover)
                    val hoverTopLeft = worldToScreen(hoverPos.x, hoverPos.y, panX, panY, zoom)
                    val localX = (wire.pointerScreen.x - hoverTopLeft.x) / zoom
                    val localY = (wire.pointerScreen.y - hoverTopLeft.y) / zoom
                    val port = SchemaPort.nearest(localX, localY, hoverSize.x, hoverSize.y)
                    val o = port.offset(hoverSize.x, hoverSize.y)
                    worldToScreen(hoverPos.x + o.x, hoverPos.y + o.y, panX, panY, zoom)
                } else {
                    wire.pointerScreen
                }
                drawArrow(start, end, AppColors.Accent, 2.5f * zoom)
            }
        }

        visible.forEach { node ->
            val hasChildren = nodes.any { it.parentId == node.id }
            val pos = displayPos(node)
            val size = displaySize(node)
            SchemaCard(
                node = node,
                worldX = pos.x,
                worldY = pos.y,
                cardW = size.x,
                cardH = size.y,
                selected = node.id == selectedNodeId,
                isSnapTarget = node.id == snapTargetId,
                hasChildren = hasChildren,
                editingTitle = node.id == editingTitleId,
                panX = panX,
                panY = panY,
                zoom = zoom,
                canvasOriginInRoot = canvasOriginInRoot,
                onSelect = { onSelectNode(node.id) },
                onDragBy = { dx, dy ->
                    val current = dragPositions[node.id]
                        ?: Offset(node.posX.toFloat(), node.posY.toFloat())
                    dragPositions = dragPositions + (node.id to Offset(
                        current.x + dx / zoom,
                        current.y + dy / zoom
                    ))
                },
                onDragEnd = {
                    val finalPos = dragPositions[node.id]
                    if (finalPos != null) {
                        onMoveNode(node.id, finalPos.x.toDouble(), finalPos.y.toDouble())
                        dragPositions = dragPositions - node.id
                    }
                },
                onResizeBy = { dw, dh ->
                    val current = resizeSizes[node.id] ?: Offset(node.w(), node.h())
                    resizeSizes = resizeSizes + (node.id to Offset(
                        (current.x + dw / zoom).coerceIn(
                            SchemaEdge.CARD_MIN_W.toFloat(),
                            SchemaEdge.CARD_MAX_W.toFloat()
                        ),
                        (current.y + dh / zoom).coerceIn(
                            SchemaEdge.CARD_MIN_H.toFloat(),
                            SchemaEdge.CARD_MAX_H.toFloat()
                        )
                    ))
                },
                onResizeEnd = {
                    val finalSize = resizeSizes[node.id]
                    if (finalSize != null) {
                        onResizeNode(node.id, finalSize.x.toDouble(), finalSize.y.toDouble())
                        resizeSizes = resizeSizes - node.id
                    }
                },
                onWireStart = { port, localOnCanvas ->
                    wireDrag = WireDrag(node.id, port, localOnCanvas)
                },
                onWireMove = { localOnCanvas ->
                    val current = wireDrag
                    if (current != null && current.fromNodeId == node.id) {
                        wireDrag = current.copy(pointerScreen = localOnCanvas)
                    }
                },
                onWireEnd = { localOnCanvas ->
                    val current = wireDrag
                    val target = hitTestNode(
                        localOnCanvas, visible, dragPositions, resizeSizes, panX, panY, zoom
                    )
                    if (current != null && target != null && target.id != node.id) {
                        val fromSize = displaySize(node)
                        val targetSize = displaySize(target)
                        val targetPos = displayPos(target)
                        val topLeftScreen = worldToScreen(targetPos.x, targetPos.y, panX, panY, zoom)
                        val localX = (localOnCanvas.x - topLeftScreen.x) / zoom
                        val localY = (localOnCanvas.y - topLeftScreen.y) / zoom
                        val toPort = SchemaPort.nearest(localX, localY, targetSize.x, targetSize.y)
                        val fromOff = current.fromPort.offset(fromSize.x, fromSize.y)
                        val toOff = toPort.offset(targetSize.x, targetSize.y)
                        onConnect(
                            node.id,
                            target.id,
                            fromOff.x.toDouble(),
                            fromOff.y.toDouble(),
                            toOff.x.toDouble(),
                            toOff.y.toDouble()
                        )
                    }
                    wireDrag = null
                },
                onWireCancel = { wireDrag = null },
                showPorts = selectedNodeId == node.id ||
                    wireDrag?.fromNodeId == node.id ||
                    node.id == snapTargetId,
                onToggleCollapse = { onToggleCollapse(node.id) },
                onOpenHint = { onOpenHint(node) },
                onTitleChange = { onTitleChange(node.id, it) },
                onStartEditTitle = { onStartEditTitle(node.id) },
                onEndEditTitle = onEndEditTitle,
                onOpenNote = { onOpenNote(node.id) }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SchemaCard(
    node: SchemaNode,
    worldX: Float,
    worldY: Float,
    cardW: Float,
    cardH: Float,
    selected: Boolean,
    isSnapTarget: Boolean,
    hasChildren: Boolean,
    editingTitle: Boolean,
    panX: Float,
    panY: Float,
    zoom: Float,
    canvasOriginInRoot: Offset,
    showPorts: Boolean,
    onSelect: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onResizeBy: (Float, Float) -> Unit,
    onResizeEnd: () -> Unit,
    onWireStart: (SchemaPort, Offset) -> Unit,
    onWireMove: (Offset) -> Unit,
    onWireEnd: (Offset) -> Unit,
    onWireCancel: () -> Unit,
    onToggleCollapse: () -> Unit,
    onOpenHint: () -> Unit,
    onTitleChange: (String) -> Unit,
    onStartEditTitle: () -> Unit,
    onEndEditTitle: () -> Unit,
    onOpenNote: () -> Unit
) {
    var showTip by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val screen = worldToScreen(worldX, worldY, panX, panY, zoom)
    val w = (cardW * zoom).dp
    val h = (cardH * zoom).dp
    val portSize = (11f * zoom).coerceIn(9f, 14f).dp
    val resizeHandle = (12f * zoom).coerceIn(10f, 16f).dp
    val portsVisible = showPorts || hovered || isSnapTarget
    val showResize = selected || hovered
    val borderColor = when {
        isSnapTarget -> AppColors.Accent
        selected -> AppColors.Accent
        else -> AppColors.Border
    }
    val borderWidth = if (isSnapTarget) 3.dp else 2.dp
    val portPad = portSize / 2
    val portPadPx = with(density) { portPad.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (screen.x - portPadPx).roundToInt(),
                    (screen.y - portPadPx).roundToInt()
                )
            }
            .padding(portPad)
            .width(w)
            .height(h)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSnapTarget) AppColors.Accent.copy(alpha = 0.12f) else AppColors.Card
                )
                .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
                .pointerInput(node.id, zoom) {
                    detectDragGestures(
                        onDragStart = { onSelect() },
                        onDrag = { change, drag ->
                            change.consume()
                            onDragBy(drag.x, drag.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .clickable(onClick = onSelect)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (editingTitle) {
                        BasicTextField(
                            value = node.title,
                            onValueChange = onTitleChange,
                            textStyle = TextStyle(
                                fontSize = (14 * zoom).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            node.title,
                            fontSize = (14 * zoom).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            maxLines = 2,
                            modifier = Modifier.pointerInput(node.id) {
                                detectTapGestures(
                                    onDoubleTap = { onStartEditTitle() },
                                    onTap = { onSelect() }
                                )
                            }
                        )
                    }
                    if (node.linkedNoteId != null) {
                        Text(
                            "Open note",
                            fontSize = (10 * zoom).sp,
                            color = AppColors.Accent,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(onClick = onOpenNote)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box {
                        Text(
                            "💡",
                            fontSize = (14 * zoom).sp,
                            modifier = Modifier
                                .clickable(onClick = onOpenHint)
                                .onPointerEvent(PointerEventType.Enter) { showTip = true }
                                .onPointerEvent(PointerEventType.Exit) { showTip = false }
                        )
                        if (showTip) {
                            Popup(alignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 220.dp)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.TextPrimary)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = node.hint.ifBlank { "No hint yet — click 💡 to add" },
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    if (hasChildren) {
                        Text(
                            if (node.collapsed) "▸" else "▾",
                            fontSize = (12 * zoom).sp,
                            color = AppColors.TextMuted,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(onClick = onToggleCollapse)
                        )
                    }
                }
            }
        }

        if (portsVisible) {
            SchemaPort.entries.forEach { port ->
                ConnectionPortHandle(
                    port = port,
                    cardWidthDp = w,
                    cardHeightDp = h,
                    cardW = cardW,
                    cardH = cardH,
                    portSize = portSize,
                    canvasOriginInRoot = canvasOriginInRoot,
                    highlighted = isSnapTarget,
                    onSelect = onSelect,
                    onWireStart = { local -> onWireStart(port, local) },
                    onWireMove = onWireMove,
                    onWireEnd = onWireEnd,
                    onWireCancel = onWireCancel
                )
            }
        }

        if (showResize) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = resizeHandle / 3, y = resizeHandle / 3)
                    .size(resizeHandle)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.Accent)
                    .border(1.dp, Color.White, RoundedCornerShape(3.dp))
                    .pointerInput(node.id, zoom) {
                        detectDragGestures(
                            onDragStart = { onSelect() },
                            onDrag = { change, drag ->
                                change.consume()
                                onResizeBy(drag.x, drag.y)
                            },
                            onDragEnd = { onResizeEnd() },
                            onDragCancel = { onResizeEnd() }
                        )
                    }
            )
        }
    }

    LaunchedEffect(selected, editingTitle) {
        if (!selected && editingTitle) onEndEditTitle()
    }
}

@Composable
private fun BoxScope.ConnectionPortHandle(
    port: SchemaPort,
    cardWidthDp: androidx.compose.ui.unit.Dp,
    cardHeightDp: androidx.compose.ui.unit.Dp,
    cardW: Float,
    cardH: Float,
    portSize: androidx.compose.ui.unit.Dp,
    canvasOriginInRoot: Offset,
    highlighted: Boolean,
    onSelect: () -> Unit,
    onWireStart: (Offset) -> Unit,
    onWireMove: (Offset) -> Unit,
    onWireEnd: (Offset) -> Unit,
    onWireCancel: () -> Unit
) {
    val off = port.offset(cardW, cardH)
    val xDp = cardWidthDp * (off.x / cardW.coerceAtLeast(1f)) - portSize / 2
    val yDp = cardHeightDp * (off.y / cardH.coerceAtLeast(1f)) - portSize / 2
    var portOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(portSize)
            .onGloballyPositioned { portOriginInRoot = it.positionInRoot() }
            .clip(CircleShape)
            .background(if (highlighted) AppColors.AccentHover else AppColors.Accent)
            .border(1.5.dp, Color.White, CircleShape)
            .pointerInput(port, canvasOriginInRoot, portOriginInRoot) {
                var lastLocal = Offset.Zero
                detectDragGestures(
                    onDragStart = { startInPort ->
                        onSelect()
                        val local = portOriginInRoot + startInPort - canvasOriginInRoot
                        lastLocal = local
                        onWireStart(local)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val local = portOriginInRoot + change.position - canvasOriginInRoot
                        lastLocal = local
                        onWireMove(local)
                    },
                    onDragEnd = { onWireEnd(lastLocal) },
                    onDragCancel = onWireCancel
                )
            }
    )
}

private fun visibleNodes(nodes: List<SchemaNode>): List<SchemaNode> {
    val byId = nodes.associateBy { it.id }
    fun hiddenByCollapse(node: SchemaNode): Boolean {
        var parentId = node.parentId
        while (parentId != null) {
            val parent = byId[parentId] ?: return false
            if (parent.collapsed) return true
            parentId = parent.parentId
        }
        return false
    }
    return nodes.filterNot { hiddenByCollapse(it) }
}

private fun worldToScreen(x: Float, y: Float, panX: Float, panY: Float, zoom: Float): Offset =
    Offset(x * zoom + panX, y * zoom + panY)

private fun displayWorld(node: SchemaNode, dragPositions: Map<String, Offset>): Offset =
    dragPositions[node.id] ?: Offset(node.posX.toFloat(), node.posY.toFloat())

private fun displayWorldSize(node: SchemaNode, resizeSizes: Map<String, Offset>): Offset =
    resizeSizes[node.id] ?: Offset(node.w(), node.h())

private fun hitTestNode(
    screen: Offset,
    nodes: List<SchemaNode>,
    dragPositions: Map<String, Offset>,
    resizeSizes: Map<String, Offset>,
    panX: Float,
    panY: Float,
    zoom: Float
): SchemaNode? {
    return nodes.asReversed().firstOrNull { node ->
        val pos = displayWorld(node, dragPositions)
        val size = displayWorldSize(node, resizeSizes)
        val topLeft = worldToScreen(pos.x, pos.y, panX, panY, zoom)
        val w = size.x * zoom
        val h = size.y * zoom
        screen.x in topLeft.x..(topLeft.x + w) && screen.y in topLeft.y..(topLeft.y + h)
    }
}

private fun hitTestEdge(
    screen: Offset,
    edges: List<SchemaEdge>,
    nodeMap: Map<String, SchemaNode>,
    dragPositions: Map<String, Offset>,
    resizeSizes: Map<String, Offset>,
    panX: Float,
    panY: Float,
    zoom: Float
): SchemaEdge? {
    var best: SchemaEdge? = null
    var bestDist = EDGE_HIT_PX * zoom
    for (edge in edges) {
        val from = nodeMap[edge.fromNodeId] ?: continue
        val to = nodeMap[edge.toNodeId] ?: continue
        val a = displayWorld(from, dragPositions)
        val b = displayWorld(to, dragPositions)
        val fromSize = displayWorldSize(from, resizeSizes)
        val toSize = displayWorldSize(to, resizeSizes)
        val fromPort = SchemaPort.nearest(
            edge.fromOffsetX.toFloat(),
            edge.fromOffsetY.toFloat(),
            from.w(),
            from.h()
        ).offset(fromSize.x, fromSize.y)
        val toPort = SchemaPort.nearest(
            edge.toOffsetX.toFloat(),
            edge.toOffsetY.toFloat(),
            to.w(),
            to.h()
        ).offset(toSize.x, toSize.y)
        val start = worldToScreen(a.x + fromPort.x, a.y + fromPort.y, panX, panY, zoom)
        val end = worldToScreen(b.x + toPort.x, b.y + toPort.y, panX, panY, zoom)
        val d = distanceToSegment(screen, start, end)
        if (d < bestDist) {
            bestDist = d
            best = edge
        }
    }
    return best
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val len2 = abx * abx + aby * aby
    if (len2 == 0f) return hypot(p.x - a.x, p.y - a.y)
    val t = ((p.x - a.x) * abx + (p.y - a.y) * aby) / len2
    val clamped = t.coerceIn(0f, 1f)
    val proj = Offset(a.x + clamped * abx, a.y + clamped * aby)
    return hypot(p.x - proj.x, p.y - proj.y)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    stroke: Float
) {
    drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLen = 12f * (stroke / 2.5f)
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            (end.x - arrowLen * cos(angle - 0.4)).toFloat(),
            (end.y - arrowLen * sin(angle - 0.4)).toFloat()
        )
        lineTo(
            (end.x - arrowLen * cos(angle + 0.4)).toFloat(),
            (end.y - arrowLen * sin(angle + 0.4)).toFloat()
        )
        close()
    }
    drawPath(path, color)
}
