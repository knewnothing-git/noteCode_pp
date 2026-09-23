package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorThemeColors

@Composable
fun SymbolQuickBar(
    theme: EditorThemeColors,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val quickSymbols = listOf(
        "TAB" to "    ",
        "{" to "{",
        "}" to "}",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        "<" to "<",
        ">" to ">",
        "=" to " = ",
        ";" to ";",
        ":" to ":",
        "\"" to "\"",
        "'" to "'",
        "$" to "$",
        "&" to "&",
        "|" to "|",
        "!" to "!",
        "#" to "#",
        "->" to " -> ",
        "=>" to " => ",
        "==" to " == ",
        "!=" to " != ",
        "// " to "// ",
        "/* */" to "/*  */",
        "def " to "def ",
        "fun " to "fun ",
        "class " to "class ",
        "const " to "const ",
        "return " to "return "
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = theme.gutterBackground,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickSymbols.forEach { (label, toInsert) ->
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.surface)
                        .clickable { onInsertSymbol(toInsert) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = theme.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("symbol_quick_$label")
                    )
                }
            }
        }
    }
}
