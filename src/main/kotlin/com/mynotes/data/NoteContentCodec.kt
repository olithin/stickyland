package com.mynotes.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BlockDto(
    val type: String,
    val content: String = "",
    val imageId: String = ""
)

@Serializable
internal data class BlocksWrapper(val blocks: List<BlockDto>)

object NoteContentCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun isStructured(content: String): Boolean =
        content.trimStart().startsWith("{\"blocks\"")

    fun parse(content: String, images: List<NoteImage> = emptyList()): List<BlockDto> {
        if (content.isBlank()) {
            return migrateOrphans(mutableListOf(BlockDto(type = "text")), images)
        }
        if (!isStructured(content)) {
            return migrateOrphans(mutableListOf(BlockDto(type = "text", content = content)), images)
        }
        return try {
            val blocks = json.decodeFromString<BlocksWrapper>(content).blocks.toMutableList()
            migrateOrphans(blocks, images)
        } catch (_: Exception) {
            migrateOrphans(mutableListOf(BlockDto(type = "text", content = content)), images)
        }
    }

    fun encode(blocks: List<BlockDto>): String {
        val normalized = normalize(blocks.ifEmpty { listOf(BlockDto(type = "text")) })
        return json.encodeToString(BlocksWrapper.serializer(), BlocksWrapper(normalized))
    }

    fun plainText(blocks: List<BlockDto>): String =
        blocks.filter { it.type == "text" }.joinToString("\n") { it.content }

    fun normalize(blocks: List<BlockDto>): List<BlockDto> {
        val result = mutableListOf<BlockDto>()
        for (block in blocks) {
            when (block.type) {
                "image" -> {
                    if (block.imageId.isNotBlank()) {
                        result.add(block)
                    }
                }
                else -> {
                    if (result.isNotEmpty() && result.last().type == "text") {
                        val last = result.removeAt(result.lastIndex)
                        result.add(last.copy(content = last.content + block.content))
                    } else {
                        result.add(block.copy(type = "text"))
                    }
                }
            }
        }
        return if (result.isEmpty()) listOf(BlockDto(type = "text")) else result
    }

    fun insertImageAt(
        blocks: List<BlockDto>,
        textBlockIndex: Int,
        cursor: Int,
        imageId: String
    ): List<BlockDto> {
        if (textBlockIndex !in blocks.indices || blocks[textBlockIndex].type != "text") {
            return normalize(blocks + BlockDto(type = "image", imageId = imageId) + BlockDto(type = "text"))
        }
        val text = blocks[textBlockIndex].content
        val safeCursor = cursor.coerceIn(0, text.length)
        val before = text.substring(0, safeCursor)
        val after = text.substring(safeCursor)
        val updated = blocks.toMutableList()
        updated[textBlockIndex] = BlockDto(type = "text", content = before)
        updated.add(textBlockIndex + 1, BlockDto(type = "image", imageId = imageId))
        updated.add(textBlockIndex + 2, BlockDto(type = "text", content = after))
        return updated
    }

    fun removeImage(blocks: List<BlockDto>, imageId: String): List<BlockDto> {
        val filtered = blocks.filterNot { it.type == "image" && it.imageId == imageId }
        return normalize(if (filtered.isEmpty()) listOf(BlockDto(type = "text")) else filtered)
    }

    private fun migrateOrphans(blocks: MutableList<BlockDto>, images: List<NoteImage>): List<BlockDto> {
        val referenced = blocks.filter { it.type == "image" }.map { it.imageId }.toSet()
        images.filter { it.id !in referenced }.forEach { image ->
            blocks.add(BlockDto(type = "image", imageId = image.id))
        }
        return normalize(blocks)
    }
}
