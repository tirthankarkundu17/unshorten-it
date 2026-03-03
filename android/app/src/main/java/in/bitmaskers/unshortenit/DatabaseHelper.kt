package `in`.bitmaskers.unshortenit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HistoryItem(
    val id: Long,
    val originalUrl: String,
    val finalUrl: String,
    val timestamp: Long,
    val responseTime: Double,
    val redirectChain: String
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

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "unshorten_history.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_HISTORY = "history"
        const val COLUMN_ID = "_id"
        const val COLUMN_ORIGINAL_URL = "original_url"
        const val COLUMN_FINAL_URL = "final_url"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_RESPONSE_TIME = "response_time"
        const val COLUMN_REDIRECT_CHAIN = "redirect_chain"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ORIGINAL_URL + " TEXT,"
                + COLUMN_FINAL_URL + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_RESPONSE_TIME + " REAL,"
                + COLUMN_REDIRECT_CHAIN + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY)
        onCreate(db)
    }

    fun insertHistory(
        originalUrl: String, 
        finalUrl: String, 
        responseTime: Double, 
        redirectChain: List<String>?
    ): Long {
        val values = ContentValues()
        values.put(COLUMN_ORIGINAL_URL, originalUrl)
        values.put(COLUMN_FINAL_URL, finalUrl)
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        values.put(COLUMN_RESPONSE_TIME, responseTime)
        values.put(COLUMN_REDIRECT_CHAIN, Gson().toJson(redirectChain ?: emptyList<String>()))

        val db = this.writableDatabase
        return db.insert(TABLE_HISTORY, null, values)
    }

    fun getAllHistory(): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_TIMESTAMP DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val originalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_URL))
                val finalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FINAL_URL))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                
                // For older DB versions where these might be null
                val responseTimeIdx = cursor.getColumnIndex(COLUMN_RESPONSE_TIME)
                val responseTime = if (responseTimeIdx != -1 && !cursor.isNull(responseTimeIdx)) {
                    cursor.getDouble(responseTimeIdx)
                } else 0.0
                
                val chainIdx = cursor.getColumnIndex(COLUMN_REDIRECT_CHAIN)
                val redirectChain = if (chainIdx != -1 && !cursor.isNull(chainIdx)) {
                    cursor.getString(chainIdx)
                } else "[]"
                
                historyList.add(HistoryItem(id, originalUrl, finalUrl, timestamp, responseTime, redirectChain))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return historyList
    }
}
