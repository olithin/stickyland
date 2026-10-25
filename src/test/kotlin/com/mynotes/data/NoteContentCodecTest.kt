package com.mynotes.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteContentCodecTest {

    @Test
    fun `isStructured detects blocks json`() {
        assertTrue(NoteContentCodec.isStructured("""{"blocks":[]}"""))
        assertFalse(NoteContentCodec.isStructured("plain text"))
        assertFalse(NoteContentCodec.isStructured(""))
    }

    @Test
    fun `parse blank and plain text`() {
        val blank = NoteContentCodec.parse("")
        assertEquals(1, blank.size)
        assertEquals("text", blank[0].type)
        assertEquals("", blank[0].content)

        val plain = NoteContentCodec.parse("hello")
        assertEquals("hello", plain.single().content)
    }

    @Test
    fun `encode and parse round trip`() {
        val blocks = listOf(
            BlockDto(type = "text", content = "hi"),
            BlockDto(type = "image", imageId = "img-1"),
            BlockDto(type = "text", content = "there")
        )
        val encoded = NoteContentCodec.encode(blocks)
        assertTrue(NoteContentCodec.isStructured(encoded))
        val parsed = NoteContentCodec.parse(encoded)
        assertEquals(3, parsed.size)
        assertEquals("hi", parsed[0].content)
        assertEquals("img-1", parsed[1].imageId)
        assertEquals("there", parsed[2].content)
    }

    @Test
    fun `normalize merges adjacent text and drops empty images`() {
        val normalized = NoteContentCodec.normalize(
            listOf(
                BlockDto(type = "text", content = "a"),
                BlockDto(type = "text", content = "b"),
                BlockDto(type = "image", imageId = ""),
                BlockDto(type = "image", imageId = "ok")
            )
        )
        assertEquals(2, normalized.size)
        assertEquals("ab", normalized[0].content)
        assertEquals("ok", normalized[1].imageId)
    }

    @Test
    fun `plainText joins text blocks`() {
        val text = NoteContentCodec.plainText(
            listOf(
                BlockDto(type = "text", content = "one"),
                BlockDto(type = "image", imageId = "x"),
                BlockDto(type = "text", content = "two")
            )
        )
        assertEquals("one\ntwo", text)
    }

    @Test
    fun `insertImageAt splits text at cursor`() {
        val blocks = listOf(BlockDto(type = "text", content = "abcdef"))
        val result = NoteContentCodec.insertImageAt(blocks, 0, 3, "img")
        assertEquals(3, result.size)
        assertEquals("abc", result[0].content)
        assertEquals("img", result[1].imageId)
        assertEquals("def", result[2].content)
    }

    @Test
    fun `insertImageAt appends when index invalid`() {
        val result = NoteContentCodec.insertImageAt(emptyList(), 5, 0, "img")
        assertTrue(result.any { it.type == "image" && it.imageId == "img" })
        assertTrue(result.any { it.type == "text" })
    }

    @Test
    fun `removeImage strips block and keeps text`() {
        val blocks = listOf(
            BlockDto(type = "text", content = "a"),
            BlockDto(type = "image", imageId = "gone"),
            BlockDto(type = "text", content = "b")
        )
        val result = NoteContentCodec.removeImage(blocks, "gone")
        assertEquals(1, result.size)
        assertEquals("ab", result[0].content)
    }

    @Test
    fun `parse migrates orphan images into blocks`() {
        val images = listOf(NoteImage(id = "orphan", noteId = "n", fileName = "x.png"))
        val parsed = NoteContentCodec.parse("hello", images)
        assertTrue(parsed.any { it.type == "image" && it.imageId == "orphan" })
        assertTrue(parsed.any { it.type == "text" && it.content == "hello" })
    }

    @Test
    fun `parse recovers from invalid json`() {
        val parsed = NoteContentCodec.parse("""{"blocks": not-json""")
        assertEquals("{\"blocks\": not-json", parsed.single().content)
    }
}
