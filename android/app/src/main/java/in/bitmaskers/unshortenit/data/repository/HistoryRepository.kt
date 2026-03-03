package `in`.bitmaskers.unshortenit.data.repository

import `in`.bitmaskers.unshortenit.DatabaseHelper
import `in`.bitmaskers.unshortenit.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(private val dbHelper: DatabaseHelper) {
    suspend fun getAllHistory(): List<HistoryItem> = withContext(Dispatchers.IO) {
        dbHelper.getAllHistory()
    }

    suspend fun insertHistory(
        originalUrl: String,
        finalUrl: String,
        responseTime: Double,
        redirectChain: List<String>?
    ) = withContext(Dispatchers.IO) {
        dbHelper.insertHistory(originalUrl, finalUrl, responseTime, redirectChain)
    }
}
