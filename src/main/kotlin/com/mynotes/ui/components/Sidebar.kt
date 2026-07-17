package com.mynotes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynotes.data.NoteTree
import com.mynotes.data.Suite
import com.mynotes.ui.AppViewModel
import com.mynotes.ui.theme.AppColors
import java.time.format.DateTimeFormatter

@Composable
fun Sidebar(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    var showCreateSuite by remember { mutableStateOf(false) }
    var suiteToDelete by remember { mutableStateOf<Suite?>(null) }
    var suiteToRename by remember { mutableStateOf<Suite?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝", fontSize = 22.sp)
            Text(
                "Stickyland",
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        }

        SearchField(
            query = state.searchQuery,
            onQueryChange = viewModel::setSearchQuery,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        SectionHeader("Suites")

        Box(modifier = Modifier.weight(1f)) {
            SuiteTreeList(
                suites = state.suites,
                selectedSuiteId = if (state.isSearching) null else state.selectedSuiteId,
                onSelect = viewModel::selectSuite,
                onRename = { suiteToRename = it },
                onDelete = { suiteToDelete = it },
                onMove = viewModel::moveSuite
            )
        }

        NotionButton(
            text = "+ New suite",
            onClick = { showCreateSuite = true },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            backgroundColor = AppColors.SidebarSelected,
            contentColor = AppColors.TextPrimary
        )
    }

    if (showCreateSuite) {
        InputDialog(
            title = "New suite",
            placeholder = "Topic name...",
            confirmText = "Create",
            onConfirm = { name ->
                viewModel.createSuite(name)
                showCreateSuite = false
            },
            onDismiss = { showCreateSuite = false }
        )
    }

    suiteToDelete?.let { suite ->
        ConfirmDialog(
            title = "Delete suite?",
            message = "Suite \"${suite.name}\" and all its notes will be permanently deleted.",
            onConfirm = {
                viewModel.deleteSuite(suite.id)
                suiteToDelete = null
            },
            onDismiss = { suiteToDelete = null }
        )
    }

    suiteToRename?.let { suite ->
        InputDialog(
            title = "Rename suite",
            initialValue = suite.name,
            onConfirm = { name ->
                viewModel.renameSuite(suite.id, name)
                suiteToRename = null
            },
            onDismiss = { suiteToRename = null }
        )
    }
}

@Composable
fun NoteListPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val treeItems = remember(state.notes, state.expandedNoteIds) {
        NoteTree.flatten(state.notes, state.expandedNoteIds)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(8.dp)
    ) {
        if (state.isSearching) {
            SectionHeader("Search results")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.searchResults, key = { it.first.id }) { (note, suite) ->
                    SidebarItem(
                        icon = "📄",
                        title = note.title,
                        selected = state.selectedNoteId == note.id,
                        onClick = {
                            viewModel.selectSuite(suite.id)
                            viewModel.selectNote(note.id)
                        }
                    )
                }
                if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "No results found",
                            modifier = Modifier.padding(16.dp),
                            color = AppColors.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val selectedSuite = state.suites.find { it.id == state.selectedSuiteId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        selectedSuite?.name ?: "Notes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "${state.notes.size} notes",
                        fontSize = 12.sp,
                        color = AppColors.TextMuted
                    )
                }
                NotionIconButton(onClick = { viewModel.createNote() }) {
                    Icon(Icons.Default.Add, "New note", tint = AppColors.Accent)
                }
            }

            Text(
                "Hold ⠿ and drag to reorder or nest notes",
                fontSize = 11.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            NoteTreeList(
                viewModel = viewModel,
                treeItems = treeItems,
                selectedNoteId = state.selectedNoteId,
                dateFormatter = dateFormatter,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
