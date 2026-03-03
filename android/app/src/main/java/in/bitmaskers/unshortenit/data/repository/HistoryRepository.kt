package `in`.bitmaskers.unshortenit.data.repository

import `in`.bitmaskers.unshortenit.data.model.HistoryItem

interface HistoryRepository {
    suspend fun getAllHistory(): List<HistoryItem>
    
    suspend fun insertHistory(
        originalUrl: String,
        finalUrl: String,
        responseTime: Double,
        redirectChain: List<String>?
    ): Long
}
