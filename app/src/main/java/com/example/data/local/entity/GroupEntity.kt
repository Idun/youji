package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.GroupInfo

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val name: String,
    val timestamp: Long,
    val colorHex: String = "#7F7F7F",
    val sortOrder: Int = 0
) {
    fun toDomain() = GroupInfo(
        name = name,
        timestamp = timestamp,
        colorHex = colorHex,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(domain: GroupInfo) = GroupEntity(
            name = domain.name,
            timestamp = domain.timestamp,
            colorHex = domain.colorHex,
            sortOrder = domain.sortOrder
        )
    }
}
