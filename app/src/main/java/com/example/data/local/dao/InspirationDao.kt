package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InspirationEntity
import com.example.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspirationDao {
    @Query("SELECT * FROM inspirations ORDER BY isPinned DESC, sortOrder ASC, modifiedTimestamp DESC")
    fun getAllInspirations(): Flow<List<InspirationEntity>>

    @Query("SELECT * FROM inspirations WHERE id = :id")
    fun getInspirationById(id: Int): Flow<InspirationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspiration(entity: InspirationEntity): Long

    @Update
    suspend fun updateInspiration(entity: InspirationEntity)

    @Update
    suspend fun updateInspirations(entities: List<InspirationEntity>)
    
    @Delete
    suspend fun deleteInspirations(entities: List<InspirationEntity>)

    @Delete
    suspend fun deleteInspiration(entity: InspirationEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
    
    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, timestamp ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroups(entities: List<GroupEntity>)

    @Query("DELETE FROM groups WHERE name = :name")
    suspend fun deleteGroupByName(name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspirations(entities: List<InspirationEntity>)

    @Query("DELETE FROM inspirations")
    suspend fun clearInspirations()

    @Query("DELETE FROM groups")
    suspend fun clearGroups()
}
