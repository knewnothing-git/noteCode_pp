package com.example.ui.screen

import android.content.ContentUris
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.DocumentStatsDialog
import com.example.ui.components.EditorCanvas
import com.example.ui.components.EditorFindReplaceBar
import com.example.ui.components.EditorStatusBar
import com.example.ui.components.EditorTabBar
import com.example.ui.components.EditorToolbar
import com.example.ui.components.GoToLineDialog
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.LineToolsDialog
import com.example.ui.components.PreviewDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.SupportTipJarDialog
import com.example.ui.components.SymbolQuickBar
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private fun extractFileNameFromUri(context: android.content.Context, uri: Uri, content: String): String {
    var resolvedName: String? = null

    // Method 1: Query the URI directly with projection = null to fetch all available metadata columns
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val candidateColumns = listOf(
                    OpenableColumns.DISPLAY_NAME,
                    "_display_name",
                    "title",
                    "_data"
                )
                for (col in candidateColumns) {
                    val idx = cursor.getColumnIndex(col)
                    if (idx >= 0) {
                        val str = cursor.getString(idx)
                        if (!str.isNullOrBlank() && !str.startsWith("document:") && !str.startsWith("raw:") && !str.all { it.isDigit() }) {
                            resolvedName = str.substringAfterLast('/')
                            break
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    // Method 2: If resolvedName is still missing or looks like an internal ID, resolve via MediaStore or DocumentContract
    if (resolvedName == null || resolvedName.startsWith("document:") || resolvedName.all { it.isDigit() }) {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("raw:") || docId.startsWith("primary:")) {
                    resolvedName = docId.substringAfter(':').substringAfterLast('/')
                } else {
                    val numericId = docId.substringAfterLast(':').toLongOrNull()
                    if (numericId != null) {
                        val mediaUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), numericId)
                        context.contentResolver.query(mediaUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0) {
                                    val name = cursor.getString(idx)
                                    if (!name.isNullOrBlank()) resolvedName = name
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Method 3: Fallback from URI path segments
    if (resolvedName == null || resolvedName.startsWith("document:") || resolvedName.all { it.isDigit() }) {
        val segment = uri.lastPathSegment ?: ""
        if (segment.isNotEmpty() && !segment.startsWith("document:") && !segment.all { it.isDigit() }) {
            resolvedName = segment.substringAfterLast('/')
        }
    }

    // Method 4: Infer from document content if name is still raw or generic
    if (resolvedName == null || resolvedName.startsWith("document:") || resolvedName.all { it.isDigit() } || resolvedName == "document.txt") {
        val firstLines = content.lineSequence().take(15).toList()
        val headingLine = firstLines.firstOrNull { it.trimStart().startsWith("#") }
        if (headingLine != null) {
            val titleText = headingLine.trimStart('#', ' ').take(30).trim()
            if (titleText.isNotEmpty()) {
                val sanitized = titleText.replace(Regex("[^a-zA-Z0-9 _.-]"), "").trim()
                if (sanitized.isNotBlank()) {
                    resolvedName = "$sanitized.md"
                }
            }
        }
    }

    // Clean any remaining invalid prefixes
    var finalName = resolvedName ?: "document.txt"
    if (finalName.startsWith("document:")) {
        finalName = finalName.removePrefix("document:").trim()
    }
    if (finalName.isEmpty() || finalName.all { it.isDigit() }) {
        finalName = "document.txt"
    }

    return finalName
}

private fun extractSavedFileName(
    context: android.content.Context,
    uri: Uri,
    suggestedName: String,
    fallbackTitle: String
): String {
    var resolvedName: String? = null

    // Method 1: Query OpenableColumns.DISPLAY_NAME on the returned document URI
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank() && !name.startsWith("document:") && !name.startsWith("raw:") && !name.all { it.isDigit() }) {
                        resolvedName = name.substringAfterLast('/')
                    }
                }
            }
        }
    } catch (_: Exception) {}

    // Method 2: Query all metadata columns if DISPLAY_NAME was not found or was numeric
    if (resolvedName == null) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val candidateColumns = listOf(
                        OpenableColumns.DISPLAY_NAME,
                        "_display_name",
                        "title",
                        "_data"
                    )
                    for (col in candidateColumns) {
                        val idx = cursor.getColumnIndex(col)
                        if (idx >= 0) {
                            val str = cursor.getString(idx)
                            if (!str.isNullOrBlank() && !str.startsWith("document:") && !str.startsWith("raw:") && !str.all { it.isDigit() }) {
                                resolvedName = str.substringAfterLast('/')
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Method 3: DocumentContract / MediaStore check if document URI
    if (resolvedName == null && DocumentsContract.isDocumentUri(context, uri)) {
        try {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("raw:") || docId.startsWith("primary:")) {
                val part = docId.substringAfter(':').substringAfterLast('/')
                if (part.isNotEmpty() && !part.all { it.isDigit() }) {
                    resolvedName = part
                }
            } else {
                val numericId = docId.substringAfterLast(':').toLongOrNull()
                if (numericId != null) {
                    val mediaUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), numericId)
                    context.contentResolver.query(mediaUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) {
                                val name = cursor.getString(idx)
                                if (!name.isNullOrBlank() && !name.startsWith("document:") && !name.all { it.isDigit() }) {
                                    resolvedName = name
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Method 4: URI lastPathSegment if valid name and not pure digits
    if (resolvedName == null) {
        val segment = uri.lastPathSegment ?: ""
        if (segment.isNotEmpty() && !segment.startsWith("document:") && !segment.all { it.isDigit() }) {
            val clean = segment.substringAfterLast('/')
            if (clean.isNotEmpty() && !clean.all { it.isDigit() }) {
                resolvedName = clean
            }
        }
    }

    // Method 5: Fallback to user's suggested name or active document title
    if (resolvedName == null || resolvedName.startsWith("document:") || resolvedName.all { it.isDigit() } || resolvedName == "document.txt") {
        resolvedName = suggestedName.ifBlank { fallbackTitle }.ifBlank { "document.txt" }
    }

    var finalName = resolvedName
    if (finalName.startsWith("document:")) {
        finalName = finalName.removePrefix("document:").trim()
    }
    if (finalName.isEmpty() || finalName.all { it.isDigit() }) {
        finalName = suggestedName.ifBlank { fallbackTitle }.ifBlank { "document.txt" }
    }

    return finalName
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val theme = uiState.theme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Active document title
    val activeDoc = uiState.documents.firstOrNull { it.id == uiState.activeDocumentId }
    val activeTitle = activeDoc?.title ?: "NoteCode++"
    var showAboutDialog by remember { mutableStateOf(false) }

    // Storage Access Framework: Open File Launcher
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: ""
                    }
                    val fileName = withContext(Dispatchers.IO) {
                        extractFileNameFromUri(context, uri, content)
                    }
                    viewModel.openFileFromSystem(fileName, content)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error opening file: ${e.localizedMessage}")
                }
            }
        }
    }

    var pendingSaveAsSuggestedName by remember { mutableStateOf("") }

    // Storage Access Framework: Export / Save As Launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(uiState.editorValue.text)
                            }
                        }
                    }
                    val fileName = withContext(Dispatchers.IO) {
                        extractSavedFileName(context, uri, pendingSaveAsSuggestedName, activeTitle)
                    }
                    viewModel.saveActiveDocumentAs(fileName)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // Calculate line & col
    val currentLine = remember(uiState.editorValue.text, uiState.editorValue.selection) {
        viewModel.getCurrentLineNumber()
    }
    val currentCol = remember(uiState.editorValue.text, uiState.editorValue.selection) {
        viewModel.getCurrentColNumber()
    }
    val totalLines = remember(uiState.editorValue.text) {
        val count = uiState.editorValue.text.count { it == '\n' } + 1
        if (uiState.editorValue.text.isEmpty()) 1 else count
    }
    val selectionLength = remember(uiState.editorValue.selection) {
        (uiState.editorValue.selection.max - uiState.editorValue.selection.min).coerceAtLeast(0)
    }

    // Show info messages in snackbar
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = theme.gutterBackground,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Logo Badge with Original Logo & >=48dp tap target
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { showAboutDialog = true }
                            .testTag("app_logo_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.notepadpp_icon_1789914595879),
                            contentDescription = "NoteCode++ Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NoteCode++",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        // Active doc subtitle badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isModified) Color(0xFFFF5252) else theme.bookmarkColor,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.isModified) {
                                Text(
                                    text = " *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }

                    // Quick Header actions with >=44-48dp touch targets
                    IconButton(
                        onClick = { viewModel.createNewDocument() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("header_new_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New File",
                            tint = theme.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.saveActiveDocument() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("header_save_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save File",
                            tint = if (uiState.isModified) Color(0xFF4CAF50) else theme.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFindBar(replaceMode = false) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("header_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = theme.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                EditorStatusBar(
                    language = uiState.activeLanguage,
                    currentLine = currentLine,
                    currentCol = currentCol,
                    selectionLength = selectionLength,
                    totalLength = uiState.editorValue.text.length,
                    totalLines = totalLines,
                    lineEnding = uiState.activeLineEnding,
                    encoding = uiState.activeEncoding,
                    fontSizeSp = uiState.fontSizeSp,
                    isReadOnly = uiState.isReadOnly,
                    theme = theme,
                    onLanguageClick = { viewModel.setShowLanguageDialog(true) },
                    onLineEndingClick = { viewModel.toggleLineEnding() },
                    onEncodingClick = { viewModel.toggleEncoding() },
                    onZoomClick = { viewModel.resetFontSize() },
                    onReadOnlyClick = { viewModel.toggleReadOnly() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Tab Bar
            EditorTabBar(
                documents = uiState.documents,
                activeDocumentId = uiState.activeDocumentId,
                isCurrentModified = uiState.isModified,
                theme = theme,
                onTabSelected = { viewModel.selectDocument(it) },
                onTabClosed = { viewModel.closeDocument(it) },
                onNewTab = { viewModel.createNewDocument() },
                onTabLongClick = { viewModel.setShowRenameDialog(true) }
            )

            // 2. Toolbar
            EditorToolbar(
                isModified = uiState.isModified,
                wordWrap = uiState.wordWrap,
                showWhitespace = uiState.showWhitespace,
                hasBookmarks = uiState.bookmarks.isNotEmpty(),
                theme = theme,
                onSave = { viewModel.saveActiveDocument() },
                onExportFile = {
                    pendingSaveAsSuggestedName = activeTitle
                    exportFileLauncher.launch(activeTitle)
                },
                onOpenFile = { openFileLauncher.launch(arrayOf("*/*", "text/*")) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleFind = { viewModel.toggleFindBar(replaceMode = false) },
                onToggleReplace = { viewModel.toggleFindBar(replaceMode = true) },
                onToggleBookmark = { viewModel.toggleBookmark(currentLine) },
                onNextBookmark = { viewModel.nextBookmark() },
                onPrevBookmark = { viewModel.prevBookmark() },
                onClearBookmarks = { viewModel.clearAllBookmarks() },
                onIndent = { viewModel.indent() },
                onUnindent = { viewModel.unindent() },
                onDuplicateLine = { viewModel.duplicateLine() },
                onDeleteLine = { viewModel.deleteLine() },
                onToggleComment = { viewModel.toggleComment() },
                onInsertTimestamp = { viewModel.insertTimestamp() },
                onChangeCase = { viewModel.changeCase(it) },
                onToggleWordWrap = { viewModel.toggleWordWrap() },
                onToggleWhitespace = { viewModel.toggleWhitespace() },
                onShowLineTools = { viewModel.setShowLineToolsDialog(true) },
                onShowSupport = { viewModel.setShowSupportDialog(true) },
                onZoomIn = { viewModel.increaseFontSize() },
                onZoomOut = { viewModel.decreaseFontSize() },
                onPreview = { viewModel.setShowPreviewDialog(true) },
                onShowStats = { viewModel.setShowStatsDialog(true) },
                onSelectTheme = { viewModel.setShowThemeDialog(true) },
                onGoToLine = { viewModel.setShowGoToLineDialog(true) },
                onRename = { viewModel.setShowRenameDialog(true) }
            )

            // 3. Quick Symbol & Snippet Accessory Bar
            SymbolQuickBar(
                theme = theme,
                onInsertSymbol = { viewModel.insertSymbol(it) }
            )

            // 4. Find and Replace Bar (Collapsible)
            AnimatedVisibility(
                visible = uiState.isFindBarVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EditorFindReplaceBar(
                    isReplaceMode = uiState.isReplaceMode,
                    searchQuery = uiState.searchQuery,
                    replaceQuery = uiState.replaceQuery,
                    searchCaseSensitive = uiState.searchCaseSensitive,
                    searchWholeWord = uiState.searchWholeWord,
                    searchRegex = uiState.searchRegex,
                    matchCount = uiState.searchMatches.size,
                    activeMatchIndex = uiState.activeSearchIndex,
                    theme = theme,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                    onToggleCaseSensitive = { viewModel.toggleSearchCaseSensitive() },
                    onToggleWholeWord = { viewModel.toggleSearchWholeWord() },
                    onToggleRegex = { viewModel.toggleSearchRegex() },
                    onFindNext = { viewModel.findNext() },
                    onFindPrevious = { viewModel.findPrevious() },
                    onReplaceCurrent = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onClose = { viewModel.closeFindBar() }
                )
            }

            // 5. Editor Canvas / Loading / Empty State
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(theme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = theme.bookmarkColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Loading workspace...",
                                color = theme.text,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                uiState.documents.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(theme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = theme.gutterText,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Documents Open",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a new document or open a file from storage to begin editing.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = theme.gutterText,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.createNewDocument() },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.bookmarkColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("empty_state_new_doc_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New File", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = { openFileLauncher.launch(arrayOf("*/*", "text/*")) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("empty_state_open_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        tint = theme.text,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open File", color = theme.text, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                else -> {
                    EditorCanvas(
                        editorValue = uiState.editorValue,
                        language = uiState.activeLanguage,
                        theme = theme,
                        bookmarks = uiState.bookmarks,
                        currentLineNumber = currentLine,
                        wordWrap = uiState.wordWrap,
                        showWhitespace = uiState.showWhitespace,
                        fontSizeSp = uiState.fontSizeSp,
                        isReadOnly = uiState.isReadOnly,
                        searchQuery = uiState.searchQuery,
                        activeSearchIndex = uiState.activeSearchIndex,
                        searchCaseSensitive = uiState.searchCaseSensitive,
                        searchWholeWord = uiState.searchWholeWord,
                        searchRegex = uiState.searchRegex,
                        onValueChange = { viewModel.onEditorValueChanged(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onPinchZoom = { viewModel.onPinchZoom(it) },
                        onResetZoom = { viewModel.resetFontSize() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    if (uiState.showLineToolsDialog) {
        LineToolsDialog(
            theme = theme,
            onSortAscending = { viewModel.sortLines(true) },
            onSortDescending = { viewModel.sortLines(false) },
            onRemoveDuplicates = { viewModel.removeDuplicateLines() },
            onTrimWhitespace = { viewModel.trimLeadingTrailingWhitespace() },
            onJoinLines = { viewModel.joinLines() },
            onReverseLines = { viewModel.reverseLines() },
            onDismiss = { viewModel.setShowLineToolsDialog(false) }
        )
    }

    if (uiState.showSupportDialog) {
        SupportTipJarDialog(
            theme = theme,
            onTipSelected = { amount ->
                viewModel.setShowSupportDialog(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Thank you for choosing to support NoteCode++ ($amount)! You are keeping developer tools free.")
                }
            },
            onDismiss = { viewModel.setShowSupportDialog(false) }
        )
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = uiState.activeLanguage,
            theme = theme,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onDismiss = { viewModel.setShowLanguageDialog(false) }
        )
    }

    if (uiState.showStatsDialog) {
        DocumentStatsDialog(
            title = activeTitle,
            content = uiState.editorValue.text,
            language = uiState.activeLanguage,
            encoding = uiState.activeEncoding,
            lineEnding = uiState.activeLineEnding,
            theme = theme,
            onDismiss = { viewModel.setShowStatsDialog(false) }
        )
    }

    if (uiState.showPreviewDialog) {
        PreviewDialog(
            title = activeTitle,
            content = uiState.editorValue.text,
            language = uiState.activeLanguage,
            theme = theme,
            onDismiss = { viewModel.setShowPreviewDialog(false) }
        )
    }

    if (uiState.showRenameDialog) {
        RenameDialog(
            currentTitle = activeTitle,
            onConfirm = { viewModel.renameActiveDocument(it) },
            onDismiss = { viewModel.setShowRenameDialog(false) }
        )
    }

    if (uiState.showGoToLineDialog) {
        GoToLineDialog(
            currentLine = currentLine,
            totalLines = totalLines,
            onGoToLine = { viewModel.goToLine(it) },
            onDismiss = { viewModel.setShowGoToLineDialog(false) }
        )
    }

    if (uiState.showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = theme,
            onSelectTheme = { viewModel.setTheme(it) },
            onDismiss = { viewModel.setShowThemeDialog(false) }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.notepadpp_icon_1789914595879),
                        contentDescription = "NoteCode++ Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("NoteCode++", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("for Android", fontSize = 12.sp, color = Color(0xFF3DDC84), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "A desktop-class text and source code editor engineered specifically for Android devices.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Multi-tab document workspace\n• Syntax highlighting for 12+ languages\n• Android Storage Access Framework (SAF) integration\n• Line operations, bookmarks, and encoding tools\n• 100% offline, privacy-first, zero analytics",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
