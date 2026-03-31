package `in`.bitmaskers.unshortenit.data.repository

import android.content.Context
import android.content.SharedPreferences

class AppPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USAGE_COUNT = "USAGE_COUNT"
        private const val TARGET_RATING_COUNT = 1 // Configurable count target
    }

    fun incrementUsageCount() {
        val currentCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        prefs.edit().putInt(KEY_USAGE_COUNT, currentCount + 1).apply()
    }

    fun shouldShowRatePopup(): Boolean {
        val currentCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        // Show after 5 uses
        return currentCount == TARGET_RATING_COUNT
    }
}
