package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorThemeColors
import com.example.ui.viewmodel.CaseType

@Composable
fun EditorToolbar(
    isModified: Boolean,
    wordWrap: Boolean,
    showWhitespace: Boolean,
    hasBookmarks: Boolean,
    theme: EditorThemeColors,
    onSave: () -> Unit,
    onExportFile: () -> Unit,
    onOpenFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleFind: () -> Unit,
    onToggleReplace: () -> Unit,
    onToggleBookmark: () -> Unit,
    onNextBookmark: () -> Unit,
    onPrevBookmark: () -> Unit,
    onClearBookmarks: () -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onDuplicateLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onToggleComment: () -> Unit,
    onInsertTimestamp: () -> Unit,
    onChangeCase: (CaseType) -> Unit,
    onToggleWordWrap: () -> Unit,
    onToggleWhitespace: () -> Unit,
    onShowLineTools: () -> Unit,
    onShowSupport: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPreview: () -> Unit,
    onShowStats: () -> Unit,
    onSelectTheme: () -> Unit,
    onGoToLine: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showDevToolsMenu by remember { mutableStateOf(false) }
    var showBookmarksMenu by remember { mutableStateOf(false) }
    var showCaseMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = theme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Open File from device storage
            ToolbarButton(
                icon = Icons.Default.FileOpen,
                contentDescription = "Open File from Device",
                testTag = "toolbar_open_file",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onOpenFile
            )

            // Save button with modified highlight
            ToolbarButton(
                icon = Icons.Default.Save,
                contentDescription = "Save Document",
                testTag = "toolbar_save",
                tint = if (isModified) Color(0xFF4CAF50) else theme.text.copy(alpha = 0.8f),
                onClick = onSave
            )

            // Save As to Device Storage
            ToolbarButton(
                icon = Icons.Default.SaveAs,
                contentDescription = "Save As...",
                testTag = "toolbar_export_file",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onExportFile
            )

            ToolbarDivider(theme = theme)

            // Undo & Redo
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                testTag = "toolbar_undo",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onUndo
            )

            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                testTag = "toolbar_redo",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onRedo
            )

            ToolbarDivider(theme = theme)

            // Find & Replace
            ToolbarButton(
                icon = Icons.Default.Search,
                contentDescription = "Find",
                testTag = "toolbar_find",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onToggleFind
            )

            ToolbarButton(
                icon = Icons.Default.FindReplace,
                contentDescription = "Find and Replace",
                testTag = "toolbar_replace",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onToggleReplace
            )

            ToolbarDivider(theme = theme)

            // Bookmarks Menu
            Box {
                ToolbarButton(
                    icon = if (hasBookmarks) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmarks Menu",
                    testTag = "toolbar_bookmarks",
                    tint = if (hasBookmarks) theme.bookmarkColor else theme.text.copy(alpha = 0.8f),
                    onClick = { showBookmarksMenu = true }
                )

                DropdownMenu(
                    expanded = showBookmarksMenu,
                    onDismissRequest = { showBookmarksMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Toggle Bookmark on Current Line") },
                        onClick = {
                            showBookmarksMenu = false
                            onToggleBookmark()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Next Bookmark (F2)") },
                        onClick = {
                            showBookmarksMenu = false
                            onNextBookmark()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Previous Bookmark (Shift+F2)") },
                        onClick = {
                            showBookmarksMenu = false
                            onPrevBookmark()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear All Bookmarks") },
                        onClick = {
                            showBookmarksMenu = false
                            onClearBookmarks()
                        }
                    )
                }
            }

            // Indent & Unindent
            ToolbarButton(
                icon = Icons.Default.FormatIndentIncrease,
                contentDescription = "Indent",
                testTag = "toolbar_indent",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onIndent
            )

            ToolbarButton(
                icon = Icons.Default.FormatIndentDecrease,
                contentDescription = "Unindent",
                testTag = "toolbar_unindent",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onUnindent
            )

            ToolbarDivider(theme = theme)

            // Developer Tools (Comment, Duplicate, Case, Timestamp)
            Box {
                ToolbarButton(
                    icon = Icons.Default.Code,
                    contentDescription = "Code Tools",
                    testTag = "toolbar_dev_tools",
                    tint = theme.text.copy(alpha = 0.85f),
                    onClick = { showDevToolsMenu = true }
                )

                DropdownMenu(
                    expanded = showDevToolsMenu,
                    onDismissRequest = { showDevToolsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Toggle Line Comment (//)") },
                        onClick = {
                            showDevToolsMenu = false
                            onToggleComment()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate Current Line") },
                        onClick = {
                            showDevToolsMenu = false
                            onDuplicateLine()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Current Line") },
                        onClick = {
                            showDevToolsMenu = false
                            onDeleteLine()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Insert Date & Time (F5)") },
                        onClick = {
                            showDevToolsMenu = false
                            onInsertTimestamp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Go to Line... (Ctrl+G)") },
                        onClick = {
                            showDevToolsMenu = false
                            onGoToLine()
                        }
                    )
                }
            }

            // Case Converter Menu
            Box {
                ToolbarButton(
                    icon = Icons.Default.TextFields,
                    contentDescription = "Convert Case",
                    testTag = "toolbar_case",
                    tint = theme.text.copy(alpha = 0.85f),
                    onClick = { showCaseMenu = true }
                )

                DropdownMenu(
                    expanded = showCaseMenu,
                    onDismissRequest = { showCaseMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("UPPERCASE") },
                        onClick = {
                            showCaseMenu = false
                            onChangeCase(CaseType.UPPERCASE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("lowercase") },
                        onClick = {
                            showCaseMenu = false
                            onChangeCase(CaseType.LOWERCASE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Title Case") },
                        onClick = {
                            showCaseMenu = false
                            onChangeCase(CaseType.TITLE_CASE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("camelCase") },
                        onClick = {
                            showCaseMenu = false
                            onChangeCase(CaseType.CAMEL_CASE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("snake_case") },
                        onClick = {
                            showCaseMenu = false
                            onChangeCase(CaseType.SNAKE_CASE)
                        }
                    )
                }
            }

            ToolbarDivider(theme = theme)

            // Word wrap toggle
            ToolbarButton(
                icon = Icons.Default.WrapText,
                contentDescription = "Toggle Word Wrap",
                testTag = "toolbar_word_wrap",
                tint = if (wordWrap) theme.bookmarkColor else theme.text.copy(alpha = 0.5f),
                onClick = onToggleWordWrap
            )

            // Show White Space / End of Line (¶)
            ToolbarButton(
                icon = Icons.Default.FormatAlignLeft,
                contentDescription = "Show White Space (¶)",
                testTag = "toolbar_whitespace",
                tint = if (showWhitespace) theme.bookmarkColor else theme.text.copy(alpha = 0.5f),
                onClick = onToggleWhitespace
            )

            // Line Operations (Sort, Deduplicate, Trim, Join)
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = "Line Operations (Sort, Clean)",
                testTag = "toolbar_line_tools",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onShowLineTools
            )

            // Zoom In & Out
            ToolbarButton(
                icon = Icons.Default.ZoomIn,
                contentDescription = "Zoom In",
                testTag = "toolbar_zoom_in",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onZoomIn
            )

            ToolbarButton(
                icon = Icons.Default.ZoomOut,
                contentDescription = "Zoom Out",
                testTag = "toolbar_zoom_out",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onZoomOut
            )

            ToolbarDivider(theme = theme)

            // Run / Live HTML/Markdown Preview
            ToolbarButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Run / Preview",
                testTag = "toolbar_preview",
                tint = Color(0xFF4CAF50),
                onClick = onPreview
            )

            // Stats / Info
            ToolbarButton(
                icon = Icons.Default.Info,
                contentDescription = "Document Statistics",
                testTag = "toolbar_stats",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onShowStats
            )

            // Theme selector
            ToolbarButton(
                icon = Icons.Default.Palette,
                contentDescription = "Editor Theme",
                testTag = "toolbar_theme",
                tint = theme.text.copy(alpha = 0.85f),
                onClick = onSelectTheme
            )

            // Support NoteCode++ (Monetization Tip Jar)
            ToolbarButton(
                icon = Icons.Default.VolunteerActivism,
                contentDescription = "Support NoteCode++",
                testTag = "toolbar_support",
                tint = Color(0xFFFFB300),
                onClick = onShowSupport
            )

            // More actions (Rename file, Go to line, Support)
            Box {
                ToolbarButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    testTag = "toolbar_more",
                    tint = theme.text.copy(alpha = 0.85f),
                    onClick = { showMoreMenu = true }
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open File from Device...") },
                        onClick = {
                            showMoreMenu = false
                            onOpenFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As...") },
                        onClick = {
                            showMoreMenu = false
                            onExportFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Current File") },
                        onClick = {
                            showMoreMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Go to Line...") },
                        onClick = {
                            showMoreMenu = false
                            onGoToLine()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Support NoteCode++ for Android") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = Color(0xFF3DDC84),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            onShowSupport()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ToolbarDivider(theme: EditorThemeColors) {
    Spacer(modifier = Modifier.width(4.dp))
    VerticalDivider(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp),
        color = theme.gutterText.copy(alpha = 0.25f)
    )
    Spacer(modifier = Modifier.width(4.dp))
}
