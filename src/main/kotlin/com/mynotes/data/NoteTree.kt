package com.mynotes.data

data class NoteTreeItem(
    val note: Note,
    val depth: Int,
    val hasChildren: Boolean,
    val isExpanded: Boolean
)

enum class NoteDropZone {
    BEFORE,
    INSIDE,
    AFTER
}

object NoteTree {
    fun flatten(notes: List<Note>, expandedIds: Set<String>): List<NoteTreeItem> {
        val byParent = notes.groupBy { it.parentId }
        fun walk(parentId: String?, depth: Int): List<NoteTreeItem> = buildList {
            val children = (byParent[parentId] ?: emptyList()).sortedBy { it.sortOrder }
            for (child in children) {
                val childNotes = byParent[child.id].orEmpty()
                val hasChildren = childNotes.isNotEmpty()
                val isExpanded = child.id in expandedIds
                add(NoteTreeItem(child, depth, hasChildren, isExpanded))
                if (hasChildren && isExpanded) {
                    addAll(walk(child.id, depth + 1))
                }
            }
        }
        return walk(null, 0)
    }

    fun isDescendant(notes: List<Note>, ancestorId: String, nodeId: String): Boolean {
        val byId = notes.associateBy { it.id }
        var current = byId[nodeId]?.parentId?.let { byId[it] }
        while (current != null) {
            if (current.id == ancestorId) return true
            current = current.parentId?.let { byId[it] }
        }
        return false
    }

    fun canMove(notes: List<Note>, draggedId: String, targetId: String, zone: NoteDropZone): Boolean {
        if (draggedId == targetId) return false
        val target = notes.find { it.id == targetId } ?: return false
        val newParentId = when (zone) {
            NoteDropZone.INSIDE -> targetId
            NoteDropZone.BEFORE, NoteDropZone.AFTER -> target.parentId
        }
        if (newParentId == null) return true
        if (newParentId == draggedId) return false
        return !isDescendant(notes, draggedId, newParentId)
    }

    fun resolveMove(
        notes: List<Note>,
        draggedId: String,
        targetId: String,
        zone: NoteDropZone
    ): Pair<String?, Int>? {
        if (!canMove(notes, draggedId, targetId, zone)) return null
        val target = notes.find { it.id == targetId } ?: return null
        val newParentId = when (zone) {
            NoteDropZone.INSIDE -> targetId
            NoteDropZone.BEFORE, NoteDropZone.AFTER -> target.parentId
        }
        val siblings = notes
            .filter { it.parentId == newParentId && it.id != draggedId }
            .sortedBy { it.sortOrder }
        val targetIndex = siblings.indexOfFirst { it.id == targetId }
        val insertIndex = when (zone) {
            NoteDropZone.INSIDE -> notes.count { it.parentId == targetId && it.id != draggedId }
            NoteDropZone.BEFORE -> if (targetIndex >= 0) targetIndex else siblings.size
            NoteDropZone.AFTER -> if (targetIndex >= 0) targetIndex + 1 else siblings.size
        }
        return newParentId to insertIndex.coerceIn(0, siblings.size)
    }
}
