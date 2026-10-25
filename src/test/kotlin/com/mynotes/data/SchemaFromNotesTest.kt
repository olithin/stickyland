package com.mynotes.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaFromNotesTest {

    @Test
    fun `empty notes produce empty graph`() {
        val graph = SchemaFromNotes.build("s1", emptyList())
        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `build creates node per note and edge for parent child`() {
        val notes = listOf(
            Note(id = "root", suiteId = "s1", title = "Root", sortOrder = 0, content = "line1\nline2"),
            Note(id = "child", suiteId = "s1", parentId = "root", title = "Child", sortOrder = 0)
        )
        val graph = SchemaFromNotes.build("s1", notes)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)

        val root = graph.nodes.first { it.linkedNoteId == "root" }
        val child = graph.nodes.first { it.linkedNoteId == "child" }
        assertEquals("Root", root.title)
        assertTrue(root.hint.contains("line1"))
        assertEquals(root.id, child.parentId)
        assertEquals(root.id, graph.edges.single().fromNodeId)
        assertEquals(child.id, graph.edges.single().toNodeId)
        assertTrue(child.posX > root.posX)
    }

    @Test
    fun `siblings get increasing Y`() {
        val notes = listOf(
            Note(id = "a", suiteId = "s1", title = "A", sortOrder = 0),
            Note(id = "b", suiteId = "s1", title = "B", sortOrder = 1)
        )
        val graph = SchemaFromNotes.build("s1", notes)
        val ya = graph.nodes.first { it.linkedNoteId == "a" }.posY
        val yb = graph.nodes.first { it.linkedNoteId == "b" }.posY
        assertTrue(yb > ya)
    }
}
