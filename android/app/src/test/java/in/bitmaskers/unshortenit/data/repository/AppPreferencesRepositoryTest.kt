package `in`.bitmaskers.unshortenit.data.repository

import android.content.Context
import android.content.SharedPreferences
import `in`.bitmaskers.unshortenit.utils.SharedPrefsKeys.APP_PREFS_NAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: AppPreferencesRepository

    private var mockCurrentTime: Long = 0L

    // In-memory map to store mock preferences
    private val mockPrefsMap = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0

        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        // Mock Boolean getting/setting
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            val key = args[0] as String
            val default = args[1] as Boolean
            mockPrefsMap[key] as? Boolean ?: default
        }
        every { editor.putBoolean(any(), any()) } answers {
            val key = args[0] as String
            val value = args[1] as Boolean
            mockPrefsMap[key] = value
            editor
        }

        // Mock Long getting/setting
        every { sharedPreferences.getLong(any(), any()) } answers {
            val key = args[0] as String
            val default = args[1] as Long
            mockPrefsMap[key] as? Long ?: default
        }
        every { editor.putLong(any(), any()) } answers {
            val key = args[0] as String
            val value = args[1] as Long
            mockPrefsMap[key] = value
            editor
        }

        repository = AppPreferencesRepository(
            context = context,
            timeProvider = { mockCurrentTime }
        )
    }

    @Test
    fun `shouldShowRatePopup - true when neverShowAgain is false and no previous prompt`() {
        mockPrefsMap["NEVER_SHOW_REVIEW"] = false
        mockPrefsMap.remove("LAST_PROMPT_TIME")
        
        mockCurrentTime = AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS + 1000L
        
        assertTrue(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - false when neverShowAgain is false but not enough time passed`() {
        mockPrefsMap["NEVER_SHOW_REVIEW"] = false
        mockPrefsMap["LAST_PROMPT_TIME"] = 1000L
        
        mockCurrentTime = 1000L + (AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS / 2)
        
        assertFalse(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - true when neverShowAgain is false and exactly 10 days passed`() {
        mockPrefsMap["NEVER_SHOW_REVIEW"] = false
        mockPrefsMap["LAST_PROMPT_TIME"] = 1000L
        
        mockCurrentTime = 1000L + AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS
        
        assertTrue(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - false when neverShowAgain is true`() {
        mockPrefsMap["NEVER_SHOW_REVIEW"] = true
        mockPrefsMap.remove("LAST_PROMPT_TIME")
        
        mockCurrentTime = AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS + 1000L
        
        assertFalse(repository.shouldShowRatePopup())
    }

    @Test
    fun `setNeverShowReviewAgain - saves correct value`() {
        repository.setNeverShowReviewAgain(true)
        verify { editor.putBoolean("NEVER_SHOW_REVIEW", true) }
        assertTrue(mockPrefsMap["NEVER_SHOW_REVIEW"] as Boolean)

        repository.setNeverShowReviewAgain(false)
        verify { editor.putBoolean("NEVER_SHOW_REVIEW", false) }
        assertFalse(mockPrefsMap["NEVER_SHOW_REVIEW"] as Boolean)
    }

    @Test
    fun `isNeverShowReviewAgain - returns correct value`() {
        mockPrefsMap["NEVER_SHOW_REVIEW"] = true
        assertTrue(repository.isNeverShowReviewAgain())

        mockPrefsMap["NEVER_SHOW_REVIEW"] = false
        assertFalse(repository.isNeverShowReviewAgain())
    }

    @Test
    fun `markRatingPromptShown - saves current time`() {
        mockCurrentTime = 999888777666L
        repository.markRatingPromptShown()
        
        verify { editor.putLong("LAST_PROMPT_TIME", 999888777666L) }
        assertEquals(999888777666L, mockPrefsMap["LAST_PROMPT_TIME"])
    }
}
