package com.example.data.repository

import com.example.data.local.dao.InspirationDao
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.InspirationEntity
import com.example.data.local.entity.SearchHistoryEntity
import com.example.domain.model.Inspiration
import com.example.domain.repository.InspirationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InspirationRepositoryImpl(
    private val dao: InspirationDao
) : InspirationRepository {
    override fun getAllInspirations(): Flow<List<Inspiration>> {
        return dao.getAllInspirations().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getInspirationById(id: Int): Flow<Inspiration?> {
        return dao.getInspirationById(id).map { it?.toDomain() }
    }

    override suspend fun insertInspiration(inspiration: Inspiration): Long {
        return dao.insertInspiration(InspirationEntity.fromDomain(inspiration))
    }

    override suspend fun deleteInspiration(inspiration: Inspiration) {
        dao.deleteInspiration(InspirationEntity.fromDomain(inspiration))
    }

    override suspend fun deleteInspirations(inspirations: List<Inspiration>) {
        dao.deleteInspirations(inspirations.map { InspirationEntity.fromDomain(it) })
    }

    override suspend fun updateInspiration(inspiration: Inspiration) {
        dao.updateInspiration(InspirationEntity.fromDomain(inspiration))
    }

    override suspend fun updateInspirations(inspirations: List<Inspiration>) {
        dao.updateInspirations(inspirations.map { InspirationEntity.fromDomain(it) })
    }

    override fun getSearchHistory(): Flow<List<String>> {
        return dao.getSearchHistory().map { list -> list.map { it.keyword } }
    }

    override suspend fun insertSearchHistory(keyword: String) {
        dao.insertSearchHistory(SearchHistoryEntity(keyword = keyword, timestamp = System.currentTimeMillis()))
    }

    override suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }
    
    override fun getAllGroups(): Flow<List<com.example.domain.model.GroupInfo>> {
        return dao.getAllGroups().map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun insertGroup(name: String, colorHex: String) {
        dao.insertGroup(GroupEntity(name = name, timestamp = System.currentTimeMillis(), colorHex = colorHex))
    }

    override suspend fun updateGroups(groups: List<com.example.domain.model.GroupInfo>) {
        dao.updateGroups(groups.map { GroupEntity.fromDomain(it) })
    }

    override suspend fun deleteGroup(name: String) {
        dao.deleteGroupByName(name)
    }

    override suspend fun restoreData(inspirations: List<Inspiration>, groups: List<com.example.domain.model.GroupInfo>) {
        dao.clearInspirations()
        dao.clearGroups()
        dao.insertInspirations(inspirations.map { InspirationEntity.fromDomain(it) })
        groups.forEach { dao.insertGroup(GroupEntity.fromDomain(it)) }
    }
}
