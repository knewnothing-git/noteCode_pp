package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.ui.syntax.LanguageDefinition
import com.example.ui.syntax.SupportedLanguages
import com.example.ui.theme.AvailableEditorThemes
import com.example.ui.theme.EditorThemeColors
import com.example.ui.theme.ObsidianDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

enum class CaseType {
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
    CAMEL_CASE,
    SNAKE_CASE
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentEntity> = emptyList(),
    val activeDocumentId: Long? = null,
    val editorValue: TextFieldValue = TextFieldValue(""),
    val isModified: Boolean = false,
    val activeLanguage: LanguageDefinition = SupportedLanguages.TEXT,
    val activeEncoding: String = "UTF-8",
    val activeLineEnding: String = "LF",
    val bookmarks: Set<Int> = emptySet(),
    val wordWrap: Boolean = false,
    val showWhitespace: Boolean = false,
    val fontSizeSp: Float = 14f,
    val theme: EditorThemeColors = ObsidianDarkTheme,
    val isReadOnly: Boolean = false,
    // Find & Replace
    val isFindBarVisible: Boolean = false,
    val isReplaceMode: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchCaseSensitive: Boolean = false,
    val searchWholeWord: Boolean = false,
    val searchRegex: Boolean = false,
    val searchMatches: List<IntRange> = emptyList(),
    val activeSearchIndex: Int = -1,
    // Dialogs
    val showLanguageDialog: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showPreviewDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showGoToLineDialog: Boolean = false,
    val showLineToolsDialog: Boolean = false,
    val showSupportDialog: Boolean = false,
    val infoMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Undo / Redo history per document ID
    private val undoStacks = mutableMapOf<Long, MutableList<String>>()
    private val redoStacks = mutableMapOf<Long, MutableList<String>>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao())

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.allDocuments.collect { docList ->
                _uiState.update { current ->
                    val activeId = if (current.activeDocumentId != null && docList.any { it.id == current.activeDocumentId }) {
                        current.activeDocumentId
                    } else {
                        docList.firstOrNull()?.id
                    }

                    val activeDoc = docList.firstOrNull { it.id == activeId }
                    if (activeDoc != null && (current.activeDocumentId != activeId || current.documents.isEmpty())) {
                        val lang = SupportedLanguages.findById(activeDoc.language)
                        val bookmarksSet = parseBookmarks(activeDoc.bookmarks)
                        current.copy(
                            isLoading = false,
                            documents = docList,
                            activeDocumentId = activeId,
                            editorValue = TextFieldValue(activeDoc.content, TextRange(activeDoc.cursorPosition.coerceIn(0, activeDoc.content.length))),
                            activeLanguage = lang,
                            activeEncoding = activeDoc.encoding,
                            activeLineEnding = activeDoc.lineEnding,
                            bookmarks = bookmarksSet,
                            isModified = false
                        )
                    } else {
                        current.copy(isLoading = false, documents = docList, activeDocumentId = activeId)
                    }
                }
            }
        }
    }

    private fun parseBookmarks(raw: String): Set<Int> {
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun serializeBookmarks(set: Set<Int>): String {
        return set.sorted().joinToString(",")
    }

    fun onEditorValueChanged(newValue: TextFieldValue) {
        val activeId = _uiState.value.activeDocumentId ?: return
        val oldValue = _uiState.value.editorValue

        if (oldValue.text != newValue.text) {
            // Push to undo stack
            val undo = undoStacks.getOrPut(activeId) { mutableListOf() }
            if (undo.size > 100) undo.removeAt(0)
            undo.add(oldValue.text)
            redoStacks[activeId]?.clear()
        }

        _uiState.update {
            it.copy(
                editorValue = newValue,
                isModified = if (oldValue.text != newValue.text) true else it.isModified
            )
        }

        if (_uiState.value.searchQuery.isNotEmpty()) {
            refreshSearchMatches(newValue.text)
        }
    }

    fun selectDocument(docId: Long) {
        val currentActiveId = _uiState.value.activeDocumentId
        val currentDocs = _uiState.value.documents
        val updatedDocs = if (currentActiveId != null && _uiState.value.isModified) {
            currentDocs.map { doc ->
                if (doc.id == currentActiveId) {
                    doc.copy(
                        content = _uiState.value.editorValue.text,
                        cursorPosition = _uiState.value.editorValue.selection.start,
                        isModified = true
                    )
                } else doc
            }
        } else {
            currentDocs
        }

        val doc = updatedDocs.firstOrNull { it.id == docId } ?: return
        val lang = SupportedLanguages.findById(doc.language)
        val bms = parseBookmarks(doc.bookmarks)

        _uiState.update {
            it.copy(
                documents = updatedDocs,
                activeDocumentId = docId,
                editorValue = TextFieldValue(doc.content, TextRange(doc.cursorPosition.coerceIn(0, doc.content.length))),
                activeLanguage = lang,
                activeEncoding = doc.encoding,
                activeLineEnding = doc.lineEnding,
                bookmarks = bms,
                isModified = doc.isModified,
                isFindBarVisible = false,
                searchQuery = "",
                searchMatches = emptyList(),
                activeSearchIndex = -1
            )
        }
    }

    fun createNewDocument(titlePrefix: String = "new") {
        viewModelScope.launch {
            val count = _uiState.value.documents.size + 1
            val title = "$titlePrefix $count"
            val newDoc = DocumentEntity(
                title = title,
                content = "",
                language = "text",
                encoding = "UTF-8",
                lineEnding = "LF",
                orderIndex = count
            )
            val newId = repository.insert(newDoc)
            val fullDoc = newDoc.copy(id = newId)
            val updatedDocs = _uiState.value.documents.filter { it.id != newId } + fullDoc
            _uiState.update {
                it.copy(
                    documents = updatedDocs,
                    activeDocumentId = newId,
                    editorValue = TextFieldValue(""),
                    activeLanguage = SupportedLanguages.findById("text"),
                    activeEncoding = "UTF-8",
                    activeLineEnding = "LF",
                    bookmarks = emptySet(),
                    isModified = false,
                    isFindBarVisible = false,
                    searchQuery = "",
                    searchMatches = emptyList(),
                    activeSearchIndex = -1
                )
            }
            showFeedback("Created $title")
        }
    }

    fun closeDocument(docId: Long) {
        viewModelScope.launch {
            val docs = _uiState.value.documents
            if (docs.size <= 1) {
                // Don't close the last one, just clear it or make a new one
                val only = docs.first()
                val updated = only.copy(title = "new 1", content = "", isModified = false, bookmarks = "")
                repository.update(updated)
                _uiState.update {
                    it.copy(
                        editorValue = TextFieldValue(""),
                        isModified = false,
                        bookmarks = emptySet()
                    )
                }
                return@launch
            }

            repository.deleteById(docId)
            undoStacks.remove(docId)
            redoStacks.remove(docId)

            val remaining = docs.filter { it.id != docId }
            val nextDoc = remaining.firstOrNull()
            if (nextDoc != null) {
                selectDocument(nextDoc.id)
            }
        }
    }

    fun saveActiveDocument() {
        val activeId = _uiState.value.activeDocumentId ?: return
        val currentDoc = _uiState.value.documents.firstOrNull { it.id == activeId } ?: return
        viewModelScope.launch {
            val updated = currentDoc.copy(
                content = _uiState.value.editorValue.text,
                language = _uiState.value.activeLanguage.id,
                encoding = _uiState.value.activeEncoding,
                lineEnding = _uiState.value.activeLineEnding,
                bookmarks = serializeBookmarks(_uiState.value.bookmarks),
                cursorPosition = _uiState.value.editorValue.selection.start,
                isModified = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updated)
            _uiState.update { it.copy(isModified = false) }
            showFeedback("Saved ${currentDoc.title}")
        }
    }

    fun saveAllDocuments() {
        viewModelScope.launch {
            saveActiveDocument()
            showFeedback("All documents saved")
        }
    }

    fun renameActiveDocument(newTitle: String) {
        val activeId = _uiState.value.activeDocumentId ?: return
        val currentDoc = _uiState.value.documents.firstOrNull { it.id == activeId } ?: return
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) return

        val detectedLang = SupportedLanguages.inferFromFilename(cleanTitle)

        viewModelScope.launch {
            val updated = currentDoc.copy(
                title = cleanTitle,
                language = detectedLang.id
            )
            repository.update(updated)
            _uiState.update { current ->
                val updatedDocs = current.documents.map { if (it.id == activeId) updated else it }
                current.copy(
                    documents = updatedDocs,
                    activeLanguage = detectedLang,
                    showRenameDialog = false
                )
            }
            showFeedback("Renamed to $cleanTitle")
        }
    }

    fun saveActiveDocumentAs(newFileName: String) {
        val activeId = _uiState.value.activeDocumentId ?: return
        val currentDoc = _uiState.value.documents.firstOrNull { it.id == activeId } ?: return
        val cleanTitle = newFileName.trim().ifEmpty { currentDoc.title }
        val detectedLang = SupportedLanguages.inferFromFilename(cleanTitle)
        val contentText = _uiState.value.editorValue.text

        viewModelScope.launch {
            val updated = currentDoc.copy(
                title = cleanTitle,
                content = contentText,
                language = detectedLang.id,
                encoding = _uiState.value.activeEncoding,
                lineEnding = _uiState.value.activeLineEnding,
                bookmarks = serializeBookmarks(_uiState.value.bookmarks),
                cursorPosition = _uiState.value.editorValue.selection.start,
                isModified = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updated)
            _uiState.update { current ->
                val updatedDocs = current.documents.map { if (it.id == activeId) updated else it }
                current.copy(
                    documents = updatedDocs,
                    activeLanguage = detectedLang,
                    isModified = false
                )
            }
            showFeedback("Saved as $cleanTitle")
        }
    }

    // --- Undo & Redo ---
    fun undo() {
        val activeId = _uiState.value.activeDocumentId ?: return
        val stack = undoStacks[activeId] ?: return
        if (stack.isEmpty()) return

        val currentText = _uiState.value.editorValue.text
        val previousText = stack.removeAt(stack.lastIndex)

        val redo = redoStacks.getOrPut(activeId) { mutableListOf() }
        redo.add(currentText)

        _uiState.update {
            it.copy(
                editorValue = TextFieldValue(previousText, TextRange(previousText.length.coerceAtMost(it.editorValue.selection.start))),
                isModified = true
            )
        }
    }

    fun redo() {
        val activeId = _uiState.value.activeDocumentId ?: return
        val stack = redoStacks[activeId] ?: return
        if (stack.isEmpty()) return

        val currentText = _uiState.value.editorValue.text
        val nextText = stack.removeAt(stack.lastIndex)

        val undo = undoStacks.getOrPut(activeId) { mutableListOf() }
        undo.add(currentText)

        _uiState.update {
            it.copy(
                editorValue = TextFieldValue(nextText, TextRange(nextText.length.coerceAtMost(it.editorValue.selection.start))),
                isModified = true
            )
        }
    }

    // --- Developer Utilities ---

    fun insertTimestamp() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = format.format(Date())
        insertTextAtCursor(timestamp)
    }

    fun duplicateLine() {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection.start
        val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', sel).let { if (it == -1) text.length else it }
        val lineContent = text.substring(lineStart, lineEnd)

        val newText = text.substring(0, lineEnd) + "\n" + lineContent + text.substring(lineEnd)
        onEditorValueChanged(TextFieldValue(newText, TextRange(lineEnd + 1 + lineContent.length)))
        showFeedback("Line duplicated")
    }

    fun deleteLine() {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val sel = _uiState.value.editorValue.selection.start
        val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', sel).let { if (it == -1) text.length else it + 1 }

        val newText = text.substring(0, lineStart) + text.substring(lineEnd)
        onEditorValueChanged(TextFieldValue(newText, TextRange(lineStart.coerceAtMost(newText.length))))
        showFeedback("Line deleted")
    }

    fun toggleComment() {
        val prefix = _uiState.value.activeLanguage.singleLineComment ?: "//"
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection.start
        val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', sel).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val updatedLine = if (currentLine.trimStart().startsWith(prefix)) {
            // Remove comment
            val idx = currentLine.indexOf(prefix)
            currentLine.substring(0, idx) + currentLine.substring(idx + prefix.length).removePrefix(" ")
        } else {
            // Add comment
            "$prefix $currentLine"
        }

        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
        onEditorValueChanged(TextFieldValue(newText, TextRange(lineStart + updatedLine.length)))
    }

    fun indent() {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection
        if (sel.collapsed) {
            insertTextAtCursor("    ")
        } else {
            // Indent selected lines
            val start = sel.min
            val end = sel.max
            val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }
            val block = text.substring(lineStart, lineEnd)
            val indented = block.split("\n").joinToString("\n") { "    $it" }
            val newText = text.substring(0, lineStart) + indented + text.substring(lineEnd)
            onEditorValueChanged(TextFieldValue(newText, TextRange(lineStart, lineStart + indented.length)))
        }
    }

    fun unindent() {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection
        val start = sel.min
        val end = sel.max
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }
        val block = text.substring(lineStart, lineEnd)
        val unindented = block.split("\n").joinToString("\n") { line ->
            when {
                line.startsWith("    ") -> line.substring(4)
                line.startsWith("\t") -> line.substring(1)
                else -> line.trimStart { it == ' ' }.let { trimmed ->
                    val removed = (line.length - trimmed.length).coerceAtMost(4)
                    line.substring(removed)
                }
            }
        }
        val newText = text.substring(0, lineStart) + unindented + text.substring(lineEnd)
        onEditorValueChanged(TextFieldValue(newText, TextRange(lineStart, lineStart + unindented.length)))
    }

    fun changeCase(caseType: CaseType) {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection
        val (start, end) = if (sel.collapsed) {
            // Select current word
            val wordStart = text.take(sel.start).lastIndexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ':', ';')).let { if (it == -1) 0 else it + 1 }
            val wordEnd = text.indexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ':', ';'), sel.start).let { if (it == -1) text.length else it }
            wordStart to wordEnd
        } else {
            sel.min to sel.max
        }

        if (start >= end) return
        val target = text.substring(start, end)
        val transformed = when (caseType) {
            CaseType.UPPERCASE -> target.uppercase(Locale.getDefault())
            CaseType.LOWERCASE -> target.lowercase(Locale.getDefault())
            CaseType.TITLE_CASE -> target.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            CaseType.CAMEL_CASE -> {
                val words = target.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }
                words.mapIndexed { idx, w ->
                    if (idx == 0) w.lowercase(Locale.getDefault())
                    else w.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                }.joinToString("")
            }
            CaseType.SNAKE_CASE -> {
                target.trim()
                    .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                    .replace(Regex("[\\s\\-]+"), "_")
                    .lowercase(Locale.getDefault())
            }
        }

        val newText = text.substring(0, start) + transformed + text.substring(end)
        onEditorValueChanged(TextFieldValue(newText, TextRange(start, start + transformed.length)))
        showFeedback("Applied ${caseType.name.lowercase().replace('_', ' ')}")
    }

    private fun insertTextAtCursor(insert: String) {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection
        val start = sel.min
        val end = sel.max
        val newText = text.substring(0, start) + insert + text.substring(end)
        val newCursor = start + insert.length
        onEditorValueChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    // --- Bookmarks ---

    fun toggleBookmark(lineNumber: Int) {
        _uiState.update { current ->
            val set = current.bookmarks.toMutableSet()
            if (set.contains(lineNumber)) {
                set.remove(lineNumber)
                showFeedback("Removed bookmark on line $lineNumber")
            } else {
                set.add(lineNumber)
                showFeedback("Added bookmark on line $lineNumber")
            }
            current.copy(bookmarks = set)
        }
    }

    fun nextBookmark() {
        val bms = _uiState.value.bookmarks.sorted()
        if (bms.isEmpty()) {
            showFeedback("No bookmarks in this document")
            return
        }
        val currentLine = getCurrentLineNumber()
        val next = bms.firstOrNull { it > currentLine } ?: bms.first()
        goToLine(next)
    }

    fun prevBookmark() {
        val bms = _uiState.value.bookmarks.sortedDescending()
        if (bms.isEmpty()) {
            showFeedback("No bookmarks in this document")
            return
        }
        val currentLine = getCurrentLineNumber()
        val prev = bms.firstOrNull { it < currentLine } ?: bms.first()
        goToLine(prev)
    }

    fun clearAllBookmarks() {
        _uiState.update { it.copy(bookmarks = emptySet()) }
        showFeedback("Cleared all bookmarks")
    }

    fun getCurrentLineNumber(): Int {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection.start
        var line = 1
        for (i in 0 until sel.coerceAtMost(text.length)) {
            if (text[i] == '\n') line++
        }
        return line
    }

    fun getCurrentColNumber(): Int {
        val text = _uiState.value.editorValue.text
        val sel = _uiState.value.editorValue.selection.start
        val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        return (sel - lineStart) + 1
    }

    fun goToLine(lineNumber: Int) {
        val text = _uiState.value.editorValue.text
        val lines = text.split("\n")
        val targetLine = lineNumber.coerceIn(1, lines.size.coerceAtLeast(1))

        var charOffset = 0
        for (i in 0 until targetLine - 1) {
            charOffset += lines[i].length + 1
        }
        val lineLen = if (targetLine <= lines.size) lines[targetLine - 1].length else 0
        _uiState.update {
            it.copy(
                editorValue = TextFieldValue(text, TextRange(charOffset, charOffset + lineLen)),
                showGoToLineDialog = false
            )
        }
    }

    // --- Find & Replace ---

    fun toggleFindBar(replaceMode: Boolean = false) {
        _uiState.update {
            it.copy(
                isFindBarVisible = !it.isFindBarVisible || it.isReplaceMode != replaceMode,
                isReplaceMode = replaceMode
            )
        }
        if (_uiState.value.isFindBarVisible && _uiState.value.searchQuery.isNotEmpty()) {
            refreshSearchMatches(_uiState.value.editorValue.text)
        }
    }

    fun closeFindBar() {
        _uiState.update {
            it.copy(
                isFindBarVisible = false,
                searchMatches = emptyList(),
                activeSearchIndex = -1
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refreshSearchMatches(_uiState.value.editorValue.text)
    }

    fun setReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleSearchCaseSensitive() {
        _uiState.update { it.copy(searchCaseSensitive = !it.searchCaseSensitive) }
        refreshSearchMatches(_uiState.value.editorValue.text)
    }

    fun toggleSearchWholeWord() {
        _uiState.update { it.copy(searchWholeWord = !it.searchWholeWord) }
        refreshSearchMatches(_uiState.value.editorValue.text)
    }

    fun toggleSearchRegex() {
        _uiState.update { it.copy(searchRegex = !it.searchRegex) }
        refreshSearchMatches(_uiState.value.editorValue.text)
    }

    private fun refreshSearchMatches(text: String) {
        val state = _uiState.value
        val query = state.searchQuery
        if (query.isEmpty() || text.isEmpty()) {
            _uiState.update { it.copy(searchMatches = emptyList(), activeSearchIndex = -1) }
            return
        }

        try {
            val patternString = if (state.searchRegex) {
                query
            } else {
                val escaped = Pattern.quote(query)
                if (state.searchWholeWord) "\\b$escaped\\b" else escaped
            }
            val flags = if (state.searchCaseSensitive) 0 else Pattern.CASE_INSENSITIVE
            val matcher = Pattern.compile(patternString, flags).matcher(text)

            val list = mutableListOf<IntRange>()
            while (matcher.find()) {
                list.add(matcher.start()..matcher.end())
            }

            val currentCursor = state.editorValue.selection.start
            var activeIdx = list.indexOfFirst { it.first >= currentCursor }
            if (activeIdx == -1 && list.isNotEmpty()) activeIdx = 0

            _uiState.update {
                it.copy(
                    searchMatches = list,
                    activeSearchIndex = if (list.isNotEmpty()) activeIdx else -1
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(searchMatches = emptyList(), activeSearchIndex = -1) }
        }
    }

    fun findNext() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        val nextIdx = (_uiState.value.activeSearchIndex + 1) % matches.size
        selectMatch(nextIdx)
    }

    fun findPrevious() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        val prevIdx = if (_uiState.value.activeSearchIndex <= 0) matches.size - 1 else _uiState.value.activeSearchIndex - 1
        selectMatch(prevIdx)
    }

    private fun selectMatch(index: Int) {
        val matches = _uiState.value.searchMatches
        if (index !in matches.indices) return
        val match = matches[index]
        _uiState.update {
            it.copy(
                activeSearchIndex = index,
                editorValue = TextFieldValue(it.editorValue.text, TextRange(match.first, match.last))
            )
        }
    }

    fun replaceCurrent() {
        val matches = _uiState.value.searchMatches
        val activeIdx = _uiState.value.activeSearchIndex
        if (matches.isEmpty() || activeIdx !in matches.indices) return

        val match = matches[activeIdx]
        val text = _uiState.value.editorValue.text
        val replaceText = _uiState.value.replaceQuery

        val newText = text.substring(0, match.first) + replaceText + text.substring(match.last)
        onEditorValueChanged(TextFieldValue(newText, TextRange(match.first + replaceText.length)))
        findNext()
    }

    fun replaceAll() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return

        val state = _uiState.value
        val text = state.editorValue.text
        val query = state.searchQuery
        val replaceText = state.replaceQuery

        try {
            val patternString = if (state.searchRegex) {
                query
            } else {
                val escaped = Pattern.quote(query)
                if (state.searchWholeWord) "\\b$escaped\\b" else escaped
            }
            val flags = if (state.searchCaseSensitive) 0 else Pattern.CASE_INSENSITIVE
            val pattern = Pattern.compile(patternString, flags)
            val newText = pattern.matcher(text).replaceAll(replaceText)

            val count = matches.size
            onEditorValueChanged(TextFieldValue(newText, TextRange(0)))
            showFeedback("Replaced $count occurrences")
        } catch (e: Exception) {
            showFeedback("Replace failed: ${e.message}")
        }
    }

    // --- Settings & Configurations ---

    fun setLanguage(language: LanguageDefinition) {
        _uiState.update { it.copy(activeLanguage = language, showLanguageDialog = false) }
        showFeedback("Language: ${language.name}")
    }

    fun setTheme(theme: EditorThemeColors) {
        _uiState.update { it.copy(theme = theme, showThemeDialog = false) }
    }

    fun toggleWordWrap() {
        _uiState.update {
            val next = !it.wordWrap
            showFeedback(if (next) "Word wrap ON" else "Word wrap OFF")
            it.copy(wordWrap = next)
        }
    }

    fun increaseFontSize() {
        _uiState.update {
            val newSize = (it.fontSizeSp + 2f).coerceAtMost(36f)
            showFeedback("Zoom: ${newSize.toInt()} sp")
            it.copy(fontSizeSp = newSize)
        }
    }

    fun decreaseFontSize() {
        _uiState.update {
            val newSize = (it.fontSizeSp - 2f).coerceAtLeast(8f)
            showFeedback("Zoom: ${newSize.toInt()} sp")
            it.copy(fontSizeSp = newSize)
        }
    }

    fun onPinchZoom(zoomMultiplier: Float) {
        _uiState.update {
            val newSize = (it.fontSizeSp * zoomMultiplier).coerceIn(8f, 36f)
            it.copy(fontSizeSp = newSize)
        }
    }

    fun resetFontSize() {
        _uiState.update {
            val next = if (kotlin.math.abs(it.fontSizeSp - 14f) < 0.3f) 18f else 14f
            showFeedback("Zoom reset: ${next.toInt()} sp")
            it.copy(fontSizeSp = next)
        }
    }

    fun setFontSize(sizeSp: Float) {
        _uiState.update {
            it.copy(fontSizeSp = sizeSp.coerceIn(8f, 36f))
        }
    }

    fun toggleEncoding() {
        _uiState.update {
            val next = if (it.activeEncoding == "UTF-8") "ANSI" else "UTF-8"
            showFeedback("Encoding: $next")
            it.copy(activeEncoding = next)
        }
    }

    fun toggleLineEnding() {
        _uiState.update {
            val next = if (it.activeLineEnding == "LF") "CRLF" else "LF"
            showFeedback("Line ending: $next")
            it.copy(activeLineEnding = next)
        }
    }

    fun toggleReadOnly() {
        _uiState.update {
            val next = !it.isReadOnly
            showFeedback(if (next) "Read-only mode ON" else "Edit mode enabled")
            it.copy(isReadOnly = next)
        }
    }

    fun toggleWhitespace() {
        _uiState.update {
            val next = !it.showWhitespace
            showFeedback(if (next) "Show whitespace ON" else "Show whitespace OFF")
            it.copy(showWhitespace = next)
        }
    }

    // --- Line Tools ---

    fun sortLines(ascending: Boolean) {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val lines = text.split("\n")
        val sorted = if (ascending) {
            lines.sortedWith(String.CASE_INSENSITIVE_ORDER)
        } else {
            lines.sortedWith(String.CASE_INSENSITIVE_ORDER.reversed())
        }
        val newText = sorted.joinToString("\n")
        onEditorValueChanged(TextFieldValue(newText, TextRange(0)))
        showFeedback(if (ascending) "Lines sorted A-Z" else "Lines sorted Z-A")
    }

    fun removeDuplicateLines() {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val lines = text.split("\n")
        val distinct = lines.distinct()
        val removedCount = lines.size - distinct.size
        val newText = distinct.joinToString("\n")
        onEditorValueChanged(TextFieldValue(newText, TextRange(0)))
        showFeedback("Removed $removedCount duplicate line(s)")
    }

    fun trimLeadingTrailingWhitespace() {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val lines = text.split("\n")
        val trimmed = lines.map { it.trim() }
        val newText = trimmed.joinToString("\n")
        onEditorValueChanged(TextFieldValue(newText, TextRange(0)))
        showFeedback("Trimmed whitespace on all lines")
    }

    fun joinLines() {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val lines = text.split("\n")
        val joined = lines.filter { it.isNotBlank() }.joinToString(" ") { it.trim() }
        onEditorValueChanged(TextFieldValue(joined, TextRange(joined.length)))
        showFeedback("Lines joined into single paragraph")
    }

    fun reverseLines() {
        val text = _uiState.value.editorValue.text
        if (text.isEmpty()) return
        val lines = text.split("\n")
        val reversed = lines.reversed().joinToString("\n")
        onEditorValueChanged(TextFieldValue(reversed, TextRange(0)))
        showFeedback("Lines reversed")
    }

    // Quick symbol / snippet insertion
    fun insertSymbol(symbol: String) {
        insertTextAtCursor(symbol)
    }

    // Load file content opened from system picker
    // Preserves existing opened tabs (like desktop Notepad++) while immediately activating and displaying the newly loaded document.
    fun openFileFromSystem(fileName: String, content: String) {
        viewModelScope.launch {
            val lang = SupportedLanguages.inferFromFilename(fileName)
            val lineEnding = if (content.contains("\r\n")) "CRLF" else "LF"
            val currentDocs = _uiState.value.documents

            // If the only open tab is an untouched, empty default document, reuse it
            val shouldReplaceBlankDefault = currentDocs.size == 1 &&
                    currentDocs.first().title.startsWith("new") &&
                    currentDocs.first().content.isEmpty() &&
                    !_uiState.value.isModified

            val (newDoc, updatedDocs) = if (shouldReplaceBlankDefault) {
                val existing = currentDocs.first()
                val updated = existing.copy(
                    title = fileName,
                    content = content,
                    language = lang.id,
                    encoding = "UTF-8",
                    lineEnding = lineEnding,
                    bookmarks = "",
                    cursorPosition = 0
                )
                repository.update(updated)
                updated to listOf(updated)
            } else {
                val count = currentDocs.size + 1
                val created = DocumentEntity(
                    title = fileName,
                    content = content,
                    language = lang.id,
                    encoding = "UTF-8",
                    lineEnding = lineEnding,
                    orderIndex = count
                )
                val newId = repository.insert(created)
                val saved = created.copy(id = newId)
                saved to (currentDocs.filter { it.id != newId } + saved)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    documents = updatedDocs,
                    activeDocumentId = newDoc.id,
                    editorValue = TextFieldValue(content, TextRange(0)),
                    activeLanguage = lang,
                    activeEncoding = "UTF-8",
                    activeLineEnding = lineEnding,
                    bookmarks = emptySet(),
                    isModified = false,
                    isFindBarVisible = false,
                    searchQuery = "",
                    searchMatches = emptyList(),
                    activeSearchIndex = -1
                )
            }
            showFeedback("Opened $fileName")
        }
    }

    // Dialog toggles
    fun setShowLanguageDialog(show: Boolean) = _uiState.update { it.copy(showLanguageDialog = show) }
    fun setShowStatsDialog(show: Boolean) = _uiState.update { it.copy(showStatsDialog = show) }
    fun setShowPreviewDialog(show: Boolean) = _uiState.update { it.copy(showPreviewDialog = show) }
    fun setShowThemeDialog(show: Boolean) = _uiState.update { it.copy(showThemeDialog = show) }
    fun setShowRenameDialog(show: Boolean) = _uiState.update { it.copy(showRenameDialog = show) }
    fun setShowGoToLineDialog(show: Boolean) = _uiState.update { it.copy(showGoToLineDialog = show) }
    fun setShowLineToolsDialog(show: Boolean) = _uiState.update { it.copy(showLineToolsDialog = show) }
    fun setShowSupportDialog(show: Boolean) = _uiState.update { it.copy(showSupportDialog = show) }

    fun clearFeedback() = _uiState.update { it.copy(infoMessage = null) }

    private fun showFeedback(msg: String) {
        _uiState.update { it.copy(infoMessage = msg) }
    }
}
