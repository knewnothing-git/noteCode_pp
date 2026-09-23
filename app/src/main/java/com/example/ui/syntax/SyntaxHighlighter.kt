package com.example.ui.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.EditorThemeColors
import java.util.regex.Pattern

data class LanguageDefinition(
    val id: String,
    val name: String,
    val extension: String,
    val keywords: Set<String>,
    val types: Set<String> = emptySet(),
    val singleLineComment: String? = null,
    val multiLineCommentStart: String? = null,
    val multiLineCommentEnd: String? = null,
    val isMarkup: Boolean = false
)

object SupportedLanguages {
    val TEXT = LanguageDefinition(
        id = "text",
        name = "Normal Text",
        extension = "txt",
        keywords = emptySet()
    )

    val PYTHON = LanguageDefinition(
        id = "python",
        name = "Python",
        extension = "py",
        keywords = setOf(
            "def", "class", "return", "if", "elif", "else", "while", "for", "in",
            "import", "from", "as", "try", "except", "finally", "with", "lambda",
            "pass", "break", "continue", "True", "False", "None", "async", "await",
            "yield", "global", "nonlocal", "assert", "del", "raise", "not", "and", "or", "is"
        ),
        types = setOf("int", "str", "float", "bool", "list", "dict", "set", "tuple", "bytes"),
        singleLineComment = "#"
    )

    val JAVASCRIPT = LanguageDefinition(
        id = "javascript",
        name = "JavaScript",
        extension = "js",
        keywords = setOf(
            "function", "const", "let", "var", "return", "if", "else", "for",
            "while", "switch", "case", "default", "break", "continue", "import",
            "export", "from", "class", "extends", "new", "this", "super",
            "async", "await", "try", "catch", "finally", "throw", "typeof",
            "instanceof", "in", "of", "true", "false", "null", "undefined", "void"
        ),
        types = setOf("Array", "Object", "String", "Number", "Boolean", "Promise", "Map", "Set"),
        singleLineComment = "//",
        multiLineCommentStart = "/*",
        multiLineCommentEnd = "*/"
    )

    val KOTLIN = LanguageDefinition(
        id = "kotlin",
        name = "Kotlin / Java",
        extension = "kt",
        keywords = setOf(
            "package", "import", "fun", "val", "var", "class", "interface", "object",
            "companion", "data", "sealed", "enum", "override", "open", "private",
            "protected", "public", "internal", "return", "if", "else", "when",
            "for", "while", "do", "try", "catch", "finally", "throw", "this", "super",
            "is", "as", "in", "true", "false", "null", "suspend", "typealias"
        ),
        types = setOf("Int", "String", "Boolean", "Long", "Float", "Double", "List", "Map", "Set", "Unit", "Any"),
        singleLineComment = "//",
        multiLineCommentStart = "/*",
        multiLineCommentEnd = "*/"
    )

    val HTML = LanguageDefinition(
        id = "html",
        name = "HTML / XML",
        extension = "html",
        keywords = emptySet(),
        singleLineComment = null,
        multiLineCommentStart = "<!--",
        multiLineCommentEnd = "-->",
        isMarkup = true
    )

    val JSON = LanguageDefinition(
        id = "json",
        name = "JSON",
        extension = "json",
        keywords = setOf("true", "false", "null")
    )

    val MARKDOWN = LanguageDefinition(
        id = "markdown",
        name = "Markdown",
        extension = "md",
        keywords = emptySet()
    )

    val SQL = LanguageDefinition(
        id = "sql",
        name = "SQL",
        extension = "sql",
        keywords = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE", "DROP", "ALTER", "JOIN", "INNER", "LEFT",
            "RIGHT", "OUTER", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT",
            "AND", "OR", "NOT", "IN", "AS", "DISTINCT", "UNION", "ALL", "EXISTS",
            "select", "from", "where", "insert", "into", "values", "update", "set",
            "delete", "join", "inner", "left", "right", "on", "group", "by", "order"
        ),
        types = setOf("INT", "VARCHAR", "TEXT", "BOOLEAN", "DATETIME", "FLOAT", "PRIMARY", "KEY"),
        singleLineComment = "--"
    )

    val CPP = LanguageDefinition(
        id = "cpp",
        name = "C / C++",
        extension = "cpp",
        keywords = setOf(
            "#include", "#define", "#ifdef", "#ifndef", "#endif", "int", "char", "float",
            "double", "void", "bool", "long", "short", "struct", "class", "public",
            "private", "protected", "virtual", "override", "return", "if", "else",
            "while", "for", "switch", "case", "default", "break", "continue",
            "new", "delete", "sizeof", "typedef", "const", "static", "namespace", "using"
        ),
        types = setOf("string", "vector", "map", "set", "unique_ptr", "shared_ptr", "size_t"),
        singleLineComment = "//",
        multiLineCommentStart = "/*",
        multiLineCommentEnd = "*/"
    )

    val ALL = listOf(TEXT, PYTHON, JAVASCRIPT, KOTLIN, HTML, JSON, MARKDOWN, SQL, CPP)

    fun findById(id: String): LanguageDefinition {
        return ALL.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TEXT
    }

    fun inferFromFilename(filename: String): LanguageDefinition {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "py" -> PYTHON
            "js", "jsx", "ts", "tsx" -> JAVASCRIPT
            "kt", "kts", "java" -> KOTLIN
            "html", "htm", "xml", "svg" -> HTML
            "json" -> JSON
            "md", "markdown" -> MARKDOWN
            "sql" -> SQL
            "c", "cpp", "h", "hpp" -> CPP
            else -> TEXT
        }
    }
}

