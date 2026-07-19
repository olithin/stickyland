package com.mynotes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mynotes.data.*
import com.mynotes.util.ClipboardImage
import kotlinx.coroutines.*
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppUiState(
    val suites: List<Suite> = emptyList(),
    val selectedSuiteId: String? = null,
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val currentNote: NoteWithImages? = null,
    val searchQuery: String = "",
    val searchResults: List<Pair<Note, Suite>> = emptyList(),
    val isSearching: Boolean = false,
    val lightboxImage: NoteImage? = null,
    val expandedNoteIds: Set<String> = emptySet(),
    val statusMessage: String? = null,
    val mainPanelMode: MainPanelMode = MainPanelMode.NOTES,
    val schemaGraph: SchemaGraph = SchemaGraph(),
    val selectedSchemaNodeId: String? = null,
    val selectedSchemaEdgeId: String? = null,
    val linkFromNodeId: String? = null,
    val schemaPanX: Float = 0f,
    val schemaPanY: Float = 0f,
    val schemaZoom: Float = 1f
)

class AppViewModel(
    private val repository: NotesRepository = NotesRepository()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var state by mutableStateOf(AppUiState())
        private set

    private var saveJob: Job? = null

    val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm", Locale.ENGLISH)

    fun loadInitialData() {
        val defaultSuite = repository.ensureDefaultSuite()
        val suites = repository.getAllSuites()
        val notes = repository.getNotesBySuite(defaultSuite.id)
        val expandableIds = notes
            .filter { parent -> notes.any { it.parentId == parent.id } }
            .map { it.id }
            .toSet()
        state = AppUiState(
            suites = suites,
            selectedSuiteId = defaultSuite.id,
            notes = notes,
            expandedNoteIds = expandableIds,
            schemaGraph = repository.getSchema(defaultSuite.id)
        )
    }

    private fun updateState(reducer: (AppUiState) -> AppUiState) {
        state = reducer(state)
    }

    fun selectSuite(suiteId: String) {
        scope.launch {
            val notes = repository.getNotesBySuite(suiteId)
            val expandableIds = notes
                .filter { parent -> notes.any { it.parentId == parent.id } }
                .map { it.id }
                .toSet()
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    selectedSuiteId = suiteId,
                    notes = notes,
                    selectedNoteId = null,
                    currentNote = null,
                    expandedNoteIds = expandableIds,
                    isSearching = false,
                    searchQuery = "",
                    schemaGraph = schema,
                    selectedSchemaNodeId = null,
                    linkFromNodeId = null
                )
            }
        }
    }

    fun setMainPanelMode(mode: MainPanelMode) {
        val suiteId = state.selectedSuiteId
        updateState {
            it.copy(
                mainPanelMode = mode,
                linkFromNodeId = null,
                selectedSchemaNodeId = if (mode == MainPanelMode.SCHEMA) it.selectedSchemaNodeId else null
            )
        }
        if (mode == MainPanelMode.SCHEMA && suiteId != null) {
            scope.launch {
                val schema = repository.getSchema(suiteId)
                updateState { it.copy(schemaGraph = schema) }
            }
        }
    }

    fun setSchemaPanZoom(panX: Float, panY: Float, zoom: Float) {
        updateState {
            it.copy(
                schemaPanX = panX,
                schemaPanY = panY,
                schemaZoom = zoom.coerceIn(0.35f, 2.5f)
            )
        }
    }

    fun createSchemaCard(parentId: String? = null) {
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            val parent = parentId?.let { id -> state.schemaGraph.nodes.find { it.id == id } }
            val node = repository.createSchemaNode(
                suiteId = suiteId,
                title = "New card",
                parentId = parentId,
                posX = (parent?.posX ?: 80.0) + if (parentId != null) 240.0 else 40.0,
                posY = (parent?.posY ?: 80.0) + if (parentId != null) 40.0 else
                    state.schemaGraph.nodes.size * 30.0,
                hint = ""
            )
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    schemaGraph = schema,
                    selectedSchemaNodeId = node.id,
                    statusMessage = if (parentId == null) "Card created" else "Child card created"
                )
            }
        }
    }

    fun selectSchemaNode(nodeId: String?) {
        updateState {
            it.copy(
                selectedSchemaNodeId = nodeId,
                selectedSchemaEdgeId = null
            )
        }
    }

    fun selectSchemaEdge(edgeId: String?) {
        updateState {
            it.copy(
                selectedSchemaEdgeId = edgeId,
                selectedSchemaNodeId = if (edgeId != null) null else it.selectedSchemaNodeId
            )
        }
    }

    fun connectSchemaNodes(
        fromNodeId: String,
        toNodeId: String,
        fromOffsetX: Double,
        fromOffsetY: Double,
        toOffsetX: Double,
        toOffsetY: Double
    ) {
        val suiteId = state.selectedSuiteId ?: return
        if (fromNodeId == toNodeId) return
        scope.launch {
            val edge = repository.createSchemaEdge(
                suiteId = suiteId,
                fromNodeId = fromNodeId,
                toNodeId = toNodeId,
                fromOffsetX = fromOffsetX,
                fromOffsetY = fromOffsetY,
                toOffsetX = toOffsetX,
                toOffsetY = toOffsetY
            )
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    schemaGraph = schema,
                    linkFromNodeId = null,
                    selectedSchemaNodeId = toNodeId,
                    selectedSchemaEdgeId = edge?.id,
                    statusMessage = if (edge != null) "Arrow created" else "Link already exists"
                )
            }
        }
    }

    fun deleteSelectedSchema() {
        val edgeId = state.selectedSchemaEdgeId
        if (edgeId != null) {
            deleteSchemaEdge(edgeId)
            return
        }
        deleteSelectedSchemaNode()
    }

    fun updateSchemaNodeTitle(nodeId: String, title: String) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        val updated = node.copy(title = title)
        patchSchemaNode(updated)
    }

    fun updateSchemaNodeHint(nodeId: String, hint: String) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        val updated = node.copy(hint = hint)
        patchSchemaNode(updated, "Hint saved")
    }

    fun moveSchemaNode(nodeId: String, posX: Double, posY: Double) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        val updated = node.copy(posX = posX, posY = posY)
        updateState {
            it.copy(schemaGraph = it.schemaGraph.copy(
                nodes = it.schemaGraph.nodes.map { n -> if (n.id == nodeId) updated else n }
            ))
        }
        scope.launch { repository.updateSchemaNode(updated) }
    }

    fun resizeSchemaNode(nodeId: String, width: Double, height: Double) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        val newW = width.coerceIn(SchemaEdge.CARD_MIN_W, SchemaEdge.CARD_MAX_W)
        val newH = height.coerceIn(SchemaEdge.CARD_MIN_H, SchemaEdge.CARD_MAX_H)
        val oldW = node.w()
        val oldH = node.h()
        val updated = node.copy(width = newW, height = newH)
        val remappedEdges = state.schemaGraph.edges.map { edge ->
            var next = edge
            if (edge.fromNodeId == nodeId) {
                val port = SchemaPort.nearest(
                    edge.fromOffsetX.toFloat(),
                    edge.fromOffsetY.toFloat(),
                    oldW,
                    oldH
                )
                val o = port.offset(newW.toFloat(), newH.toFloat())
                next = next.copy(fromOffsetX = o.x.toDouble(), fromOffsetY = o.y.toDouble())
            }
            if (edge.toNodeId == nodeId) {
                val port = SchemaPort.nearest(
                    edge.toOffsetX.toFloat(),
                    edge.toOffsetY.toFloat(),
                    oldW,
                    oldH
                )
                val o = port.offset(newW.toFloat(), newH.toFloat())
                next = next.copy(toOffsetX = o.x.toDouble(), toOffsetY = o.y.toDouble())
            }
            next
        }
        updateState {
            it.copy(
                schemaGraph = it.schemaGraph.copy(
                    nodes = it.schemaGraph.nodes.map { n -> if (n.id == nodeId) updated else n },
                    edges = remappedEdges
                )
            )
        }
        scope.launch {
            repository.updateSchemaNode(updated)
            remappedEdges
                .filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
                .forEach { repository.updateSchemaEdge(it) }
        }
    }

    fun toggleSchemaNodeCollapsed(nodeId: String) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        patchSchemaNode(node.copy(collapsed = !node.collapsed))
    }

    fun deleteSelectedSchemaNode() {
        val nodeId = state.selectedSchemaNodeId ?: return
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            repository.deleteSchemaNode(nodeId)
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    schemaGraph = schema,
                    selectedSchemaNodeId = null,
                    selectedSchemaEdgeId = null,
                    linkFromNodeId = null,
                    statusMessage = "Card deleted"
                )
            }
        }
    }

    fun beginOrCompleteLink(nodeId: String) {
        val suiteId = state.selectedSuiteId ?: return
        val from = state.linkFromNodeId
        if (from == null) {
            updateState {
                it.copy(
                    linkFromNodeId = nodeId,
                    selectedSchemaNodeId = nodeId,
                    statusMessage = "Link mode: click another card"
                )
            }
            return
        }
        if (from == nodeId) {
            updateState { it.copy(linkFromNodeId = null, statusMessage = "Link cancelled") }
            return
        }
        scope.launch {
            val edge = repository.createSchemaEdge(
                suiteId = suiteId,
                fromNodeId = from,
                toNodeId = nodeId,
                fromOffsetX = SchemaEdge.CARD_W_DEFAULT,
                fromOffsetY = SchemaEdge.CARD_H_DEFAULT / 2.0,
                toOffsetX = 0.0,
                toOffsetY = SchemaEdge.CARD_H_DEFAULT / 2.0
            )
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    schemaGraph = schema,
                    linkFromNodeId = null,
                    selectedSchemaNodeId = nodeId,
                    statusMessage = if (edge != null) "Arrow created" else "Link already exists"
                )
            }
        }
    }

    fun cancelLinkMode() {
        updateState { it.copy(linkFromNodeId = null) }
    }

    fun deleteSchemaEdge(edgeId: String) {
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            repository.deleteSchemaEdge(edgeId)
            val schema = repository.getSchema(suiteId)
            updateState {
                it.copy(
                    schemaGraph = schema,
                    selectedSchemaEdgeId = null,
                    statusMessage = "Arrow removed"
                )
            }
        }
    }

    fun autoBuildSchemaFromNotes() {
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            val notes = repository.getNotesBySuite(suiteId)
            if (notes.isEmpty()) {
                updateState { it.copy(statusMessage = "No notes to build from") }
                return@launch
            }
            val graph = SchemaFromNotes.build(suiteId, notes)
            val saved = repository.replaceSchema(suiteId, graph)
            updateState {
                it.copy(
                    schemaGraph = saved,
                    selectedSchemaNodeId = null,
                    linkFromNodeId = null,
                    schemaPanX = 0f,
                    schemaPanY = 0f,
                    schemaZoom = 1f,
                    statusMessage = "Schema built from ${notes.size} notes · olithin"
                )
            }
        }
    }

    private fun patchSchemaNode(updated: SchemaNode, message: String? = null) {
        updateState {
            it.copy(
                schemaGraph = it.schemaGraph.copy(
                    nodes = it.schemaGraph.nodes.map { n -> if (n.id == updated.id) updated else n }
                ),
                statusMessage = message ?: it.statusMessage
            )
        }
        scope.launch { repository.updateSchemaNode(updated) }
    }

    fun openLinkedNote(nodeId: String) {
        val node = state.schemaGraph.nodes.find { it.id == nodeId } ?: return
        val noteId = node.linkedNoteId ?: return
        setMainPanelMode(MainPanelMode.NOTES)
        selectNote(noteId)
    }

    fun selectNote(noteId: String) {
        scope.launch {
            val noteWithImages = repository.getNoteWithImages(noteId)
            updateState {
                it.copy(
                    selectedNoteId = noteId,
                    currentNote = noteWithImages,
                    isSearching = false
                )
            }
        }
    }

    fun createNote(parentId: String? = null) {
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            val note = repository.createNote(suiteId, parentId = parentId)
            val notes = repository.getNotesBySuite(suiteId)
            val expanded = if (parentId != null) {
                state.expandedNoteIds + parentId
            } else {
                state.expandedNoteIds
            }
            val noteWithImages = repository.getNoteWithImages(note.id)
            updateState {
                it.copy(
                    notes = notes,
                    selectedNoteId = note.id,
                    currentNote = noteWithImages,
                    expandedNoteIds = expanded,
                    statusMessage = if (parentId == null) "Note created" else "Sub-note created"
                )
            }
        }
    }

    fun toggleNoteExpanded(noteId: String) {
        updateState { current ->
            val expanded = current.expandedNoteIds
            current.copy(
                expandedNoteIds = if (noteId in expanded) expanded - noteId else expanded + noteId
            )
        }
    }

    fun moveNote(draggedId: String, targetId: String, zone: NoteDropZone) {
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            repository.moveNote(suiteId, draggedId, targetId, zone)
            val notes = repository.getNotesBySuite(suiteId)
            val expanded = if (zone == NoteDropZone.INSIDE) {
                state.expandedNoteIds + targetId
            } else {
                state.expandedNoteIds
            }
            updateState {
                it.copy(notes = notes, expandedNoteIds = expanded, statusMessage = "Order updated")
            }
        }
    }

    fun moveSuite(draggedId: String, targetId: String, insertBefore: Boolean) {
        scope.launch {
            repository.moveSuite(draggedId, targetId, insertBefore)
            val suites = repository.getAllSuites()
            updateState { it.copy(suites = suites, statusMessage = "Suite moved") }
        }
    }

    fun updateTitle(title: String) {
        val current = state.currentNote ?: return
        val updated = current.note.copy(title = title)
        updateState {
            it.copy(
                currentNote = current.copy(note = updated),
                notes = it.notes.map { n -> if (n.id == updated.id) updated else n }
            )
        }
        scheduleSave(updated)
    }

    fun updateContent(content: String) {
        val current = state.currentNote ?: return
        val updated = current.note.copy(content = content)
        updateState {
            it.copy(currentNote = current.copy(note = updated))
        }
        scheduleSave(updated)
    }

    fun updateBlockText(blockIndex: Int, text: String) {
        val current = state.currentNote ?: return
        val blocks = NoteContentCodec.parse(current.note.content, current.images).toMutableList()
        if (blockIndex !in blocks.indices || blocks[blockIndex].type != "text") return
        blocks[blockIndex] = blocks[blockIndex].copy(content = text)
        updateContent(NoteContentCodec.encode(blocks))
    }

    private fun scheduleSave(note: Note) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            repository.updateNote(note)
            val suiteId = state.selectedSuiteId
            if (suiteId != null) {
                val notes = repository.getNotesBySuite(suiteId)
                updateState { it.copy(notes = notes) }
            }
        }
    }

    fun deleteCurrentNote() {
        val noteId = state.selectedNoteId ?: return
        val suiteId = state.selectedSuiteId ?: return
        scope.launch {
            repository.deleteNote(noteId)
            val notes = repository.getNotesBySuite(suiteId)
            updateState {
                it.copy(
                    notes = notes,
                    selectedNoteId = null,
                    currentNote = null,
                    statusMessage = "Note deleted"
                )
            }
        }
    }

    fun createSuite(name: String, icon: String = "📁") {
        if (name.isBlank()) return
        scope.launch {
            repository.createSuite(name, icon)
            val suites = repository.getAllSuites()
            updateState { it.copy(suites = suites, statusMessage = "Suite \"$name\" created") }
        }
    }

    fun deleteSuite(suiteId: String) {
        scope.launch {
            repository.deleteSuite(suiteId)
            val suites = repository.getAllSuites()
            val defaultSuite = suites.firstOrNull() ?: repository.ensureDefaultSuite()
            val notes = repository.getNotesBySuite(defaultSuite.id)
            updateState {
                it.copy(
                    suites = if (suites.isEmpty()) listOf(defaultSuite) else suites,
                    selectedSuiteId = defaultSuite.id,
                    notes = notes,
                    selectedNoteId = null,
                    currentNote = null,
                    statusMessage = "Suite deleted"
                )
            }
        }
    }

    fun renameSuite(suiteId: String, newName: String) {
        if (newName.isBlank()) return
        scope.launch {
            val suite = state.suites.find { it.id == suiteId } ?: return@launch
            val updated = suite.copy(name = newName.trim())
            repository.updateSuite(updated)
            val suites = repository.getAllSuites()
            updateState { it.copy(suites = suites) }
        }
    }

    fun addImage(sourceFile: File, textBlockIndex: Int = 0, cursor: Int = Int.MAX_VALUE) {
        val noteId = state.selectedNoteId ?: return
        scope.launch {
            val savedImage = repository.addImage(noteId, sourceFile) ?: return@launch
            insertImageIntoContent(textBlockIndex, cursor, savedImage.id, "Screenshot added")
        }
    }

    fun pasteImageFromClipboard(textBlockIndex: Int, cursor: Int): Boolean {
        val noteId = state.selectedNoteId ?: return false
        val image = ClipboardImage.readImage() ?: return false
        scope.launch {
            val savedImage = repository.addImageFromClipboard(noteId, image) ?: return@launch
            insertImageIntoContent(textBlockIndex, cursor, savedImage.id, "Screenshot pasted")
        }
        return true
    }

    private fun insertImageIntoContent(
        textBlockIndex: Int,
        cursor: Int,
        imageId: String,
        message: String
    ) {
        val current = state.currentNote ?: return
        val blocks = NoteContentCodec.insertImageAt(
            NoteContentCodec.parse(current.note.content, current.images),
            textBlockIndex,
            cursor,
            imageId
        )
        val updated = current.note.copy(content = NoteContentCodec.encode(blocks))
        updateState { it.copy(currentNote = current.copy(note = updated)) }
        saveJob?.cancel()
        scope.launch {
            repository.updateNote(updated)
            refreshCurrentNote(updated.id, message)
        }
    }

    private suspend fun refreshCurrentNote(noteId: String, message: String) {
        val noteWithImages = repository.getNoteWithImages(noteId)
        updateState {
            it.copy(
                currentNote = noteWithImages,
                statusMessage = message
            )
        }
    }

    fun deleteImage(imageId: String) {
        scope.launch {
            val noteId = state.selectedNoteId ?: return@launch
            val current = state.currentNote ?: return@launch
            val blocks = NoteContentCodec.removeImage(
                NoteContentCodec.parse(current.note.content, current.images),
                imageId
            )
            repository.updateNote(current.note.copy(content = NoteContentCodec.encode(blocks)))
            repository.deleteImage(imageId)
            val noteWithImages = repository.getNoteWithImages(noteId)
            updateState {
                it.copy(
                    currentNote = noteWithImages,
                    lightboxImage = if (it.lightboxImage?.id == imageId) null else it.lightboxImage,
                    statusMessage = "Screenshot deleted"
                )
            }
        }
    }

    fun showLightbox(image: NoteImage) {
        updateState { it.copy(lightboxImage = image) }
    }

    fun hideLightbox() {
        updateState { it.copy(lightboxImage = null) }
    }

    fun getImageFile(image: NoteImage): File = repository.getImageFile(image)

    fun setSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        if (query.isBlank()) {
            updateState { it.copy(searchResults = emptyList()) }
            return
        }
        scope.launch {
            delay(250)
            if (state.searchQuery != query) return@launch
            val results = repository.searchNotes(query)
            updateState { it.copy(searchResults = results) }
        }
    }

    fun clearStatus() {
        updateState { it.copy(statusMessage = null) }
    }

    fun setStatus(message: String) {
        updateState { it.copy(statusMessage = message) }
    }

    fun dispose() {
        scope.cancel()
    }
}
