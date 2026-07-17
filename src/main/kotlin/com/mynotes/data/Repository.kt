package com.mynotes.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class NotesRepository {

    fun getAllSuites(): List<Suite> = transaction {
        SuitesTable.selectAll()
            .orderBy(SuitesTable.sortOrder to SortOrder.ASC, SuitesTable.createdAt to SortOrder.ASC)
            .map { rowToSuite(it) }
    }

    fun createSuite(name: String, icon: String = "📁"): Suite {
        val suite = Suite(name = name.trim(), icon = icon)
        transaction {
            SuitesTable.insert {
                it[id] = suite.id
                it[SuitesTable.name] = suite.name
                it[SuitesTable.icon] = suite.icon
                it[createdAt] = suite.createdAt
                it[sortOrder] = (SuitesTable.selectAll().count().toInt())
            }
        }
        return suite
    }

    fun updateSuite(suite: Suite) = transaction {
        SuitesTable.update({ SuitesTable.id eq suite.id }) {
            it[name] = suite.name.trim()
            it[icon] = suite.icon
        }
    }

    fun deleteSuite(suiteId: String) {
        val noteIds = transaction {
            NotesTable.select { NotesTable.suiteId eq suiteId }.map { it[NotesTable.id] }
        }
        noteIds.forEach { deleteNote(it) }
        transaction {
            SuitesTable.deleteWhere { SuitesTable.id eq suiteId }
        }
    }

    fun getNotesBySuite(suiteId: String): List<Note> = transaction {
        val notes = NotesTable.select { NotesTable.suiteId eq suiteId }
            .orderBy(NotesTable.sortOrder to SortOrder.ASC, NotesTable.createdAt to SortOrder.ASC)
            .map { rowToNote(it) }
        if (notes.size > 1 && notes.all { it.sortOrder == 0 }) {
            notes.mapIndexed { index, note ->
                NotesTable.update({ NotesTable.id eq note.id }) {
                    it[sortOrder] = index
                }
                note.copy(sortOrder = index)
            }
        } else {
            notes
        }
    }

    fun getNoteWithImages(noteId: String): NoteWithImages? = transaction {
        val noteRow = NotesTable.select { NotesTable.id eq noteId }.singleOrNull() ?: return@transaction null
        val note = rowToNote(noteRow)
        val images = NoteImagesTable.select { NoteImagesTable.noteId eq noteId }
            .orderBy(NoteImagesTable.sortOrder to SortOrder.ASC, NoteImagesTable.createdAt to SortOrder.ASC)
            .map { rowToImage(it) }
        NoteWithImages(note, images)
    }

    fun createNote(suiteId: String, title: String = "Untitled", parentId: String? = null): Note {
        val sortOrder = transaction {
            val query = if (parentId == null) {
                (NotesTable.suiteId eq suiteId) and (NotesTable.parentId eq null)
            } else {
                (NotesTable.suiteId eq suiteId) and (NotesTable.parentId eq parentId)
            }
            NotesTable.select { query }.count().toInt()
        }
        val note = Note(
            suiteId = suiteId,
            parentId = parentId,
            title = title.trim().ifBlank { "Untitled" },
            sortOrder = sortOrder
        )
        transaction {
            NotesTable.insert {
                it[id] = note.id
                it[NotesTable.suiteId] = note.suiteId
                it[NotesTable.parentId] = note.parentId
                it[NotesTable.title] = note.title
                it[content] = note.content
                it[NotesTable.sortOrder] = note.sortOrder
                it[createdAt] = note.createdAt
                it[updatedAt] = note.updatedAt
            }
        }
        return note
    }

    fun updateNote(note: Note) {
        val updated = note.copy(
            title = note.title.trim().ifBlank { "Untitled" },
            updatedAt = LocalDateTime.now()
        )
        transaction {
            NotesTable.update({ NotesTable.id eq updated.id }) {
                it[title] = updated.title
                it[content] = updated.content
                it[parentId] = updated.parentId
                it[sortOrder] = updated.sortOrder
                it[updatedAt] = updated.updatedAt
            }
        }
    }

    fun moveNote(suiteId: String, draggedId: String, targetId: String, zone: NoteDropZone) {
        transaction {
            val notes = NotesTable.select { NotesTable.suiteId eq suiteId }.map { rowToNote(it) }
            val move = NoteTree.resolveMove(notes, draggedId, targetId, zone) ?: return@transaction
            val (newParentId, insertIndex) = move
            val dragged = notes.firstOrNull { it.id == draggedId } ?: return@transaction
            val siblings = notes
                .filter { it.parentId == newParentId && it.id != draggedId }
                .sortedBy { it.sortOrder }
                .toMutableList()
            siblings.add(insertIndex.coerceIn(0, siblings.size), dragged.copy(parentId = newParentId))
            siblings.forEachIndexed { index, note ->
                NotesTable.update({ NotesTable.id eq note.id }) {
                    it[parentId] = newParentId
                    it[sortOrder] = index
                    it[updatedAt] = LocalDateTime.now()
                }
            }
        }
    }

    fun moveSuite(suiteId: String, targetSuiteId: String, insertBefore: Boolean) {
        transaction {
            val suites = SuitesTable.selectAll()
                .orderBy(SuitesTable.sortOrder to SortOrder.ASC)
                .map { rowToSuite(it) }
                .toMutableList()
            val dragged = suites.firstOrNull { it.id == suiteId } ?: return@transaction
            if (dragged.id == targetSuiteId) return@transaction
            suites.removeAll { it.id == suiteId }
            val targetIndex = suites.indexOfFirst { it.id == targetSuiteId }
            if (targetIndex < 0) return@transaction
            val insertIndex = if (insertBefore) targetIndex else targetIndex + 1
            suites.add(insertIndex.coerceIn(0, suites.size), dragged)
            suites.forEachIndexed { index, suite ->
                SuitesTable.update({ SuitesTable.id eq suite.id }) {
                    it[sortOrder] = index
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        val childIds = transaction {
            NotesTable.select { NotesTable.parentId eq noteId }.map { it[NotesTable.id] }
        }
        childIds.forEach { deleteNote(it) }
        val images = transaction {
            NoteImagesTable.select { NoteImagesTable.noteId eq noteId }.map { rowToImage(it) }
        }
        images.forEach { deleteImageFile(it) }
        transaction {
            NoteImagesTable.deleteWhere { NoteImagesTable.noteId eq noteId }
            NotesTable.deleteWhere { NotesTable.id eq noteId }
        }
        AppPaths.noteImagesDir(noteId).deleteRecursively()
    }

    fun addImage(noteId: String, sourceFile: File): NoteImage? {
        if (!sourceFile.exists()) return null
        val image = NoteImage(
            noteId = noteId,
            fileName = "${UUID.randomUUID()}.${sourceFile.extension.ifBlank { "png" }}",
            sortOrder = transaction {
                NoteImagesTable.select { NoteImagesTable.noteId eq noteId }.count().toInt()
            }
        )
        val destDir = AppPaths.noteImagesDir(noteId)
        val destFile = destDir.resolve(image.fileName)
        try {
            val buffered = ImageIO.read(sourceFile)
            if (buffered != null) {
                ImageIO.write(buffered, destFile.extension.ifBlank { "png" }, destFile)
            } else {
                sourceFile.copyTo(destFile, overwrite = true)
            }
        } catch (_: Exception) {
            sourceFile.copyTo(destFile, overwrite = true)
        }
        transaction {
            NoteImagesTable.insert {
                it[id] = image.id
                it[NoteImagesTable.noteId] = image.noteId
                it[fileName] = image.fileName
                it[createdAt] = image.createdAt
                it[sortOrder] = image.sortOrder
            }
            NotesTable.update({ NotesTable.id eq noteId }) {
                it[updatedAt] = LocalDateTime.now()
            }
        }
        return image
    }

    fun addImageFromClipboard(noteId: String, image: BufferedImage): NoteImage? {
        val noteImage = NoteImage(
            noteId = noteId,
            fileName = "${UUID.randomUUID()}.png",
            sortOrder = transaction {
                NoteImagesTable.select { NoteImagesTable.noteId eq noteId }.count().toInt()
            }
        )
        val destFile = AppPaths.noteImagesDir(noteId).resolve(noteImage.fileName)
        return try {
            ImageIO.write(image, "png", destFile)
            transaction {
                NoteImagesTable.insert {
                    it[id] = noteImage.id
                    it[NoteImagesTable.noteId] = noteImage.noteId
                    it[fileName] = noteImage.fileName
                    it[createdAt] = noteImage.createdAt
                    it[sortOrder] = noteImage.sortOrder
                }
                NotesTable.update({ NotesTable.id eq noteId }) {
                    it[updatedAt] = LocalDateTime.now()
                }
            }
            noteImage
        } catch (_: Exception) {
            destFile.delete()
            null
        }
    }

    fun deleteImage(imageId: String) {
        val image = transaction {
            NoteImagesTable.select { NoteImagesTable.id eq imageId }.singleOrNull()?.let { rowToImage(it) }
        } ?: return
        deleteImageFile(image)
        transaction {
            NoteImagesTable.deleteWhere { NoteImagesTable.id eq imageId }
            NotesTable.update({ NotesTable.id eq image.noteId }) {
                it[updatedAt] = LocalDateTime.now()
            }
        }
    }

    fun getImageFile(image: NoteImage): File =
        AppPaths.noteImagesDir(image.noteId).resolve(image.fileName)

    fun searchNotes(query: String): List<Pair<Note, Suite>> {
        if (query.isBlank()) return emptyList()
        val pattern = "%${query.trim()}%"
        return transaction {
            (NotesTable innerJoin SuitesTable)
                .select {
                    (NotesTable.title like pattern) or (NotesTable.content like pattern)
                }
                .orderBy(NotesTable.updatedAt to SortOrder.DESC)
                .limit(50)
                .map { row ->
                    rowToNote(row) to rowToSuite(row)
                }
        }
    }

    fun ensureDefaultSuite(): Suite {
        val existing = getAllSuites()
        if (existing.isNotEmpty()) return existing.first()
        return createSuite("General", "📝")
    }

    private fun deleteImageFile(image: NoteImage) {
        getImageFile(image).delete()
    }

    private fun rowToSuite(row: ResultRow) = Suite(
        id = row[SuitesTable.id],
        name = row[SuitesTable.name],
        icon = row[SuitesTable.icon],
        createdAt = row[SuitesTable.createdAt],
        sortOrder = row[SuitesTable.sortOrder]
    )

    private fun rowToNote(row: ResultRow) = Note(
        id = row[NotesTable.id],
        suiteId = row[NotesTable.suiteId],
        parentId = row[NotesTable.parentId],
        title = row[NotesTable.title],
        content = row[NotesTable.content],
        sortOrder = row[NotesTable.sortOrder],
        createdAt = row[NotesTable.createdAt],
        updatedAt = row[NotesTable.updatedAt]
    )

    private fun rowToImage(row: ResultRow) = NoteImage(
        id = row[NoteImagesTable.id],
        noteId = row[NoteImagesTable.noteId],
        fileName = row[NoteImagesTable.fileName],
        createdAt = row[NoteImagesTable.createdAt],
        sortOrder = row[NoteImagesTable.sortOrder]
    )
}
