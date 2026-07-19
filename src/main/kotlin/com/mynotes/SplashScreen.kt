package com.mynotes

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

object SplashScreen {
    private var window: JWindow? = null

    fun show() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
        }
        SwingUtilities.invokeAndWait {
            val panel = JPanel(BorderLayout()).apply {
                border = EmptyBorder(24, 32, 24, 32)
                add(
                    JLabel(
                        "<html><center>" +
                            "<span style='font-size:18px;font-family:Segoe UI'>Stickyland</span>" +
                            "<br><span style='font-size:11px;font-family:Segoe UI;color:#9B9A97;letter-spacing:2px'>olithin</span>" +
                            "<br><span style='font-size:12px;font-family:Segoe UI;color:#666666'>Loading...</span>" +
                            "</center></html>",
                        SwingConstants.CENTER
                    ),
                    BorderLayout.CENTER
                )
            }
            window = JWindow().apply {
                contentPane = panel
                size = Dimension(260, 110)
                setLocationRelativeTo(null)
                isAlwaysOnTop = true
                isVisible = true
            }
        }
    }

    fun hide() {
        SwingUtilities.invokeLater {
            window?.isVisible = false
            window?.dispose()
            window = null
        }
    }
}
