package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.syntax.LanguageDefinition
import com.example.ui.theme.EditorThemeColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EditorStatusBar(
    language: LanguageDefinition,
    currentLine: Int,
    currentCol: Int,
    selectionLength: Int,
    totalLength: Int,
    totalLines: Int,
    lineEnding: String,
    encoding: String,
    fontSizeSp: Float,
    isReadOnly: Boolean,
    theme: EditorThemeColors,
    onLanguageClick: () -> Unit,
    onLineEndingClick: () -> Unit,
    onEncodingClick: () -> Unit,
    onZoomClick: () -> Unit,
    onReadOnlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = theme.gutterBackground,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector button
            StatusItem(
                text = language.name,
                testTag = "status_language",
                theme = theme,
                isHighlight = true,
                onClick = onLanguageClick
            )

            StatusDivider(theme)

            // Length and lines
            StatusItem(
                text = "length: $totalLength   lines: $totalLines",
                testTag = "status_length_lines",
                theme = theme
            )

            StatusDivider(theme)

            // Cursor Ln, Col, Sel
            val selText = if (selectionLength > 0) "   Sel: $selectionLength" else ""
            StatusItem(
                text = "Ln: $currentLine   Col: $currentCol$selText",
                testTag = "status_ln_col",
                theme = theme
            )

            StatusDivider(theme)

            // Zoom level
            val zoomPercent = (fontSizeSp / 14f * 100).roundToInt()
            StatusItem(
                text = "${fontSizeSp.toInt()}sp ($zoomPercent%)",
                testTag = "status_zoom_level",
                theme = theme,
                isHighlight = abs(fontSizeSp - 14f) > 0.3f,
                highlightColor = theme.bookmarkColor,
                onClick = onZoomClick
            )

            StatusDivider(theme)

            // Line Ending (LF / CRLF)
            val eolDisplay = if (lineEnding == "CRLF") "Windows (CRLF)" else "Unix (LF)"
            StatusItem(
                text = eolDisplay,
                testTag = "status_line_ending",
                theme = theme,
                onClick = onLineEndingClick
            )

            StatusDivider(theme)

            // Encoding (UTF-8 / ANSI)
            StatusItem(
                text = encoding,
                testTag = "status_encoding",
                theme = theme,
                onClick = onEncodingClick
            )

            StatusDivider(theme)

            // Mode: INS (Insert) / RO (Read-Only)
            StatusItem(
                text = if (isReadOnly) "RO" else "INS",
                testTag = "status_read_only",
                theme = theme,
                isHighlight = isReadOnly,
                highlightColor = if (isReadOnly) Color(0xFFFF5252) else null,
                onClick = onReadOnlyClick
            )
        }
    }
}

@Composable
private fun StatusItem(
    text: String,
    testTag: String,
    theme: EditorThemeColors,
    isHighlight: Boolean = false,
    highlightColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    } else {
        Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    }

    Box(
        modifier = clickModifier.testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = highlightColor ?: (if (isHighlight) theme.bookmarkColor else theme.gutterText)
        )
    }
}

@Composable
private fun StatusDivider(theme: EditorThemeColors) {
    VerticalDivider(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp),
        color = theme.gutterText.copy(alpha = 0.35f)
    )
}
