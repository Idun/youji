package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class YouJiExportData(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val appName: String = "由记",
    val themeColor: Long = 0xFF1B7679L,
    val groups: List<GroupInfo> = emptyList(),
    val inspirations: List<Inspiration> = emptyList()
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportTime", exportTime)
        root.put("appName", appName)
        root.put("themeColor", themeColor)

        val groupsArray = JSONArray()
        groups.forEach { g ->
            val gObj = JSONObject()
            gObj.put("name", g.name)
            gObj.put("timestamp", g.timestamp)
            gObj.put("colorHex", g.colorHex)
            gObj.put("sortOrder", g.sortOrder)
            groupsArray.put(gObj)
        }
        root.put("groups", groupsArray)

        val notesArray = JSONArray()
        inspirations.forEach { n ->
            val nObj = JSONObject()
            nObj.put("id", n.id)
            nObj.put("title", n.title)
            nObj.put("content", n.content)
            nObj.put("timestamp", n.timestamp)
            nObj.put("modifiedTimestamp", n.modifiedTimestamp)
            nObj.put("tag", n.tag)
            nObj.put("category", n.category)
            nObj.put("isPinned", n.isPinned)
            nObj.put("isArchived", n.isArchived)
            nObj.put("sortOrder", n.sortOrder)
            nObj.put("isContentVisible", n.isContentVisible)
            notesArray.put(nObj)
        }
        root.put("inspirations", notesArray)

        return root.toString(2)
    }

    companion object {
        fun parseFromJson(jsonStr: String): YouJiExportData? {
            return try {
                val root = JSONObject(jsonStr)
                val version = root.optInt("version", 1)
                val exportTime = root.optLong("exportTime", System.currentTimeMillis())
                val appName = root.optString("appName", "由记")
                val themeColor = root.optLong("themeColor", 0xFF1B7679L)

                val groupsList = mutableListOf<GroupInfo>()
                if (root.has("groups")) {
                    val groupsArray = root.getJSONArray("groups")
                    for (i in 0 until groupsArray.length()) {
                        val gObj = groupsArray.getJSONObject(i)
                        groupsList.add(
                            GroupInfo(
                                name = gObj.getString("name"),
                                timestamp = gObj.optLong("timestamp", System.currentTimeMillis()),
                                colorHex = gObj.optString("colorHex", "#7F7F7F"),
                                sortOrder = gObj.optInt("sortOrder", i)
                            )
                        )
                    }
                }

                val notesList = mutableListOf<Inspiration>()
                if (root.has("inspirations")) {
                    val notesArray = root.getJSONArray("inspirations")
                    for (i in 0 until notesArray.length()) {
                        val nObj = notesArray.getJSONObject(i)
                        notesList.add(
                            Inspiration(
                                id = nObj.optInt("id", 0),
                                title = nObj.optString("title", ""),
                                content = nObj.optString("content", ""),
                                timestamp = nObj.optLong("timestamp", System.currentTimeMillis()),
                                modifiedTimestamp = nObj.optLong("modifiedTimestamp", System.currentTimeMillis()),
                                tag = nObj.optString("tag", ""),
                                category = nObj.optString("category", ""),
                                isPinned = nObj.optBoolean("isPinned", false),
                                isArchived = nObj.optBoolean("isArchived", false),
                                sortOrder = nObj.optLong("sortOrder", i.toLong()),
                                isContentVisible = nObj.optBoolean("isContentVisible", true)
                            )
                        )
                    }
                }

                YouJiExportData(
                    version = version,
                    exportTime = exportTime,
                    appName = appName,
                    themeColor = themeColor,
                    groups = groupsList,
                    inspirations = notesList
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