object SyntaxHighlighter {

    private val numberRegex = Regex("""\b\d+(\.\d+)?([eE][+-]?\d+)?\b""")
    private val stringDoubleRegex = Regex(""""([^"\\]*(\\.[^"\\]*)*)"""")
    private val stringSingleRegex = Regex("""'([^'\\]*(\\.[^'\\]*)*)'""")
    private val stringBacktickRegex = Regex("""`([^`\\]*(\\.[^`\\]*)*)`""")
    private val htmlTagRegex = Regex("""</?([a-zA-Z0-9\-]+)(\s+[^>]*)?/?>""")
    private val htmlAttrRegex = Regex("""\b([a-zA-Z\-:]+)\s*=\s*("[^"]*"|'[^']*')""")
    private val wordRegex = Regex("""\b[a-zA-Z_][a-zA-Z0-9_]*\b""")

    fun highlight(
        text: String,
        language: LanguageDefinition,
        colors: EditorThemeColors,
        searchQuery: String = "",
        activeSearchIndex: Int = -1,
        searchCaseSensitive: Boolean = false,
        searchWholeWord: Boolean = false,
        searchRegex: Boolean = false,
        showWhitespace: Boolean = false
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        if (text.isEmpty()) return builder.toAnnotatedString()

        // Apply syntax styles based on language
        when {
            language.isMarkup -> highlightMarkup(text, builder, colors)
            language.id == "markdown" -> highlightMarkdown(text, builder, colors)
            language.id == "json" -> highlightJson(text, builder, colors)
            else -> highlightCode(text, builder, language, colors)
        }

        // Apply whitespace indicators (space as ·, tab as →) if enabled
        if (showWhitespace) {
            val whitespaceColor = colors.gutterText.copy(alpha = 0.5f)
            for (i in text.indices) {
                val c = text[i]
                if (c == ' ' || c == '\t') {
                    builder.addStyle(
                        SpanStyle(color = whitespaceColor, fontWeight = FontWeight.Bold),
                        i,
                        i + 1
                    )
                }
            }
        }

        // Apply search highlights if search query is present
        if (searchQuery.isNotEmpty()) {
            highlightSearch(
                text = text,
                builder = builder,
                colors = colors,
                query = searchQuery,
                activeSearchIndex = activeSearchIndex,
                caseSensitive = searchCaseSensitive,
                wholeWord = searchWholeWord,
                isRegex = searchRegex
            )
        }

        return builder.toAnnotatedString()
    }

    private fun highlightCode(
        text: String,
        builder: AnnotatedString.Builder,
        language: LanguageDefinition,
        colors: EditorThemeColors
    ) {
        // 1. Numbers
        numberRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1)
        }

