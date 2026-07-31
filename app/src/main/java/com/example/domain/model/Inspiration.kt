package com.example.domain.model

data class Inspiration(
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis(),
    val tag: String = "",
    val category: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Long = 0L,
    val isContentVisible: Boolean = true
)
