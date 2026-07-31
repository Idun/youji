package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Inspiration

@Entity(tableName = "inspirations")
data class InspirationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val modifiedTimestamp: Long,
    val tag: String,
    val category: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Long = 0L,
    val isContentVisible: Boolean = true
) {
    fun toDomain(): Inspiration {
        return Inspiration(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            modifiedTimestamp = modifiedTimestamp,
            tag = tag,
            category = category,
            isPinned = isPinned,
            isArchived = isArchived,
            sortOrder = sortOrder,
            isContentVisible = isContentVisible
        )
    }

    companion object {
        fun fromDomain(domain: Inspiration): InspirationEntity {
            return InspirationEntity(
                id = domain.id,
                title = domain.title,
                content = domain.content,
                timestamp = domain.timestamp,
                modifiedTimestamp = domain.modifiedTimestamp,
                tag = domain.tag,
                category = domain.category,
                isPinned = domain.isPinned,
                isArchived = domain.isArchived,
                sortOrder = domain.sortOrder,
                isContentVisible = domain.isContentVisible
            )
        }
    }
}
