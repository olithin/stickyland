package com.mynotes.data

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotesRepositoryTest : RepositoryTestBase() {

    @Test
    fun `ensureDefaultSuite creates General once`() {
        val first = repo.ensureDefaultSuite()
        val second = repo.ensureDefaultSuite()
        assertEquals("General", first.name)
        assertEquals(first.id, second.id)
        assertEquals(1, repo.getAllSuites().size)
    }

    @Test
    fun `create rename and delete suite`() {
        val a = repo.createSuite("AWS")
        val b = repo.createSuite("DB")
        assertEquals(listOf("AWS", "DB"), repo.getAllSuites().map { it.name })

        repo.updateSuite(a.copy(name = "Amazon"))
        assertEquals("Amazon", repo.getAllSuites().first { it.id == a.id }.name)

        repo.createNote(a.id, title = "n1")
        repo.deleteSuite(a.id)
        assertEquals(listOf(b.id), repo.getAllSuites().map { it.id })
    }

    @Test
    fun `moveSuite reorders before and after`() {
        val a = repo.createSuite("A")
        val b = repo.createSuite("B")
        val c = repo.createSuite("C")
        repo.moveSuite(c.id, a.id, insertBefore = true)
        assertEquals(listOf("C", "A", "B"), repo.getAllSuites().map { it.name })

        repo.moveSuite(c.id, b.id, insertBefore = false)
        assertEquals(listOf("A", "B", "C"), repo.getAllSuites().map { it.name })
    }

    @Test
    fun `create update and nest notes`() {
        val suite = repo.createSuite("S")
        val root = repo.createNote(suite.id, title = "Root")
        val child = repo.createNote(suite.id, title = "Child", parentId = root.id)
        assertEquals(0, root.sortOrder)
        assertEquals(0, child.sortOrder)
        assertEquals(root.id, child.parentId)

        repo.updateNote(root.copy(title = "  Rooted  ", content = "body"))
        val loaded = repo.getNoteWithImages(root.id)
        assertNotNull(loaded)
        assertEquals("Rooted", loaded.note.title)
        assertEquals("body", loaded.note.content)
    }

    @Test
    fun `moveNote reorders and nests`() {
        val suite = repo.createSuite("S")
        val a = repo.createNote(suite.id, title = "A")
        val b = repo.createNote(suite.id, title = "B")
        val c = repo.createNote(suite.id, title = "C")

        repo.moveNote(suite.id, c.id, a.id, NoteDropZone.BEFORE)
        assertEquals(listOf("C", "A", "B"), repo.getNotesBySuite(suite.id).sortedBy { it.sortOrder }.map { it.title })

        repo.moveNote(suite.id, b.id, a.id, NoteDropZone.INSIDE)
        val notes = repo.getNotesBySuite(suite.id)
        val moved = notes.first { it.id == b.id }
        assertEquals(a.id, moved.parentId)
    }

    @Test
    fun `moveNoteToSuite moves subtree and preserves nesting`() {
        val source = repo.createSuite("Source")
        val target = repo.createSuite("Target")
        val parent = repo.createNote(source.id, title = "Parent")
        val child = repo.createNote(source.id, title = "Child", parentId = parent.id)
        repo.createNote(source.id, title = "Stay")

        repo.moveNoteToSuite(parent.id, target.id)

        val sourceNotes = repo.getNotesBySuite(source.id)
        val targetNotes = repo.getNotesBySuite(target.id)
        assertEquals(listOf("Stay"), sourceNotes.map { it.title })
        assertEquals(2, targetNotes.size)
        val movedParent = targetNotes.first { it.id == parent.id }
        val movedChild = targetNotes.first { it.id == child.id }
        assertNull(movedParent.parentId)
        assertEquals(parent.id, movedChild.parentId)
        assertEquals(target.id, movedParent.suiteId)
        assertEquals(target.id, movedChild.suiteId)
    }

    @Test
    fun `deleteNote removes children recursively`() {
        val suite = repo.createSuite("S")
        val parent = repo.createNote(suite.id, title = "P")
        repo.createNote(suite.id, title = "C", parentId = parent.id)
        repo.deleteNote(parent.id)
        assertTrue(repo.getNotesBySuite(suite.id).isEmpty())
    }

    @Test
    fun `searchNotes finds by title and content`() {
        val suite = repo.createSuite("Work")
        val n1 = repo.createNote(suite.id, title = "AWS SSO")
        repo.updateNote(n1.copy(content = "login instructions"))
        repo.createNote(suite.id, title = "Other")

        val byTitle = repo.searchNotes("SSO")
        assertEquals(1, byTitle.size)
        assertEquals(n1.id, byTitle[0].first.id)
        assertEquals("Work", byTitle[0].second.name)

        val byContent = repo.searchNotes("instructions")
        assertEquals(1, byContent.size)
        assertTrue(repo.searchNotes("   ").isEmpty())
    }

    @Test
    fun `schema replace create edge and delete node`() {
        val suite = repo.createSuite("S")
        val a = repo.createSchemaNode(suite.id, title = "A", posX = 10.0, posY = 20.0)
        // parentId auto-creates an edge; create B as root then link manually.
        val b = repo.createSchemaNode(suite.id, title = "B")
        var graph = repo.getSchema(suite.id)
        assertEquals(2, graph.nodes.size)
        assertTrue(graph.edges.isEmpty())

        val edge = repo.createSchemaEdge(suite.id, a.id, b.id)
        assertNotNull(edge)
        assertNull(repo.createSchemaEdge(suite.id, a.id, b.id)) // duplicate

        repo.updateSchemaNode(a.copy(title = "Alpha", posX = 99.0))
        assertEquals("Alpha", repo.getSchema(suite.id).nodes.first { it.id == a.id }.title)

        repo.deleteSchemaNode(a.id)
        graph = repo.getSchema(suite.id)
        assertEquals(1, graph.nodes.size)
        assertEquals(b.id, graph.nodes.single().id)
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `replaceSchema overwrites previous graph`() {
        val suite = repo.createSuite("S")
        repo.createSchemaNode(suite.id, title = "Old")
        val built = SchemaFromNotes.build(
            suite.id,
            listOf(Note(id = "n1", suiteId = suite.id, title = "FromNote"))
        )
        val saved = repo.replaceSchema(suite.id, built)
        assertEquals(1, saved.nodes.size)
        assertEquals("FromNote", saved.nodes.single().title)
    }

    @Test
    fun `addImageFromClipboard and deleteImage`() {
        val suite = repo.createSuite("S")
        val note = repo.createNote(suite.id, title = "Pic")
        val bitmap = BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB)
        val image = repo.addImageFromClipboard(note.id, bitmap)
        assertNotNull(image)
        assertTrue(repo.getImageFile(image).exists())

        val withImages = repo.getNoteWithImages(note.id)
        assertEquals(1, withImages?.images?.size)

        repo.deleteImage(image.id)
        assertTrue(repo.getNoteWithImages(note.id)!!.images.isEmpty())
        assertTrue(!repo.getImageFile(image).exists())
    }

    @Test
    fun `addImage copies file from disk`() {
        val suite = repo.createSuite("S")
        val note = repo.createNote(suite.id, title = "File")
        val src = File(AppPaths.dataDir, "sample.png")
        ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", src)

        val image = repo.addImage(note.id, src)
        assertNotNull(image)
        assertTrue(repo.getImageFile(image).exists())
        assertNull(repo.addImage(note.id, File("/no/such/file.png")))
    }

    @Test
    fun `getNotesBySuite backfills sortOrder when all zero`() {
        val suite = repo.createSuite("S")
        val n1 = repo.createNote(suite.id, title = "One")
        val n2 = repo.createNote(suite.id, title = "Two")
        // Force both to 0 to simulate legacy rows.
        repo.updateNote(n1.copy(sortOrder = 0))
        repo.updateNote(n2.copy(sortOrder = 0))
        val notes = repo.getNotesBySuite(suite.id)
        assertEquals(setOf(0, 1), notes.map { it.sortOrder }.toSet())
    }
}
