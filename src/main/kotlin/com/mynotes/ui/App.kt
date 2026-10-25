package com.mynotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mynotes.ui.components.FloatingDragGhost
import com.mynotes.ui.components.NoteEditor
import com.mynotes.ui.components.NoteListPanel
import com.mynotes.ui.components.SchemaPanel
import com.mynotes.ui.components.Sidebar
import com.mynotes.ui.theme.AppColors
import com.mynotes.data.MainPanelMode
import kotlin.math.roundToInt

@Composable
fun App(viewModel: AppViewModel) {
    val state = viewModel.state
    var rootOffsetX by remember { mutableFloatStateOf(0f) }
    var rootOffsetY by remember { mutableFloatStateOf(0f) }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(TestTags.APP_ROOT)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        rootOffsetX = pos.x
                        rootOffsetY = pos.y
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
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

                // Ghost sits so the grab handle stays under the cursor tip.
                val ghostTitle = state.draggingSuiteTitle ?: state.draggingNoteTitle
                val ghostHint = when {
                    state.draggingSuiteId != null -> state.suiteDragHint
                    else -> state.noteDragHint
                }
                val ghostIcon = when {
                    state.draggingSuiteId != null -> state.draggingSuiteIcon ?: "📁"
                    state.draggingNoteId != null -> "📄"
                    else -> null
                }
                if (ghostTitle != null && ghostIcon != null) {
                    val localX = viewModel.dragPointerX - rootOffsetX
                    val localY = viewModel.dragPointerY - rootOffsetY
                    FloatingDragGhost(
                        title = ghostTitle,
                        hint = ghostHint,
                        leadingIcon = ghostIcon,
                        modifier = Modifier
                            .zIndex(20f)
                            .offset {
                                // Offset so the ∷ handle (left side of card) tracks the pointer.
                                IntOffset(
                                    (localX - 18f).roundToInt(),
                                    (localY - 18f).roundToInt()
                                )
                            }
                    )
                }
            }
        }
    }
}
