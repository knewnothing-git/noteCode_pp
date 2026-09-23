package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.syntax.LanguageDefinition
import com.example.ui.theme.EditorThemeColors
import java.nio.charset.StandardCharsets

@Composable
fun DocumentStatsDialog(
    title: String,
    content: String,
    language: LanguageDefinition,
    encoding: String,
    lineEnding: String,
    theme: EditorThemeColors,
    onDismiss: () -> Unit
) {
    val stats = remember(content) {
        val chars = content.length
        val charsNoSpaces = content.count { !it.isWhitespace() }
        val words = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
        val lines = if (content.isEmpty()) 0 else content.split("\n").size
        val bytes = content.toByteArray(StandardCharsets.UTF_8).size
        val sizeFormatted = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
        DocumentStats(
            chars = chars,
            charsNoSpaces = charsNoSpaces,
            words = words,
            lines = lines,
            size = sizeFormatted
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Document Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.surface)
                    .padding(14.dp)
            ) {
                StatRow(label = "File Name", value = title)
                StatRow(label = "Language", value = language.name)
                StatRow(label = "Encoding", value = encoding)
                StatRow(label = "Format", value = if (lineEnding == "CRLF") "Windows (CRLF)" else "Unix (LF)")

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = theme.gutterText.copy(alpha = 0.2f)
                )

                StatRow(label = "Characters (total)", value = "%,d".format(stats.chars))
                StatRow(label = "Characters (no spaces)", value = "%,d".format(stats.charsNoSpaces))
                StatRow(label = "Words", value = "%,d".format(stats.words))
                StatRow(label = "Lines", value = "%,d".format(stats.lines))
                StatRow(label = "File Size", value = stats.size)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private data class DocumentStats(
    val chars: Int,
    val charsNoSpaces: Int,
    val words: Int,
    val lines: Int,
    val size: String
)

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
