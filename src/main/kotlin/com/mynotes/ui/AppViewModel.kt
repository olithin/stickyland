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
    val statusMessage: String? = null
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
            expandedNoteIds = expandableIds
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
            updateState {
                it.copy(
                    selectedSuiteId = suiteId,
                    notes = notes,
                    selectedNoteId = null,
                    currentNote = null,
                    expandedNoteIds = expandableIds,
                    isSearching = false,
                    searchQuery = ""
                )
            }
        }
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

    fun dispose() {
        scope.cancel()
    }
}
