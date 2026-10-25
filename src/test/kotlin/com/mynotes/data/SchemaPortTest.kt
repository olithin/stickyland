package com.mynotes.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaPortTest {

    @Test
    fun `nearest picks closest mid-side port`() {
        val w = 200f
        val h = 80f
        assertEquals(SchemaPort.WEST, SchemaPort.nearest(0f, 40f, w, h))
        assertEquals(SchemaPort.EAST, SchemaPort.nearest(200f, 40f, w, h))
        assertEquals(SchemaPort.NORTH, SchemaPort.nearest(100f, 0f, w, h))
        assertEquals(SchemaPort.SOUTH, SchemaPort.nearest(100f, 80f, w, h))
    }

    @Test
    fun `offset matches card mid-sides`() {
        val east = SchemaPort.EAST.offset(200f, 80f)
        assertEquals(200f, east.x)
        assertEquals(40f, east.y)
    }
}
