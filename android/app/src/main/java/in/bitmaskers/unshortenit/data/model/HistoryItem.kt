package `in`.bitmaskers.unshortenit.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HistoryItem(
    val id: Long,
    val originalUrl: String,
    val finalUrl: String,
    val cleanedUrl: String,
    val timestamp: Long,
    val responseTime: Double,
    val redirectChain: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val isSafe: Boolean = true,
    val threatType: String? = null
) {
    fun getChainList(): List<String> {
        if (redirectChain.isEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(redirectChain, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
