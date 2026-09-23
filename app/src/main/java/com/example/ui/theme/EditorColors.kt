package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class EditorThemeColors(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val gutterBackground: Color,
    val gutterText: Color,
    val text: Color,
    val activeLineBackground: Color,
    val bookmarkColor: Color,
    val selectionColor: Color,
    val searchHighlightColor: Color,
    val activeSearchHighlightColor: Color,
    // Syntax colors
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val function: Color,
    val type: Color,
    val tag: Color,
    val attribute: Color,
    val operator: Color
)

val NotepadClassicTheme = EditorThemeColors(
    id = "light",
    name = "Light (Notepad++)",
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF6F8FA),
    gutterBackground = Color(0xFFEEEEEE),
    gutterText = Color(0xFF505050),
    text = Color(0xFF1E1E1E),
    activeLineBackground = Color(0xFFE8F2FE),
    bookmarkColor = Color(0xFF0062A3),
    selectionColor = Color(0xFFADD6FF),
    searchHighlightColor = Color(0xFFFFE57F),
    activeSearchHighlightColor = Color(0xFFFF9800),
    keyword = Color(0xFF0000FF),
    string = Color(0xFFA31515),
    comment = Color(0xFF007000),
    number = Color(0xFFD64E00),
    function = Color(0xFF795E26),
    type = Color(0xFF267F99),
    tag = Color(0xFF000080),
    attribute = Color(0xFFE50000),
    operator = Color(0xFF000000)
)

val ObsidianDarkTheme = EditorThemeColors(
    id = "dark",
    name = "Dark (Obsidian)",
    isDark = true,
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF181825),
    gutterBackground = Color(0xFF11111B),
    gutterText = Color(0xFFA6ADC8),
    text = Color(0xFFCDD6F4),
    activeLineBackground = Color(0xFF2A2A3E),
    bookmarkColor = Color(0xFF89B4FA),
    selectionColor = Color(0xFF45475A),
    searchHighlightColor = Color(0xFFF9E2AF).copy(alpha = 0.5f),
    activeSearchHighlightColor = Color(0xFFFAB387),
    keyword = Color(0xFFCBA6F7),
    string = Color(0xFFA6E3A1),
    comment = Color(0xFF9399B2),
    number = Color(0xFFFAB387),
    function = Color(0xFF89B4FA),
    type = Color(0xFFF9E2AF),
    tag = Color(0xFF89DCEB),
    attribute = Color(0xFFF38BA8),
    operator = Color(0xFF94E2D5)
)

val MonokaiTheme = EditorThemeColors(
    id = "monokai",
    name = "Monokai",
    isDark = true,
    background = Color(0xFF272822),
    surface = Color(0xFF1E1F1C),
    gutterBackground = Color(0xFF171814),
    gutterText = Color(0xFFA5A08C),
    text = Color(0xFFF8F8F2),
    activeLineBackground = Color(0xFF3E3D32),
    bookmarkColor = Color(0xFFA6E22E),
    selectionColor = Color(0xFF49483E),
    searchHighlightColor = Color(0xFFE6DB74).copy(alpha = 0.5f),
    activeSearchHighlightColor = Color(0xFFFD971F),
    keyword = Color(0xFFF92672),
    string = Color(0xFFE6DB74),
    comment = Color(0xFFA5A08C),
    number = Color(0xFFAE81FF),
    function = Color(0xFFA6E22E),
    type = Color(0xFF66D9EF),
    tag = Color(0xFFF92672),
    attribute = Color(0xFFA6E22E),
    operator = Color(0xFFF92672)
)

val AvailableEditorThemes = listOf(
    NotepadClassicTheme,
    ObsidianDarkTheme,
    MonokaiTheme
)