        // 2. Words (Keywords, Types, Functions)
        wordRegex.findAll(text).forEach { match ->
            val word = match.value
            val start = match.range.first
            val end = match.range.last + 1

            if (language.keywords.contains(word)) {
                builder.addStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold), start, end)
            } else if (language.types.contains(word)) {
                builder.addStyle(SpanStyle(color = colors.type, fontWeight = FontWeight.SemiBold), start, end)
            } else {
                // Check if followed by opening parenthesis (function call or declaration)
                var nextCharIdx = end
                while (nextCharIdx < text.length && text[nextCharIdx].isWhitespace()) {
                    nextCharIdx++
                }
                if (nextCharIdx < text.length && text[nextCharIdx] == '(') {
                    builder.addStyle(SpanStyle(color = colors.function), start, end)
                }
            }
        }

        // 3. Strings (double quote, single quote, template literals)
        stringDoubleRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
        }
        stringSingleRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
        }
        if (language.id == "javascript") {
            stringBacktickRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
            }
        }

        // 4. Single-line comments
        language.singleLineComment?.let { commentPrefix ->
            val lines = text.split("\n")
            var lineOffset = 0
            for (line in lines) {
                val commentIdx = line.indexOf(commentPrefix)
                if (commentIdx != -1) {
                    val start = lineOffset + commentIdx
                    val end = lineOffset + line.length
                    builder.addStyle(
                        SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                }
                lineOffset += line.length + 1
            }
        }

        // 5. Multi-line comments
        if (language.multiLineCommentStart != null && language.multiLineCommentEnd != null) {
            val startToken = language.multiLineCommentStart
            val endToken = language.multiLineCommentEnd
            var searchStart = 0
            while (searchStart < text.length) {
                val start = text.indexOf(startToken, searchStart)
                if (start == -1) break
                val end = text.indexOf(endToken, start + startToken.length)
                val blockEnd = if (end == -1) text.length else end + endToken.length
                builder.addStyle(
                    SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                    start,
                    blockEnd
                )
                searchStart = blockEnd
            }
        }
    }

    private fun highlightMarkup(text: String, builder: AnnotatedString.Builder, colors: EditorThemeColors) {
        // HTML Tags and Attributes
        htmlTagRegex.findAll(text).forEach { match ->
            val fullTag = match.value
            val start = match.range.first
            val end = match.range.last + 1

            // Base tag color
            builder.addStyle(SpanStyle(color = colors.tag, fontWeight = FontWeight.SemiBold), start, end)

            // Attributes inside the tag
            htmlAttrRegex.findAll(fullTag).forEach { attrMatch ->
                val attrName = attrMatch.groupValues[1]
                val attrVal = attrMatch.groupValues[2]

                val attrStart = start + attrMatch.range.first
                val nameEnd = attrStart + attrName.length
                builder.addStyle(SpanStyle(color = colors.attribute), attrStart, nameEnd)

                val valStart = attrStart + attrMatch.value.indexOf(attrVal)
                val valEnd = valStart + attrVal.length
                builder.addStyle(SpanStyle(color = colors.string), valStart, valEnd)
            }
        }

        // HTML comments: <!-- ... -->
        var searchStart = 0
        while (searchStart < text.length) {
            val start = text.indexOf("<!--", searchStart)
            if (start == -1) break
            val end = text.indexOf("-->", start + 4)
            val blockEnd = if (end == -1) text.length else end + 3
            builder.addStyle(
                SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic),
                start,
                blockEnd
            )
            searchStart = blockEnd
        }
    }

    private fun highlightJson(text: String, builder: AnnotatedString.Builder, colors: EditorThemeColors) {
        // String properties (keys vs values)
        stringDoubleRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1

            // Check if followed by ':' (JSON key)
            var idx = end
            while (idx < text.length && text[idx].isWhitespace()) {
                idx++
            }
            if (idx < text.length && text[idx] == ':') {
                builder.addStyle(SpanStyle(color = colors.tag, fontWeight = FontWeight.SemiBold), start, end)
            } else {
                builder.addStyle(SpanStyle(color = colors.string), start, end)
            }
        }

        // Numbers
        numberRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1)
        }

        // Booleans & null
        listOf("true", "false", "null").forEach { keyword ->
            val regex = Regex("""\b$keyword\b""")
            regex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
        }
    }

    private fun highlightMarkdown(text: String, builder: AnnotatedString.Builder, colors: EditorThemeColors) {
        val lines = text.split("\n")
        var offset = 0
        for (line in lines) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("#")) {
                // Headings
                val headerLevel = trimmed.takeWhile { it == '#' }.length
                if (headerLevel in 1..6) {
                    val weight = when (headerLevel) {
                        1 -> FontWeight.ExtraBold
                        2 -> FontWeight.Bold
                        else -> FontWeight.SemiBold
                    }
                    builder.addStyle(SpanStyle(color = colors.keyword, fontWeight = weight), offset, offset + line.length)
                }
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("> ")) {
                builder.addStyle(SpanStyle(color = colors.operator), offset, offset + (line.length - trimmed.length + 2))
            }
            offset += line.length + 1
        }

        // Inline code `code`
        val inlineCodeRegex = Regex("""`[^`\n]+`""")
        inlineCodeRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = colors.number, background = colors.surface), match.range.first, match.range.last + 1)
        }

        // Bold **text**
        val boldRegex = Regex("""\*\*([^*]+)\*\*""")
        boldRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.text), match.range.first, match.range.last + 1)
        }

        // Italic *text*
        val italicRegex = Regex("""\*([^*]+)\*""")
        italicRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
    }

    private fun highlightSearch(
        text: String,
        builder: AnnotatedString.Builder,
        colors: EditorThemeColors,
        query: String,
        activeSearchIndex: Int,
        caseSensitive: Boolean,
        wholeWord: Boolean,
        isRegex: Boolean
    ) {
        try {
            val patternString = if (isRegex) {
                query
            } else {
                val escaped = Pattern.quote(query)
                if (wholeWord) "\\b$escaped\\b" else escaped
            }

            val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            val matcher = Pattern.compile(patternString, flags).matcher(text)

            var matchIdx = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val isCurrent = (matchIdx == activeSearchIndex)

                val bgColor = if (isCurrent) colors.activeSearchHighlightColor else colors.searchHighlightColor
                builder.addStyle(
                    SpanStyle(
                        background = bgColor,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    start,
                    end
                )
                matchIdx++
            }
        } catch (_: Exception) {
            // In case of invalid regex while typing
        }
    }
}
