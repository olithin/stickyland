package com.mynotes

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mynotes.data.DatabaseFactory
import com.mynotes.ui.App
import com.mynotes.ui.AppViewModel

fun main() {
    try {
        DatabaseFactory.init()
    } catch (e: Exception) {
        e.printStackTrace()
        System.err.println("Failed to start Stickyland: ${e.message ?: e.javaClass.simpleName}")
        return
    }

    application {
        val viewModel = remember {
            AppViewModel().apply { loadInitialData() }
        }

        Window(
            onCloseRequest = {
                viewModel.dispose()
                exitApplication()
            },
            title = "Stickyland",
            state = rememberWindowState(width = 1280.dp, height = 800.dp)
        ) {
            App(viewModel)
        }
    }
}
