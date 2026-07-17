package com.mynotes.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage

object ClipboardImage {
    fun readImage(): BufferedImage? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return try {
            when {
                clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) ->
                    clipboard.getData(DataFlavor.imageFlavor) as? BufferedImage
                clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) -> null
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasImage(): Boolean {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)
    }
}
