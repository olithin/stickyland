package com.mynotes.data

import java.time.LocalDateTime
import java.util.UUID

data class Suite(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "📁",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val suiteId: String,
    val parentId: String? = null,
    val title: String,
    val content: String = "",
    val sortOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class NoteImage(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val fileName: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0
)

data class NoteWithImages(
    val note: Note,
    val images: List<NoteImage> = emptyList()
)

data class SchemaNode(
    val id: String = UUID.randomUUID().toString(),
    val suiteId: String,
    val parentId: String? = null,
    val linkedNoteId: String? = null,
    val title: String,
    val hint: String = "",
    val posX: Double = 0.0,
    val posY: Double = 0.0,
    val width: Double = 200.0,
    val height: Double = 80.0,
    val collapsed: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun w(): Float = width.toFloat().coerceAtLeast(120f)
    fun h(): Float = height.toFloat().coerceAtLeast(56f)
}

data class SchemaEdge(
    val id: String = UUID.randomUUID().toString(),
    val suiteId: String,
    val fromNodeId: String,
    val toNodeId: String,
    /** Start attach on source card (relative to top-left), Miro-style port. */
    val fromOffsetX: Double = CARD_W_DEFAULT,
    val fromOffsetY: Double = CARD_H_DEFAULT / 2.0,
    /** End attach on target card (relative to top-left), Miro-style port. */
    val toOffsetX: Double = 0.0,
    val toOffsetY: Double = CARD_H_DEFAULT / 2.0,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val CARD_W_DEFAULT = 200.0
        const val CARD_H_DEFAULT = 80.0
        const val CARD_MIN_W = 120.0
        const val CARD_MIN_H = 56.0
        const val CARD_MAX_W = 520.0
        const val CARD_MAX_H = 360.0
    }
}

/** Miro-like connection ports on card mid-sides. */
enum class SchemaPort {
    NORTH, EAST, SOUTH, WEST;

    fun offset(width: Float = SchemaEdge.CARD_W_DEFAULT.toFloat(), height: Float = SchemaEdge.CARD_H_DEFAULT.toFloat()): OffsetPair =
        when (this) {
            NORTH -> OffsetPair(width / 2f, 0f)
            EAST -> OffsetPair(width, height / 2f)
            SOUTH -> OffsetPair(width / 2f, height)
            WEST -> OffsetPair(0f, height / 2f)
        }

    companion object {
        fun nearest(localX: Float, localY: Float, width: Float, height: Float): SchemaPort {
            return entries.minBy { port ->
                val o = port.offset(width, height)
                val dx = localX - o.x
                val dy = localY - o.y
                dx * dx + dy * dy
            }
        }
    }
}

data class OffsetPair(val x: Float, val y: Float)

data class SchemaGraph(
    val nodes: List<SchemaNode> = emptyList(),
    val edges: List<SchemaEdge> = emptyList()
)

enum class MainPanelMode {
    NOTES,
    SCHEMA
}
