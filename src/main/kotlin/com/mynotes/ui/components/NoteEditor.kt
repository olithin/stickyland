package com.mynotes.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mynotes.data.NoteContentCodec
import com.mynotes.ui.AppViewModel
import com.mynotes.ui.theme.AppColors
import com.mynotes.util.ClipboardImage
import org.jetbrains.skia.Image
import java.io.File

@Composable
fun NoteEditor(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    val noteWithImages = state.currentNote
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (noteWithImages == null) {
        EmptyEditor(modifier)
        return
    }

    val note = noteWithImages.note
    val blocks = remember(note.content, noteWithImages.images) {
        NoteContentCodec.parse(note.content, noteWithImages.images)
    }

    var titleState by remember(note.id) { mutableStateOf(TextFieldValue(note.title)) }
    var focusedBlockIndex by remember(note.id) { mutableIntStateOf(0) }
    var textFieldStates by remember(note.id) {
        mutableStateOf(
            blocks.mapIndexedNotNull { index, block ->
                if (block.type == "text") index to TextFieldValue(block.content) else null
            }.toMap()
        )
    }

    LaunchedEffect(note.title) {
        if (titleState.text != note.title) {
            titleState = TextFieldValue(note.title)
        }
    }

    LaunchedEffect(note.content) {
        val parsed = NoteContentCodec.parse(note.content, noteWithImages.images)
        textFieldStates = parsed.mapIndexedNotNull { index, block ->
            if (block.type != "text") return@mapIndexedNotNull null
            val existing = textFieldStates[index]
            val value = if (existing != null && existing.text == block.content) {
                existing
            } else {
                TextFieldValue(block.content)
            }
            index to value
        }.toMap()
    }

    fun tryPasteScreenshot(): Boolean {
        if (!ClipboardImage.hasImage()) return false
        val targetIndex = textFieldStates.keys
            .filter { it <= focusedBlockIndex }
            .maxOrNull()
            ?: textFieldStates.keys.minOrNull()
            ?: return false
        val cursor = textFieldStates[targetIndex]?.selection?.start
            ?: textFieldStates[targetIndex]?.text?.length
            ?: 0
        return viewModel.pasteImageFromClipboard(targetIndex, cursor)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // Notion-style: Cmd+V (Mac) / Ctrl+V (Windows) pastes screenshot into the note.
                val isPaste =
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.V &&
                        (event.isMetaPressed || event.isCtrlPressed)
                if (isPaste && tryPasteScreenshot()) true else false
            }
            .padding(horizontal = 48.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Created: ${note.createdAt.format(viewModel.dateFormatter)}",
                    fontSize = 12.sp,
                    color = AppColors.TextMuted
                )
                if (note.updatedAt != note.createdAt) {
                    Text(
                        "Modified: ${note.updatedAt.format(viewModel.dateFormatter)}",
                        fontSize = 12.sp,
                        color = AppColors.TextMuted
                    )
                }
            }
            NotionButton(
                text = "Delete",
                onClick = { showDeleteConfirm = true },
                backgroundColor = AppColors.Danger.copy(alpha = 0.1f),
                contentColor = AppColors.Danger
            )
        }

        Spacer(Modifier.height(24.dp))

        BasicTextField(
            value = titleState,
            onValueChange = { newValue ->
                titleState = newValue
                viewModel.updateTitle(newValue.text)
            },
            textStyle = TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (titleState.text.isEmpty()) {
                        Text(
                            "Untitled",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Divider(color = AppColors.Border)
        Spacer(Modifier.height(16.dp))

        blocks.forEachIndexed { index, block ->
            when (block.type) {
                "text" -> {
                    val fieldValue = textFieldStates[index] ?: TextFieldValue(block.content)
                    val isOnlyEmptyBlock = blocks.size == 1 && block.content.isEmpty()

                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { newValue ->
                            textFieldStates = textFieldStates + (index to newValue)
                            viewModel.updateBlockText(index, newValue.text)
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = AppColors.TextPrimary,
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = if (isOnlyEmptyBlock) 200.dp else 48.dp)
                            .onFocusChanged { focus ->
                                if (focus.isFocused) focusedBlockIndex = index
                            },
                        decorationBox = { inner ->
                            Box {
                                if (isOnlyEmptyBlock && fieldValue.text.isEmpty()) {
                                    Text(
                                        "Start writing, or paste a screenshot…",
                                        fontSize = 16.sp,
                                        color = AppColors.TextMuted.copy(alpha = 0.5f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                "image" -> {
                    val image = noteWithImages.images.find { it.id == block.imageId }
                    if (image != null) {
                        InlineImageBlock(
                            imageFile = viewModel.getImageFile(image),
                            onClick = { viewModel.showLightbox(image) },
                            onDelete = { viewModel.deleteImage(image.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete note?",
            message = "Note \"${note.title}\" and all screenshots will be permanently deleted.",
            onConfirm = {
                viewModel.deleteCurrentNote()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    state.lightboxImage?.let { image ->
        ImageLightbox(
            imageFile = viewModel.getImageFile(image),
            onDismiss = viewModel::hideLightbox,
            onDelete = {
                viewModel.deleteImage(image.id)
                viewModel.hideLightbox()
            }
        )
    }
}

@Composable
private fun EmptyEditor(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨", fontSize = 48.sp)
            Text(
                "Select or create a note",
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 18.sp,
                color = AppColors.TextMuted
            )
        }
    }
}

@Composable
private fun InlineImageBlock(
    imageFile: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val bitmap = remember(imageFile.absolutePath) { loadImageBitmap(imageFile) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Screenshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(AppColors.InputBackground),
                contentAlignment = Alignment.Center
            ) {
                Text("🖼", fontSize = 32.sp)
            }
        }

        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete screenshot?",
            message = "The image will be removed from the note.",
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun ImageLightbox(
    imageFile: File,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val bitmap = remember(imageFile.absolutePath) { loadImageBitmap(imageFile) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Overlay)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .clickable(enabled = false, onClick = {})
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    NotionButton(
                        text = "Delete",
                        onClick = { showDeleteConfirm = true },
                        backgroundColor = AppColors.Danger.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(8.dp))
                    NotionButton(
                        text = "✕ Close",
                        onClick = onDismiss,
                        backgroundColor = Color.White.copy(alpha = 0.2f)
                    )
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Enlarged screenshot",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 800.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Failed to load image", color = Color.White)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete screenshot?",
            message = "The image will be removed from the note.",
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

private fun loadImageBitmap(file: File): ImageBitmap? {
    if (!file.exists()) return null
    return try {
        Image.makeFromEncoded(file.readBytes()).asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
