package `in`.bitmaskers.unshortenit.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import `in`.bitmaskers.unshortenit.utils.SharedPrefsKeys.APP_PREFS_NAME

class AppPreferencesRepository(
    context: Context,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NEVER_SHOW_REVIEW = "NEVER_SHOW_REVIEW"
        private const val KEY_LAST_PROMPT_TIME = "LAST_PROMPT_TIME"
        private const val MIN_DAYS_BETWEEN_PROMPTS = 7L
        const val MIN_TIME_BETWEEN_PROMPTS_MS = MIN_DAYS_BETWEEN_PROMPTS * 24 * 60 * 60 * 1000
    }

    fun setNeverShowReviewAgain(neverShow: Boolean) {
        prefs.edit { putBoolean(KEY_NEVER_SHOW_REVIEW, neverShow) }
    }

    fun isNeverShowReviewAgain(): Boolean {
        return prefs.getBoolean(KEY_NEVER_SHOW_REVIEW, false)
    }

    fun markRatingPromptShown() {
        prefs.edit { putLong(KEY_LAST_PROMPT_TIME, timeProvider()) }
    }

    fun shouldShowRatePopup(): Boolean {
        if (isNeverShowReviewAgain()) {
            return false
        }
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        val currentTime = timeProvider()
        val isEnoughTimePassed = (currentTime - lastPromptTime) >= MIN_TIME_BETWEEN_PROMPTS_MS

        Log.d("AppPrefsRepo", "Rate popup check - lastPromptTime: $lastPromptTime, isEnoughTimePassed: $isEnoughTimePassed")
        return isEnoughTimePassed
    }
}
