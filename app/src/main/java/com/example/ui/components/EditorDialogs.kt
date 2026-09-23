package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AvailableEditorThemes
import com.example.ui.theme.EditorThemeColors

@Composable
fun RenameDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Enter a new filename (e.g. script.py, index.html, notes.txt):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTitle.isNotBlank()) onConfirm(newTitle)
                    })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newTitle.isNotBlank()) onConfirm(newTitle) },
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GoToLineDialog(
    currentLine: Int,
    totalLines: Int,
    onGoToLine: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lineInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "You are currently at line $currentLine of $totalLines.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = {
                        lineInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("1 - $totalLines") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = {
                        val num = lineInput.toIntOrNull()
                        if (num != null && num in 1..totalLines.coerceAtLeast(1)) {
                            onGoToLine(num)
                        } else {
                            errorMessage = "Please enter a valid line between 1 and $totalLines"
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("go_to_line_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = lineInput.toIntOrNull()
                    if (num != null && num in 1..totalLines.coerceAtLeast(1)) {
                        onGoToLine(num)
                    } else {
                        errorMessage = "Please enter a valid line between 1 and $totalLines"
                    }
                },
                modifier = Modifier.testTag("go_to_line_button")
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThemeSelectorDialog(
    currentTheme: EditorThemeColors,
    onSelectTheme: (EditorThemeColors) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Editor Theme", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(AvailableEditorThemes) { theme ->
                    val isSelected = theme.id == currentTheme.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) theme.bookmarkColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelectTheme(theme) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color palette swatch circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(theme.background)
                                .border(2.dp, theme.bookmarkColor, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (theme.isDark) "Dark mode" else "Light mode",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = theme.bookmarkColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LineToolsDialog(
    theme: EditorThemeColors,
    onSortAscending: () -> Unit,
    onSortDescending: () -> Unit,
    onRemoveDuplicates: () -> Unit,
    onTrimWhitespace: () -> Unit,
    onJoinLines: () -> Unit,
    onReverseLines: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Line & Text Operations", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                LineToolActionItem(
                    title = "Sort Lines Ascending (A-Z)",
                    subtitle = "Order document lines alphabetically",
                    theme = theme,
                    onClick = {
                        onSortAscending()
                        onDismiss()
                    }
                )

                LineToolActionItem(
                    title = "Sort Lines Descending (Z-A)",
                    subtitle = "Reverse alphabetical sorting",
                    theme = theme,
                    onClick = {
                        onSortDescending()
                        onDismiss()
                    }
                )

                LineToolActionItem(
                    title = "Remove Duplicate Lines",
                    subtitle = "Deduplicate all unique lines in document",
                    theme = theme,
                    onClick = {
                        onRemoveDuplicates()
                        onDismiss()
                    }
                )

                LineToolActionItem(
                    title = "Trim Leading & Trailing Whitespace",
                    subtitle = "Clean up irregular spaces and indentation",
                    theme = theme,
                    onClick = {
                        onTrimWhitespace()
                        onDismiss()
                    }
                )

                LineToolActionItem(
                    title = "Join Lines",
                    subtitle = "Collapse lines into a single paragraph",
                    theme = theme,
                    onClick = {
                        onJoinLines()
                        onDismiss()
                    }
                )

                LineToolActionItem(
                    title = "Reverse All Lines",
                    subtitle = "Invert line order from top to bottom",
                    theme = theme,
                    onClick = {
                        onReverseLines()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun LineToolActionItem(
    title: String,
    subtitle: String,
    theme: EditorThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(theme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SupportTipJarDialog(
    theme: EditorThemeColors,
    onTipSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.notepadpp_icon_1789914595879),
                    contentDescription = "NoteCode++ Logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Support NoteCode++",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "NoteCode++ honors the legacy of desktop Notepad++: 100% free, privacy-first, and zero subscription paywalls.\n\nIf NoteCode++ boosts your productivity, consider dropping a tip in the developer jar to support continuous updates, new syntax lexers, and performance upgrades!",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val tipTiers = listOf(
                    Triple("☕ Coffee Tip", "$1.99", "A warm coffee for late-night coding"),
                    Triple("🍕 Pizza Slice", "$4.99", "Helps fuel feature development"),
                    Triple("🚀 Patron Badge", "$9.99", "Ultimate supporter of open developer tools")
                )

                tipTiers.forEach { (title, amount, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surface)
                            .border(1.dp, theme.bookmarkColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onTipSelected(amount) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(theme.bookmarkColor)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = amount,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}
