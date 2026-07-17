package com.mynotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynotes.ui.theme.AppColors

@Composable
fun NotionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.Accent,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledBackgroundColor = backgroundColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun NotionTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppColors.TextSecondary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun NotionIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String? = null,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp)) {
        content()
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = AppColors.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        backgroundColor = AppColors.Card,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun InputDialog(
    title: String,
    initialValue: String = "",
    placeholder: String = "",
    confirmText: String = "Save",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.Border
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text(confirmText, color = AppColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        backgroundColor = AppColors.Card,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun SidebarItem(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) AppColors.SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Text(
            title,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            fontSize = 14.sp,
            color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
            maxLines = 1
        )
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextMuted,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search notes..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = { Text(placeholder, color = AppColors.TextMuted, fontSize = 14.sp) },
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = AppColors.InputBackground,
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Border,
            textColor = AppColors.TextPrimary
        ),
        shape = RoundedCornerShape(6.dp)
    )
}
