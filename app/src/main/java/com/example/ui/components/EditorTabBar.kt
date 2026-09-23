package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.theme.EditorThemeColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorTabBar(
    documents: List<DocumentEntity>,
    activeDocumentId: Long?,
    isCurrentModified: Boolean,
    theme: EditorThemeColors,
    onTabSelected: (Long) -> Unit,
    onTabClosed: (Long) -> Unit,
    onNewTab: () -> Unit,
    onTabLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // When a new document is created or loaded (or when active tab is the last tab),
    // automatically scroll the tab bar so the last tab and the '+' button are within the view area.
    LaunchedEffect(activeDocumentId, documents.size) {
        if (documents.isNotEmpty() && activeDocumentId != null) {
            val activeIndex = documents.indexOfFirst { it.id == activeDocumentId }
            if (activeIndex == documents.size - 1 || activeIndex == -1) {
                delay(50)
                scrollState.animateScrollTo(scrollState.maxValue)
                delay(50)
                if (scrollState.value < scrollState.maxValue) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }

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
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            documents.forEachIndexed { index, doc ->
                val isSelected = doc.id == activeDocumentId
                val isModified = if (isSelected) isCurrentModified else doc.isModified
                val isLastTab = index == documents.size - 1

                DocumentTabItem(
                    title = doc.title,
                    isSelected = isSelected,
                    isModified = isModified,
                    isLastTab = isLastTab,
                    theme = theme,
                    onClick = { onTabSelected(doc.id) },
                    onClose = { onTabClosed(doc.id) },
                    onLongClick = { onTabLongClick(doc.id) }
                )
            }

            // '+' New Tab button
            IconButton(
                onClick = onNewTab,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(48.dp)
                    .testTag("add_tab_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = theme.text,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right-side breathing margin so the '+' button is always cleanly visible inside the view area
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentTabItem(
    title: String,
    isSelected: Boolean,
    isModified: Boolean,
    isLastTab: Boolean,
    theme: EditorThemeColors,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit
) {
    val tabBg = if (isSelected) theme.background else theme.surface
    val textColor = if (isSelected) theme.text else theme.gutterText
    val borderColor = if (isSelected) theme.bookmarkColor else Color.Transparent
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Ensure active non-last tabs are also scrolled smoothly into view when selected
    LaunchedEffect(isSelected) {
        if (isSelected && !isLastTab) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Box(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(start = 4.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(tabBg)
            .border(
                width = 1.dp,
                color = if (isSelected) borderColor.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(start = 10.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unsaved changes indicator (iconic red/amber dot when modified, calm blue/transparent when saved)
            if (isModified) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Close tab 'x' button with accessible tap target
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .testTag("close_tab_${title}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close $title",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
