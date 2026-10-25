package com.mynotes.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteTreeTest {

    private fun note(
        id: String,
        parentId: String? = null,
        sortOrder: Int = 0,
        title: String = id
    ) = Note(
        id = id,
        suiteId = "suite",
        parentId = parentId,
        title = title,
        sortOrder = sortOrder
    )

    @Test
    fun `flatten shows roots sorted and hides collapsed children`() {
        val notes = listOf(
            note("a", sortOrder = 1),
            note("b", sortOrder = 0),
            note("a1", parentId = "a", sortOrder = 0),
            note("a2", parentId = "a", sortOrder = 1)
        )

        val collapsed = NoteTree.flatten(notes, expandedIds = emptySet())
        assertEquals(listOf("b", "a"), collapsed.map { it.note.id })
        assertTrue(collapsed.first { it.note.id == "a" }.hasChildren)
        assertFalse(collapsed.first { it.note.id == "a" }.isExpanded)

        val expanded = NoteTree.flatten(notes, expandedIds = setOf("a"))
        assertEquals(listOf("b", "a", "a1", "a2"), expanded.map { it.note.id })
        assertEquals(0, expanded[0].depth)
        assertEquals(1, expanded.first { it.note.id == "a1" }.depth)
    }

    @Test
    fun `isDescendant walks parent chain`() {
        val notes = listOf(
            note("root"),
            note("child", parentId = "root"),
            note("grand", parentId = "child"),
            note("other")
        )
        assertTrue(NoteTree.isDescendant(notes, "root", "grand"))
        assertTrue(NoteTree.isDescendant(notes, "child", "grand"))
        assertFalse(NoteTree.isDescendant(notes, "grand", "root"))
        assertFalse(NoteTree.isDescendant(notes, "root", "other"))
        assertFalse(NoteTree.isDescendant(notes, "root", "root"))
    }

    @Test
    fun `canMove rejects self and nesting into own descendant`() {
        val notes = listOf(
            note("parent"),
            note("child", parentId = "parent"),
            note("sib")
        )
        assertFalse(NoteTree.canMove(notes, "parent", "parent", NoteDropZone.AFTER))
        assertFalse(NoteTree.canMove(notes, "parent", "child", NoteDropZone.INSIDE))
        assertTrue(NoteTree.canMove(notes, "sib", "parent", NoteDropZone.INSIDE))
        assertTrue(NoteTree.canMove(notes, "child", "sib", NoteDropZone.BEFORE))
    }

    @Test
    fun `resolveMove BEFORE and AFTER places among siblings`() {
        val notes = listOf(
            note("a", sortOrder = 0),
            note("b", sortOrder = 1),
            note("c", sortOrder = 2)
        )
        assertEquals(null to 0, NoteTree.resolveMove(notes, "c", "a", NoteDropZone.BEFORE))
        // Among remaining siblings [b, c], insert after b → index 1 → order [b, a, c].
        assertEquals(null to 1, NoteTree.resolveMove(notes, "a", "b", NoteDropZone.AFTER))
    }

    @Test
    fun `resolveMove INSIDE nests under target at end`() {
        val notes = listOf(
            note("parent"),
            note("existing", parentId = "parent", sortOrder = 0),
            note("orphan", sortOrder = 1)
        )
        val move = NoteTree.resolveMove(notes, "orphan", "parent", NoteDropZone.INSIDE)
        assertEquals("parent" to 1, move)
    }

    @Test
    fun `resolveMove returns null for illegal cycle`() {
        val notes = listOf(
            note("parent"),
            note("child", parentId = "parent")
        )
        assertNull(NoteTree.resolveMove(notes, "parent", "child", NoteDropZone.INSIDE))
    }
}
