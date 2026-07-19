package com.mynotes.util

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO

object ClipboardImage {
    private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "tif", "tiff")

    fun hasImage(): Boolean = readImage() != null

    fun readImage(): BufferedImage? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val transferable = try {
            clipboard.getContents(null)
        } catch (_: Exception) {
            null
        } ?: return null

        return readFromImageFlavor(transferable)
            ?: readFromImageBytes(transferable)
            ?: readFromFileList(transferable)
    }

    private fun readFromImageFlavor(transferable: Transferable): BufferedImage? {
        if (!transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) return null
        return try {
            when (val data = transferable.getTransferData(DataFlavor.imageFlavor)) {
                is BufferedImage -> data
                is Image -> toBufferedImage(data)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readFromImageBytes(transferable: Transferable): BufferedImage? {
        for (flavor in transferable.transferDataFlavors) {
            val mime = flavor.mimeType.lowercase()
            val looksLikeImage =
                mime.startsWith("image/") ||
                    mime.contains("png") ||
                    mime.contains("jpeg") ||
                    mime.contains("jpg") ||
                    mime.contains("tiff")
            if (!looksLikeImage) continue

            val image = try {
                when (val data = transferable.getTransferData(flavor)) {
                    is BufferedImage -> data
                    is Image -> toBufferedImage(data)
                    is ByteArray -> ImageIO.read(ByteArrayInputStream(data))
                    is InputStream -> data.use { ImageIO.read(it) }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
            if (image != null) return image
        }
        return null
    }

    private fun readFromFileList(transferable: Transferable): BufferedImage? {
        if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null
        return try {
            @Suppress("UNCHECKED_CAST")
            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
            val file = files
                ?.filterIsInstance<File>()
                ?.firstOrNull { it.extension.lowercase() in imageExtensions }
                ?: return null
            ImageIO.read(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun toBufferedImage(image: Image): BufferedImage? {
        if (image is BufferedImage) return image
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        if (width <= 0 || height <= 0) return null
        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = buffered.createGraphics()
        try {
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return buffered
    }
}
