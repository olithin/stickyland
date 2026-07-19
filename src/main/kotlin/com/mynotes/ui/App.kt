package com.mynotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mynotes.ui.components.NoteEditor
import com.mynotes.ui.components.NoteListPanel
import com.mynotes.ui.components.SchemaPanel
import com.mynotes.ui.components.Sidebar
import com.mynotes.ui.theme.AppColors
import com.mynotes.data.MainPanelMode

@Composable
fun App(viewModel: AppViewModel) {
    val state = viewModel.state

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            kotlinx.coroutines.delay(2500)
            viewModel.clearStatus()
        }
    }

    MaterialTheme(
        colors = lightColors(
            primary = AppColors.Accent,
            background = AppColors.Background,
            surface = AppColors.Card
        )
    ) {
        Scaffold(
            snackbarHost = {
                state.statusMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        backgroundColor = AppColors.TextPrimary,
                        content = { Text(message, color = AppColors.Card) }
                    )
                }
            },
            backgroundColor = AppColors.Background
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Sidebar(
                    viewModel = viewModel,
                    modifier = Modifier.background(AppColors.Sidebar)
                )
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = AppColors.Border
                )
                if (state.mainPanelMode == MainPanelMode.NOTES) {
                    NoteListPanel(
                        viewModel = viewModel,
                        modifier = Modifier.background(AppColors.Background)
                    )
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = AppColors.Border
                    )
                    NoteEditor(
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .background(AppColors.Background)
                    )
                } else {
                    SchemaPanel(
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .background(AppColors.Background)
                    )
                }
            }
        }
    }
}
