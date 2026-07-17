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
