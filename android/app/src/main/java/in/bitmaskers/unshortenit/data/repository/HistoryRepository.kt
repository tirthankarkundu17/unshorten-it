package `in`.bitmaskers.unshortenit.data.repository

import `in`.bitmaskers.unshortenit.data.model.HistoryItem

interface HistoryRepository {
    suspend fun getAllHistory(): List<HistoryItem>
    
    suspend fun getHistoryByUrl(url: String): HistoryItem?
    
    suspend fun insertHistory(
        originalUrl: String,
        finalUrl: String,
        responseTime: Double,
        redirectChain: List<String>?,
        title: String? = null,
        description: String? = null,
        imageUrl: String? = null
    ): Long

    suspend fun updateHistoryTimestamp(id: Long)
    
    suspend fun clearHistory()
    
    suspend fun deleteHistoryItem(id: Long)
}
