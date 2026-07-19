package com.mynotes.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object SuitesTable : Table("suites") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val icon = varchar("icon", 8).default("📁")
    val createdAt = datetime("created_at")
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}

object NotesTable : Table("notes") {
    val id = varchar("id", 36)
    val suiteId = varchar("suite_id", 36).references(SuitesTable.id)
    val parentId = varchar("parent_id", 36).nullable()
    val title = varchar("title", 500)
    val content = text("content").default("")
    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object NoteImagesTable : Table("note_images") {
    val id = varchar("id", 36)
    val noteId = varchar("note_id", 36).references(NotesTable.id)
    val fileName = varchar("file_name", 255)
    val createdAt = datetime("created_at")
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}

/** Mind-map / project schema cards for a suite (olithin Schema). */
object SchemaNodesTable : Table("schema_nodes") {
    val id = varchar("id", 36)
    val suiteId = varchar("suite_id", 36).references(SuitesTable.id)
    val parentId = varchar("parent_id", 36).nullable()
    val linkedNoteId = varchar("linked_note_id", 36).nullable()
    val title = varchar("title", 500)
    val hint = text("hint").default("")
    val posX = double("pos_x").default(0.0)
    val posY = double("pos_y").default(0.0)
    val width = double("width").default(200.0)
    val height = double("height").default(80.0)
    val collapsed = bool("collapsed").default(false)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

/** Freeform links between schema cards (Miro-style arrows). */
object SchemaEdgesTable : Table("schema_edges") {
    val id = varchar("id", 36)
    val suiteId = varchar("suite_id", 36).references(SuitesTable.id)
    val fromNodeId = varchar("from_node_id", 36).references(SchemaNodesTable.id)
    val toNodeId = varchar("to_node_id", 36).references(SchemaNodesTable.id)
    val fromOffsetX = double("from_offset_x").default(200.0)
    val fromOffsetY = double("from_offset_y").default(40.0)
    val toOffsetX = double("to_offset_x").default(0.0)
    val toOffsetY = double("to_offset_y").default(40.0)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
