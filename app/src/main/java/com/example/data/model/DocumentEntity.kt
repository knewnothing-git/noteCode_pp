package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val language: String = "text",
    val encoding: String = "UTF-8",
    val lineEnding: String = "LF",
    val isModified: Boolean = false,
    val bookmarks: String = "", // Comma-separated 1-based line numbers
    val cursorPosition: Int = 0,
    val orderIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
