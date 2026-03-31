package `in`.bitmaskers.unshortenit.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
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
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        // Mock Int getting/setting
        every { sharedPreferences.getInt(any(), any()) } answers {
            val key = args[0] as String
            val default = args[1] as Int
            mockPrefsMap[key] as? Int ?: default
        }
        every { editor.putInt(any(), any()) } answers {
            val key = args[0] as String
            val value = args[1] as Int
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
    fun `incrementUsageCount - from 0 increments to 1`() {
        mockPrefsMap.clear() // Start fresh
        repository.incrementUsageCount()
        assertEquals(1, mockPrefsMap["USAGE_COUNT"])
    }

    @Test
    fun `incrementUsageCount - resets to 1 after surpassing target rating count`() {
        // Start count at TARGET_RATING_COUNT (e.g. 1 in current code)
        mockPrefsMap["USAGE_COUNT"] = 1
        repository.incrementUsageCount()
        
        // Next logic says: if >= target (1), nextCount is 1.
        assertEquals(1, mockPrefsMap["USAGE_COUNT"])
    }

    @Test
    fun `shouldShowRatePopup - false when target not reached`() {
        mockPrefsMap["USAGE_COUNT"] = 0 // not 1
        assertFalse(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - true when target reached and no previous prompt`() {
        mockPrefsMap["USAGE_COUNT"] = 1
        mockPrefsMap.remove("LAST_PROMPT_TIME") // 0 by default
        
        mockCurrentTime = AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS + 1000L
        
        assertTrue(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - false when target reached but not enough time passed`() {
        mockPrefsMap["USAGE_COUNT"] = 1
        mockPrefsMap["LAST_PROMPT_TIME"] = 1000L
        
        // Wait only half a day (less than MIN_TIME_BETWEEN_PROMPTS_MS)
        mockCurrentTime = 1000L + (AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS / 2)
        
        assertFalse(repository.shouldShowRatePopup())
    }

    @Test
    fun `shouldShowRatePopup - true when target reached and exactly 10 days passed`() {
        mockPrefsMap["USAGE_COUNT"] = 1
        mockPrefsMap["LAST_PROMPT_TIME"] = 1000L
        
        mockCurrentTime = 1000L + AppPreferencesRepository.MIN_TIME_BETWEEN_PROMPTS_MS
        
        assertTrue(repository.shouldShowRatePopup())
    }

    @Test
    fun `markRatingPromptShown - saves current time`() {
        mockCurrentTime = 999888777666L
        repository.markRatingPromptShown()
        
        verify { editor.putLong("LAST_PROMPT_TIME", 999888777666L) }
        assertEquals(999888777666L, mockPrefsMap["LAST_PROMPT_TIME"])
    }
}
