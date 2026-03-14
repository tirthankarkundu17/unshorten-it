package `in`.bitmaskers.unshortenit.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import `in`.bitmaskers.unshortenit.data.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), HistoryRepository {

    companion object {
        private const val DATABASE_NAME = "unshorten_history.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_HISTORY = "history"
        const val COLUMN_ID = "_id"
        const val COLUMN_ORIGINAL_URL = "original_url"
        const val COLUMN_FINAL_URL = "final_url"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_RESPONSE_TIME = "response_time"
        const val COLUMN_REDIRECT_CHAIN = "redirect_chain"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_IMAGE_URL = "image_url"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ORIGINAL_URL + " TEXT,"
                + COLUMN_FINAL_URL + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_RESPONSE_TIME + " REAL,"
                + COLUMN_REDIRECT_CHAIN + " TEXT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_IMAGE_URL + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY)
        onCreate(db)
    }

    override suspend fun insertHistory(
        originalUrl: String,
        finalUrl: String,
        responseTime: Double,
        redirectChain: List<String>?,
        title: String?,
        description: String?,
        imageUrl: String?
    ): Long = withContext(Dispatchers.IO) {
        val values = ContentValues()
        values.put(COLUMN_ORIGINAL_URL, originalUrl)
        values.put(COLUMN_FINAL_URL, finalUrl)
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        values.put(COLUMN_RESPONSE_TIME, responseTime)
        values.put(COLUMN_REDIRECT_CHAIN, Gson().toJson(redirectChain ?: emptyList<String>()))
        values.put(COLUMN_TITLE, title)
        values.put(COLUMN_DESCRIPTION, description)
        values.put(COLUMN_IMAGE_URL, imageUrl)

        val db = writableDatabase
        db.insert(TABLE_HISTORY, null, values)
    }

    override suspend fun getHistoryByUrl(url: String): HistoryItem? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_HISTORY WHERE $COLUMN_ORIGINAL_URL = ? ORDER BY $COLUMN_TIMESTAMP DESC LIMIT 1",
            arrayOf(url)
        )

        var item: HistoryItem? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val finalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FINAL_URL))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

            val responseTimeIdx = cursor.getColumnIndex(COLUMN_RESPONSE_TIME)
            val responseTime = if (responseTimeIdx != -1 && !cursor.isNull(responseTimeIdx)) {
                cursor.getDouble(responseTimeIdx)
            } else 0.0

            val chainIdx = cursor.getColumnIndex(COLUMN_REDIRECT_CHAIN)
            val redirectChain = if (chainIdx != -1 && !cursor.isNull(chainIdx)) {
                cursor.getString(chainIdx)
            } else "[]"

            val titleIdx = cursor.getColumnIndex(COLUMN_TITLE)
            val title = if (titleIdx != -1 && !cursor.isNull(titleIdx)) cursor.getString(titleIdx) else null

            val descIdx = cursor.getColumnIndex(COLUMN_DESCRIPTION)
            val description = if (descIdx != -1 && !cursor.isNull(descIdx)) cursor.getString(descIdx) else null

            val imgIdx = cursor.getColumnIndex(COLUMN_IMAGE_URL)
            val imageUrl = if (imgIdx != -1 && !cursor.isNull(imgIdx)) cursor.getString(imgIdx) else null

            item = HistoryItem(id, url, finalUrl, timestamp, responseTime, redirectChain, title, description, imageUrl)
        }
        cursor.close()
        item
    }

    override suspend fun updateHistoryTimestamp(id: Long) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            }
            val db = writableDatabase
            db.update(
                TABLE_HISTORY,
                values,
                "$COLUMN_ID = ?",
                arrayOf(id.toString())
            )
        }
    }

    override suspend fun getAllHistory(): List<HistoryItem> = withContext(Dispatchers.IO) {
        val historyList = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_TIMESTAMP DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val originalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_URL))
                val finalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FINAL_URL))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

                val responseTimeIdx = cursor.getColumnIndex(COLUMN_RESPONSE_TIME)
                val responseTime = if (responseTimeIdx != -1 && !cursor.isNull(responseTimeIdx)) {
                    cursor.getDouble(responseTimeIdx)
                } else 0.0

                val chainIdx = cursor.getColumnIndex(COLUMN_REDIRECT_CHAIN)
                val redirectChain = if (chainIdx != -1 && !cursor.isNull(chainIdx)) {
                    cursor.getString(chainIdx)
                } else "[]"

                val titleIdx = cursor.getColumnIndex(COLUMN_TITLE)
                val title = if (titleIdx != -1 && !cursor.isNull(titleIdx)) cursor.getString(titleIdx) else null

                val descIdx = cursor.getColumnIndex(COLUMN_DESCRIPTION)
                val description = if (descIdx != -1 && !cursor.isNull(descIdx)) cursor.getString(descIdx) else null

                val imgIdx = cursor.getColumnIndex(COLUMN_IMAGE_URL)
                val imageUrl = if (imgIdx != -1 && !cursor.isNull(imgIdx)) cursor.getString(imgIdx) else null

                historyList.add(HistoryItem(id, originalUrl, finalUrl, timestamp, responseTime, redirectChain, title, description, imageUrl))
            } while (cursor.moveToNext())
        }
        cursor.close()
        historyList
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_HISTORY, null, null)
        }
    }

    override suspend fun deleteHistoryItem(id: Long) {
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_HISTORY, "$COLUMN_ID = ?", arrayOf(id.toString()))
        }
    }
}
