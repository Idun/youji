package com.example.domain.repository

import com.example.domain.model.Inspiration
import kotlinx.coroutines.flow.Flow

interface InspirationRepository {
    fun getAllInspirations(): Flow<List<Inspiration>>
    fun getInspirationById(id: Int): Flow<Inspiration?>
    suspend fun insertInspiration(inspiration: Inspiration): Long
    suspend fun deleteInspiration(inspiration: Inspiration)
    suspend fun deleteInspirations(inspirations: List<Inspiration>)
    suspend fun updateInspiration(inspiration: Inspiration)
    suspend fun updateInspirations(inspirations: List<Inspiration>)
    fun getSearchHistory(): Flow<List<String>>
    suspend fun insertSearchHistory(keyword: String)
    suspend fun clearSearchHistory()
    fun getAllGroups(): Flow<List<com.example.domain.model.GroupInfo>>
    suspend fun insertGroup(name: String, colorHex: String)
    suspend fun updateGroups(groups: List<com.example.domain.model.GroupInfo>)
    suspend fun deleteGroup(name: String)
    suspend fun restoreData(inspirations: List<Inspiration>, groups: List<com.example.domain.model.GroupInfo>)
}
