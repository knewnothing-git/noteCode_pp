package com.example.data.repository

import com.example.data.local.DocumentDao
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: Long): DocumentEntity? = documentDao.getDocumentById(id)

    suspend fun insert(document: DocumentEntity): Long = documentDao.insertDocument(document)

    suspend fun update(document: DocumentEntity) = documentDao.updateDocument(document)

    suspend fun delete(document: DocumentEntity) = documentDao.deleteDocument(document)

    suspend fun deleteById(id: Long) = documentDao.deleteDocumentById(id)

    suspend fun deleteAll() = documentDao.deleteAllDocuments()

    suspend fun seedInitialDataIfEmpty() {
        if (documentDao.getDocumentCount() == 0) {
            val defaultDocs = listOf(
                DocumentEntity(
                    title = "Welcome.txt",
                    language = "text",
                    encoding = "UTF-8",
                    lineEnding = "LF",
                    bookmarks = "7,18",
                    orderIndex = 0,
                    content = """=====================================================
 Welcome to Notepad++ for Android!
 Powerful, fast code & text editor
=====================================================

Key Features:
- Multi-Document Tab bar with unsaved change status
- Syntax Highlighting for 10+ languages (Python, Kotlin, HTML, JS, JSON, SQL, etc.)
- Line Number Gutter with interactive Bookmarking
- Find & Replace with Case, Whole Word, and Regex support
- Quick Developer Tools:
    * Indent / Unindent selection
    * Case conversion (UPPER, lower, Title, camelCase, snake_case)
    * Line duplication & deletion
    * Insert timestamp (F5 style)
    * Word wrap toggle & Monospace font zoom
- Real-time Document Status Bar (Ln, Col, Length, Encoding, EOL)
- Live Markdown and HTML Preview

Tip: Tap any line number on the left gutter to toggle a bookmark!
""".trimIndent()
                ),
                DocumentEntity(
                    title = "Fibonacci.py",
                    language = "python",
                    encoding = "UTF-8",
                    lineEnding = "LF",
                    bookmarks = "2",
                    orderIndex = 1,
                    content = """# Fibonacci sequence generator in Python

def fibonacci(n: int) -> list[int]:
    \"\"\"Return a list of the first n Fibonacci numbers.\"\"\"
    if n <= 0:
        return []
    sequence = [0, 1]
    while len(sequence) < n:
        next_val = sequence[-1] + sequence[-2]
        sequence.append(next_val)
    return sequence[:n]

if __name__ == "__main__":
    count = 10
    results = fibonacci(count)
    print(f"First {count} numbers: {results}")
""".trimIndent()
                ),
                DocumentEntity(
                    title = "Index.html",
                    language = "html",
                    encoding = "UTF-8",
                    lineEnding = "LF",
                    bookmarks = "",
                    orderIndex = 2,
                    content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Notepad++ Mobile</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #e2e8f0; padding: 28px; line-height: 1.6; }
    h1 { color: #38bdf8; font-size: 24px; margin-bottom: 8px; }
    p { color: #94a3b8; font-size: 15px; }
    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 16px; margin-top: 16px; }
    .badge { display: inline-block; background: #10b981; color: #022c22; font-weight: 700; font-size: 12px; padding: 4px 10px; border-radius: 9999px; }
  </style>
</head>
<body>
  <h1>Notepad++ Live HTML Preview</h1>
  <p>Edit HTML and CSS right here and tap Preview to see the rendered output!</p>
  <div class="card">
    <span class="badge">Active Document</span>
    <p style="margin-top: 8px;">Fast, lightweight, and offline capable.</p>
  </div>
</body>
</html>""".trimIndent()
                ),
                DocumentEntity(
                    title = "Config.json",
                    language = "json",
                    encoding = "UTF-8",
                    lineEnding = "LF",
                    bookmarks = "",
                    orderIndex = 3,
                    content = """{
  "editor": {
    "theme": "dark",
    "fontSize": 14,
    "tabSize": 4,
    "wordWrap": false,
    "showLineNumbers": true,
    "encoding": "UTF-8"
  },
  "syntaxHighlighting": {
    "enabled": true,
    "bracketMatching": true,
    "autoIndent": true
  },
  "file": {
    "autoSave": true,
    "defaultExtension": ".txt"
  }
}""".trimIndent()
                )
            )
            documentDao.insertAll(defaultDocs)
        }
    }
}
