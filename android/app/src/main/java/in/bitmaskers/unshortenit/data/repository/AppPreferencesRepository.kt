package `in`.bitmaskers.unshortenit.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class AppPreferencesRepository(
    context: Context,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USAGE_COUNT = "USAGE_COUNT"
        private const val KEY_LAST_PROMPT_TIME = "LAST_PROMPT_TIME"
        private const val TARGET_RATING_COUNT = 5 // Configurable count target
        const val MIN_DAYS_BETWEEN_PROMPTS = 10L
        const val MIN_TIME_BETWEEN_PROMPTS_MS = MIN_DAYS_BETWEEN_PROMPTS * 24 * 60 * 60 * 1000
    }

    fun incrementUsageCount() {
        val currentCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        
        // Loop it back to 1 if it has surpassed the target rating count previously
        val nextCount = if (currentCount >= TARGET_RATING_COUNT) 1 else currentCount + 1
        
        prefs.edit { putInt(KEY_USAGE_COUNT, nextCount) }
    }

    fun markRatingPromptShown() {
        prefs.edit { putLong(KEY_LAST_PROMPT_TIME, timeProvider()) }
    }

    fun shouldShowRatePopup(): Boolean {
        val currentCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        val currentTime = timeProvider()

        // Show exactly when it hits the target (loops each cycle)
        val isTargetReached = currentCount == TARGET_RATING_COUNT
        // Check if the 10 days gap has been surpassed
        val isEnoughTimePassed = (currentTime - lastPromptTime) >= MIN_TIME_BETWEEN_PROMPTS_MS
        
        Log.d("AppPrefsRepo", "Rate popup check - isTargetReached: $isTargetReached (count: $currentCount), isEnoughTimePassed: $isEnoughTimePassed")
        
        return isTargetReached && isEnoughTimePassed
    }
}
