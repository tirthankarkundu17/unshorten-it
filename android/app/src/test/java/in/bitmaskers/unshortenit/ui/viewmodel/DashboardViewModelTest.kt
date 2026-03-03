package `in`.bitmaskers.unshortenit.ui.viewmodel

import `in`.bitmaskers.unshortenit.data.model.HistoryItem
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import `in`.bitmaskers.unshortenit.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val historyRepository: HistoryRepository = mockk()
    private val unshortenRepository: `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository = mockk()

    @Test
    fun `loadHistory success updates UI state to Success with data`() = runTest {
        // Arrange
        val mockItems = listOf(
            HistoryItem(1L, "http://short.url", "http://long.url", 12345L, 100.0, "[]")
        )
        coEvery { historyRepository.getAllHistory() } returns mockItems

        // Act
        val viewModel = DashboardViewModel(historyRepository, unshortenRepository)

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is UiState.Success)
        assertEquals(mockItems, (state as UiState.Success).data)
    }

    @Test
    fun `loadHistory error updates UI state to Error`() = runTest {
        // Arrange
        val errorMessage = "Database error"
        coEvery { historyRepository.getAllHistory() } throws RuntimeException(errorMessage)

        // Act
        val viewModel = DashboardViewModel(historyRepository, unshortenRepository)

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).message)
    }

    @Test
    fun `loadHistory refresh flag skips Loading state update`() = runTest {
        // Arrange
        val mockItems = listOf(
            HistoryItem(1L, "http://another.url", "http://long.val", 123456L, 120.0, "[]")
        )
        coEvery { historyRepository.getAllHistory() } returns mockItems
        val viewModel = DashboardViewModel(historyRepository, unshortenRepository)
        // Ensure state is fully resolved by Unconfined dispatcher from init

        coEvery { historyRepository.getAllHistory() } returns emptyList()
        
        // Act
        viewModel.loadHistory(isRefresh = true)

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(emptyList<HistoryItem>(), (state as UiState.Success).data)
    }
}
