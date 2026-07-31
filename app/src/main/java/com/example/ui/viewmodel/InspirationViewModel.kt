package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.GroupInfo
import com.example.domain.model.Inspiration
import com.example.domain.model.YouJiExportData
import com.example.domain.repository.InspirationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortType {
    MODIFIED_TIME, CREATED_TIME, TITLE_ALPHA
}

enum class SortOrder {
    ASC, DESC
}

class InspirationViewModel(
    private val repository: InspirationRepository,
    private val sharedPreferences: android.content.SharedPreferences? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _tagTimeFilter = MutableStateFlow("全部")
    val tagTimeFilter: StateFlow<String> = _tagTimeFilter.asStateFlow()

    private val _tagSearchQuery = MutableStateFlow("")
    val tagSearchQuery: StateFlow<String> = _tagSearchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("全部笔记")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.MODIFIED_TIME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _themeColor = MutableStateFlow(
        sharedPreferences?.getLong("theme_color", 0xFF1B7679L) ?: 0xFF1B7679L
    )
    val themeColor: StateFlow<Long> = _themeColor.asStateFlow()

    fun setThemeColor(color: Long) {
        _themeColor.value = color
        sharedPreferences?.edit()?.putLong("theme_color", color)?.apply()
    }

    val searchHistory: StateFlow<List<String>> = repository.getSearchHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allInspirations: StateFlow<List<Inspiration>> = repository.getAllInspirations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inspirations: StateFlow<List<Inspiration>> = combine(
        repository.getAllInspirations(),
        combine(_searchQuery, _selectedTag, _showArchived, _selectedGroup, _tagTimeFilter) { q, t, a, g, tf -> listOf(q, t, a, g, tf) },
        combine(_sortType, _sortOrder, ::Pair)
    ) { list, queryData, sortData ->
        val query = queryData[0] as String
        val tag = queryData[1] as String
        val showArchived = queryData[2] as Boolean
        val selectedGroup = queryData[3] as String
        val tagTimeFilter = queryData[4] as String
        val sortType = sortData.first
        val sortOrder = sortData.second
        
        val filtered = list.filter { inspiration ->
            val matchesArchive = inspiration.isArchived == showArchived
            val matchesQuery = query.isEmpty() ||
                    inspiration.title.contains(query, ignoreCase = true) ||
                    inspiration.content.contains(query, ignoreCase = true) ||
                    inspiration.tag.contains(query, ignoreCase = true) ||
                    inspiration.category.contains(query, ignoreCase = true)
            val matchesTag = tag.isEmpty() || 
                    inspiration.tag.split(",").any { it.trim().equals(tag, ignoreCase = true) }
            val matchesGroup = when (selectedGroup) {
                "全部笔记" -> true
                "未分类" -> inspiration.category.isBlank()
                else -> inspiration.category == selectedGroup
            }
            
            val matchesTime = if (tag.isNotEmpty() && tagTimeFilter != "全部") {
                val now = System.currentTimeMillis()
                val startOfPeriod = when (tagTimeFilter) {
                    "今天" -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    "本周" -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    "本月" -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    "最近三个月" -> {
                        now - 90L * 24 * 60 * 60 * 1000
                    }
                    else -> 0L
                }
                inspiration.timestamp >= startOfPeriod
            } else {
                true
            }

            matchesArchive && matchesQuery && matchesTag && matchesGroup && matchesTime
        }

        // Pinned ones always come first. Then apply sorting.
        val (pinned, unpinned) = filtered.partition { it.isPinned }
        
        val comparator = when (sortType) {
            SortType.MODIFIED_TIME -> if (sortOrder == SortOrder.DESC) compareByDescending<Inspiration> { it.modifiedTimestamp } else compareBy<Inspiration> { it.modifiedTimestamp }
            SortType.CREATED_TIME -> if (sortOrder == SortOrder.DESC) compareByDescending<Inspiration> { it.timestamp } else compareBy<Inspiration> { it.timestamp }
            SortType.TITLE_ALPHA -> if (sortOrder == SortOrder.DESC) compareByDescending<Inspiration> { it.title.lowercase() } else compareBy<Inspiration> { it.title.lowercase() }
        }

        val sortedPinned = if (sortType == SortType.MODIFIED_TIME && sortOrder == SortOrder.DESC) {
            // Keep manual sort order (by sortOrder ascending, then modified descending)
            pinned.sortedWith(compareBy<Inspiration> { it.sortOrder }.thenByDescending { it.modifiedTimestamp })
        } else {
            pinned.sortedWith(comparator)
        }

        val sortedUnpinned = if (sortType == SortType.MODIFIED_TIME && sortOrder == SortOrder.DESC) {
            unpinned.sortedWith(compareBy<Inspiration> { it.sortOrder }.thenByDescending { it.modifiedTimestamp })
        } else {
            unpinned.sortedWith(comparator)
        }

        sortedPinned + sortedUnpinned
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTags: StateFlow<List<String>> = repository.getAllInspirations()
        .map { list ->
            list.filter { !it.isArchived }
                .flatMap { 
                    it.tag.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() }
                }
                .distinct()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.insertSearchHistory(query.trim())
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun setSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun setShowArchived(show: Boolean) {
        _showArchived.value = show
        _selectedTag.value = ""
    }

    fun getInspirationById(id: Int): Flow<Inspiration?> {
        return repository.getInspirationById(id)
    }

    suspend fun saveInspiration(
        title: String,
        content: String,
        tag: String,
        category: String = "",
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        existingId: Int = 0,
        sortOrder: Long = 0L,
        createdTimestamp: Long = System.currentTimeMillis()
    ): Int {
        val trimmedTitle = title.trim()
        val finalTitle = if (trimmedTitle.isEmpty()) "无标题灵感" else trimmedTitle
        
        val inspiration = Inspiration(
            id = existingId,
            title = finalTitle,
            content = content,
            tag = tag.trim(),
            category = category.trim(),
            isPinned = isPinned,
            isArchived = isArchived,
            timestamp = createdTimestamp,
            modifiedTimestamp = System.currentTimeMillis(),
            sortOrder = sortOrder
        )
        return if (existingId == 0) {
            val newId = repository.insertInspiration(inspiration)
            newId.toInt()
        } else {
            repository.updateInspiration(inspiration)
            existingId
        }
    }

    fun togglePin(inspiration: Inspiration) {
        viewModelScope.launch {
            repository.updateInspiration(inspiration.copy(isPinned = !inspiration.isPinned, modifiedTimestamp = System.currentTimeMillis()))
        }
    }

    fun toggleArchive(inspiration: Inspiration) {
        viewModelScope.launch {
            repository.updateInspiration(inspiration.copy(isArchived = !inspiration.isArchived, modifiedTimestamp = System.currentTimeMillis()))
        }
    }

    fun toggleContentVisibility(inspiration: Inspiration) {
        viewModelScope.launch {
            repository.updateInspiration(inspiration.copy(isContentVisible = !inspiration.isContentVisible, modifiedTimestamp = System.currentTimeMillis()))
        }
    }

    fun deleteInspiration(inspiration: Inspiration) {
        viewModelScope.launch {
            repository.deleteInspiration(inspiration)
        }
    }

    fun reorderInspirations(fromIndex: Int, toIndex: Int, currentList: List<Inspiration>) {
        if (fromIndex == toIndex) return
        val items = currentList.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
        
        // Update sortOrder for the affected items
        val updatedItems = items.mapIndexed { index, inspiration ->
            inspiration.copy(sortOrder = index.toLong())
        }
        
        // Save to DB (this sets modified order to custom sorting effectively when default sorting is selected)
        viewModelScope.launch {
            // we should reset the sort preference to default if user drags
            _sortType.value = SortType.MODIFIED_TIME
            _sortOrder.value = SortOrder.DESC
            repository.updateInspirations(updatedItems)
        }
    }


    val allGroups: StateFlow<List<GroupInfo>> = repository.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    fun setSelectedGroup(group: String) {
        _selectedGroup.value = group
    }

    fun addGroup(name: String, colorHex: String = "#7F7F7F") {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.insertGroup(name.trim(), colorHex)
            }
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || oldName == trimmedNew) return
        viewModelScope.launch {
            val currentGroups = allGroups.value
            val updatedGroups = currentGroups.map {
                if (it.name == oldName) it.copy(name = trimmedNew) else it
            }
            repository.updateGroups(updatedGroups)

            val allNotes = allInspirations.value
            val affectedNotes = allNotes.filter { it.category == oldName }
            if (affectedNotes.isNotEmpty()) {
                val updatedNotes = affectedNotes.map { it.copy(category = trimmedNew, modifiedTimestamp = System.currentTimeMillis()) }
                repository.updateInspirations(updatedNotes)
            }

            if (_selectedGroup.value == oldName) {
                _selectedGroup.value = trimmedNew
            }
        }
    }

    fun deleteGroup(groupName: String) {
        if (groupName.isBlank()) return
        viewModelScope.launch {
            repository.deleteGroup(groupName)

            val allNotes = allInspirations.value
            val affectedNotes = allNotes.filter { it.category == groupName }
            if (affectedNotes.isNotEmpty()) {
                val updatedNotes = affectedNotes.map { it.copy(category = "", modifiedTimestamp = System.currentTimeMillis()) }
                repository.updateInspirations(updatedNotes)
            }

            if (_selectedGroup.value == groupName) {
                _selectedGroup.value = "全部笔记"
            }
        }
    }

    fun reorderGroups(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val currentGroups = allGroups.value.toMutableList()
        if (fromIndex in currentGroups.indices && toIndex in currentGroups.indices) {
            val item = currentGroups.removeAt(fromIndex)
            currentGroups.add(toIndex, item)
            val updatedGroups = currentGroups.mapIndexed { index, group ->
                group.copy(sortOrder = index)
            }
            viewModelScope.launch {
                repository.updateGroups(updatedGroups)
            }
        }
    }

    fun deleteInspirations(inspirations: List<Inspiration>) {
        viewModelScope.launch {
            repository.deleteInspirations(inspirations)
        }
    }

    fun archiveInspirations(inspirations: List<Inspiration>, archive: Boolean) {
        viewModelScope.launch {
            val updated = inspirations.map { it.copy(isArchived = archive, modifiedTimestamp = System.currentTimeMillis()) }
            repository.updateInspirations(updated)
        }
    }

    fun tagInspirations(inspirations: List<Inspiration>, tag: String) {
        viewModelScope.launch {
            val updated = inspirations.map { it.copy(tag = tag, modifiedTimestamp = System.currentTimeMillis()) }
            repository.updateInspirations(updated)
        }
    }

    fun moveInspirationsToGroup(inspirations: List<Inspiration>, group: String) {
        viewModelScope.launch {
            val targetGroup = if (group == "全部笔记") "" else group
            val updated = inspirations.map { it.copy(category = targetGroup, modifiedTimestamp = System.currentTimeMillis()) }
            repository.updateInspirations(updated)
        }
    }

    fun setTagTimeFilter(filter: String) {
        _tagTimeFilter.value = filter
    }

    fun setTagSearchQuery(query: String) {
        _tagSearchQuery.value = query
    }

    private fun loadQuickPhrasesFromPrefs(): List<com.example.domain.model.QuickPhrase> {
        if (sharedPreferences != null && !sharedPreferences.getBoolean("cleared_legacy_mock_phrases_v2", false)) {
            sharedPreferences.edit().remove("quick_phrases_json").putBoolean("cleared_legacy_mock_phrases_v2", true).apply()
            return emptyList()
        }
        val jsonStr = sharedPreferences?.getString("quick_phrases_json", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<com.example.domain.model.QuickPhrase>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.domain.model.QuickPhrase(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        label = obj.optString("label", ""),
                        content = obj.optString("content", ""),
                        usageCount = obj.optInt("usageCount", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveQuickPhrasesToPrefs(list: List<com.example.domain.model.QuickPhrase>) {
        val array = org.json.JSONArray()
        list.forEach { phrase ->
            val obj = org.json.JSONObject()
            obj.put("id", phrase.id)
            obj.put("label", phrase.label)
            obj.put("content", phrase.content)
            obj.put("usageCount", phrase.usageCount)
            array.put(obj)
        }
        sharedPreferences?.edit()?.putString("quick_phrases_json", array.toString())?.apply()
    }

    private val _quickPhrases = MutableStateFlow(loadQuickPhrasesFromPrefs())
    val quickPhrases: StateFlow<List<com.example.domain.model.QuickPhrase>> = _quickPhrases.asStateFlow()

    fun incrementQuickPhraseUsage(id: String) {
        val current = _quickPhrases.value.map {
            if (it.id == id) it.copy(usageCount = it.usageCount + 1) else it
        }
        _quickPhrases.value = current
        saveQuickPhrasesToPrefs(current)
    }

    fun addQuickPhrase(label: String, content: String) {
        if (label.isBlank() || content.isBlank()) return
        val current = _quickPhrases.value.toMutableList()
        current.add(com.example.domain.model.QuickPhrase(label = label.trim(), content = content))
        _quickPhrases.value = current
        saveQuickPhrasesToPrefs(current)
    }

    fun updateQuickPhrase(id: String, newLabel: String, newContent: String) {
        if (newLabel.isBlank() || newContent.isBlank()) return
        val current = _quickPhrases.value.map {
            if (it.id == id) it.copy(label = newLabel.trim(), content = newContent) else it
        }
        _quickPhrases.value = current
        saveQuickPhrasesToPrefs(current)
    }

    fun deleteQuickPhrase(id: String) {
        val current = _quickPhrases.value.filter { it.id != id }
        _quickPhrases.value = current
        saveQuickPhrasesToPrefs(current)
    }

    fun restoreDefaultQuickPhrases() {
        _quickPhrases.value = emptyList()
        saveQuickPhrasesToPrefs(emptyList())
    }

    fun exportAllDataJson(): String {
        val data = YouJiExportData(
            version = 1,
            exportTime = System.currentTimeMillis(),
            themeColor = _themeColor.value,
            groups = allGroups.value,
            inspirations = allInspirations.value
        )
        return data.toJsonString()
    }

    suspend fun importDataJson(jsonStr: String): Boolean {
        val data = YouJiExportData.parseFromJson(jsonStr) ?: return false
        repository.restoreData(data.inspirations, data.groups)
        setThemeColor(data.themeColor)
        return true
    }

    class Factory(
        private val repository: InspirationRepository,
        private val sharedPreferences: android.content.SharedPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InspirationViewModel::class.java)) {
                return InspirationViewModel(repository, sharedPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class ThemeColorOption(val name: String, val colorValue: Long)

val themeColors = listOf(
    ThemeColorOption("青色（默认）", 0xFF1B7679L),
    ThemeColorOption("橙色", 0xFFFFE083L),
    ThemeColorOption("粉色", 0xFFFB7299L),
    ThemeColorOption("金色", 0xFFFE8326L),
    ThemeColorOption("绿色", 0xFF06AD56L)
)
