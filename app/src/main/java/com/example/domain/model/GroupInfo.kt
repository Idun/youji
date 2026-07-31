package com.example.domain.model

data class GroupInfo(
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val colorHex: String = "#7F7F7F",
    val sortOrder: Int = 0
)
