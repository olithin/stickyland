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
