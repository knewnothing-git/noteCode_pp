package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.syntax.LanguageDefinition
import com.example.ui.syntax.SyntaxHighlighter
import com.example.ui.theme.EditorThemeColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class SyntaxVisualTransformation(
    private val language: LanguageDefinition,
    private val theme: EditorThemeColors,
    private val searchQuery: String,
    private val activeSearchIndex: Int,
    private val searchCaseSensitive: Boolean,
    private val searchWholeWord: Boolean,
    private val searchRegex: Boolean,
    private val showWhitespace: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(
            text = text.text,
            language = language,
            colors = theme,
            searchQuery = searchQuery,
            activeSearchIndex = activeSearchIndex,
            searchCaseSensitive = searchCaseSensitive,
            searchWholeWord = searchWholeWord,
            searchRegex = searchRegex,
            showWhitespace = showWhitespace
        )
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun EditorCanvas(
    editorValue: TextFieldValue,
    language: LanguageDefinition,
    theme: EditorThemeColors,
    bookmarks: Set<Int>,
    currentLineNumber: Int,
    wordWrap: Boolean,
    showWhitespace: Boolean,
    fontSizeSp: Float,
    isReadOnly: Boolean,
    searchQuery: String,
    activeSearchIndex: Int,
    searchCaseSensitive: Boolean,
    searchWholeWord: Boolean,
    searchRegex: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onPinchZoom: (Float) -> Unit,
    onResetZoom: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val currentOnPinchZoom by rememberUpdatedState(onPinchZoom)
    val coroutineScope = rememberCoroutineScope()
    var showZoomIndicator by remember { mutableStateOf(false) }
    var hideZoomJob by remember { mutableStateOf<Job?>(null) }

    val handleZoom: (Float) -> Unit = { zoomDelta ->
        currentOnPinchZoom(zoomDelta)
        showZoomIndicator = true
        hideZoomJob?.cancel()
        hideZoomJob = coroutineScope.launch {
            delay(1200)
            showZoomIndicator = false
        }
    }

    val visualTransformation = remember(
        language,
        theme,
        searchQuery,
        activeSearchIndex,
        searchCaseSensitive,
        searchWholeWord,
        searchRegex,
        showWhitespace
    ) {
        SyntaxVisualTransformation(
            language = language,
            theme = theme,
            searchQuery = searchQuery,
            activeSearchIndex = activeSearchIndex,
            searchCaseSensitive = searchCaseSensitive,
            searchWholeWord = searchWholeWord,
            searchRegex = searchRegex,
            showWhitespace = showWhitespace
        )
    }

    val lines = remember(editorValue.text) {
        editorValue.text.split("\n")
    }
    val lineCount = lines.size.coerceAtLeast(1)

    val gutterWidth = remember(lineCount, fontSizeSp) {
        val digits = lineCount.toString().length.coerceAtLeast(2)
        val charWidthEstimate = (fontSizeSp * 0.85f * 0.62f).dp
        val calculated = (digits * charWidthEstimate.value + 26f).dp
        maxOf(calculated, 40.dp)
    }

    val editorTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.45f).sp,
        color = theme.text
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var previousDist = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val p0 = pressed[0].position
                            val p1 = pressed[1].position
                            val currentDist = hypot(p0.x - p1.x, p0.y - p1.y)

                            if (previousDist > 0f && currentDist > 0f) {
                                val zoomFactor = currentDist / previousDist
                                if (zoomFactor in 0.5f..2.0f && abs(zoomFactor - 1f) > 0.001f) {
                                    handleZoom(zoomFactor)
                                }
                            }
                            previousDist = currentDist
                            pressed.forEach { it.consume() }
                        } else {
                            previousDist = 0f
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers gutter
            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .background(theme.gutterBackground)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val isBookmarked = bookmarks.contains(i)
                    val isCurrentLine = (i == currentLineNumber)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((fontSizeSp * 1.45f).dp)
                            .clickable { onToggleBookmark(i) }
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isBookmarked) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmark on line $i",
                                tint = theme.bookmarkColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }

                        Text(
                            text = i.toString(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = (fontSizeSp * 0.85f).sp,
                                fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentLine) theme.text else theme.gutterText
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Gutter vertical separator line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(theme.gutterText.copy(alpha = 0.25f))
            )

            // Text Editor Canvas Area
            val textContainerModifier = if (wordWrap) {
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            } else {
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .widthIn(min = 800.dp)
            }

            Box(modifier = textContainerModifier) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = {
                        if (!isReadOnly) {
                            onValueChange(it)
                        }
                    },
                    readOnly = isReadOnly,
                    textStyle = editorTextStyle,
                    cursorBrush = SolidColor(theme.bookmarkColor),
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_text_field")
                )
            }
        }

        // Floating Zoom HUD Badge
        AnimatedVisibility(
            visible = showZoomIndicator,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            val zoomPercent = (fontSizeSp / 14f * 100).roundToInt()
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.5.dp, theme.bookmarkColor),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("pinch_zoom_hud_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (fontSizeSp >= 14f) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                        contentDescription = "Zoom Indicator",
                        tint = theme.bookmarkColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", fontSizeSp)} sp  ($zoomPercent%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = theme.text
                    )
                    if (abs(fontSizeSp - 14f) > 0.3f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.bookmarkColor.copy(alpha = 0.15f))
                                .clickable {
                                    onResetZoom()
                                    showZoomIndicator = true
                                    hideZoomJob?.cancel()
                                    hideZoomJob = coroutineScope.launch {
                                        delay(1000)
                                        showZoomIndicator = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("pinch_zoom_reset_button")
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.bookmarkColor
                            )
                        }
                    }
                }
            }
        }
    }
}
