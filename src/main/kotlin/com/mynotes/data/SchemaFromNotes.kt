package com.mynotes.data

/**
 * Builds a suite mind map from the notes tree (NotebookLM-style, fully local).
 * Parent/child notes become nested cards + arrows. Layout is left-to-right by depth.
 */
object SchemaFromNotes {
    private const val COL_GAP = 260.0
    private const val ROW_GAP = 110.0

    fun build(suiteId: String, notes: List<Note>): SchemaGraph {
        if (notes.isEmpty()) return SchemaGraph()

        val byParent = notes.groupBy { it.parentId }
        val nodeByNoteId = mutableMapOf<String, SchemaNode>()
        val edges = mutableListOf<SchemaEdge>()
        var rowCursor = 0

        fun walk(parentNoteId: String?, parentNodeId: String?, depth: Int) {
            val children = (byParent[parentNoteId] ?: emptyList()).sortedBy { it.sortOrder }
            for (note in children) {
                val y = rowCursor * ROW_GAP
                rowCursor++
                val node = SchemaNode(
                    suiteId = suiteId,
                    parentId = parentNodeId,
                    linkedNoteId = note.id,
                    title = note.title.ifBlank { "Untitled" },
                    hint = hintFromNote(note),
                    posX = depth * COL_GAP + 40.0,
                    posY = y + 40.0,
                    sortOrder = note.sortOrder
                )
                nodeByNoteId[note.id] = node
                if (parentNodeId != null) {
                    edges += SchemaEdge(
                        suiteId = suiteId,
                        fromNodeId = parentNodeId,
                        toNodeId = node.id,
                        fromOffsetX = SchemaEdge.CARD_W_DEFAULT,
                        fromOffsetY = SchemaEdge.CARD_H_DEFAULT / 2.0,
                        toOffsetX = 0.0,
                        toOffsetY = SchemaEdge.CARD_H_DEFAULT / 2.0
                    )
                }
                walk(note.id, node.id, depth + 1)
            }
        }

        walk(null, null, 0)
        return SchemaGraph(nodes = nodeByNoteId.values.toList(), edges = edges)
    }

    private fun hintFromNote(note: Note): String {
        val plain = NoteContentCodec.plainText(NoteContentCodec.parse(note.content))
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3)
            .joinToString(" ")
        return plain.take(240)
    }
}
