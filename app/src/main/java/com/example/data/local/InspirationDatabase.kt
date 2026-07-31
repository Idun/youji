package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.InspirationDao
import com.example.data.local.entity.InspirationEntity

@Database(entities = [InspirationEntity::class, com.example.data.local.entity.SearchHistoryEntity::class, com.example.data.local.entity.GroupEntity::class], version = 7, exportSchema = false)
abstract class InspirationDatabase : RoomDatabase() {
    abstract fun inspirationDao(): InspirationDao
}
