package com.mynotes.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DropRectTest {

    @Test
    fun `contains is inclusive within bounds`() {
        val rect = DropRect(left = 10f, top = 20f, right = 100f, bottom = 80f)
        assertTrue(rect.contains(10f, 20f))
        assertTrue(rect.contains(50f, 50f))
        assertTrue(rect.contains(100f, 80f))
        assertFalse(rect.contains(9f, 50f))
        assertFalse(rect.contains(50f, 81f))
    }
}
