package com.example.domain.model

import java.util.UUID

data class QuickPhrase(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val content: String,
    val usageCount: Int = 0
)

val DEFAULT_QUICK_PHRASES = emptyList<QuickPhrase>()
