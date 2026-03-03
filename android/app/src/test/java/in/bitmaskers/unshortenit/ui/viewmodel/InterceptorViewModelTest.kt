package `in`.bitmaskers.unshortenit.ui.viewmodel

import `in`.bitmaskers.unshortenit.data.api.UnshortenResponse
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository
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
class InterceptorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val historyRepository: HistoryRepository = mockk()
    private val unshortenRepository: UnshortenRepository = mockk()

    @Test
    fun `processUrls creates success state after unshortening and inserting in DB`() = runTest {
        // Arrange
        val urls = listOf("http://short.link")
        val fakeResponse = UnshortenResponse(
            originalUrl = "http://short.link",
            finalUrl = "http://destination.link",
            redirectChain = listOf("http://short.link", "http://destination.link"),
            responseTimeMs = 250.0
        )
        coEvery { unshortenRepository.unshortenUrl("http://short.link") } returns Result.success(fakeResponse)
        coEvery { historyRepository.insertHistory(any(), any(), any(), any()) } returns 1L

        val viewModel = InterceptorViewModel(unshortenRepository, historyRepository)

        // Act
        viewModel.processUrls(urls)

        // Assert
        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("http://destination.link", data["http://short.link"]?.getOrNull()?.finalUrl)
    }

    @Test
    fun `processUrls creates success state with failure map when API fails`() = runTest {
        // Arrange
        val urls = listOf("http://bad.link")
        coEvery { unshortenRepository.unshortenUrl("http://bad.link") } returns Result.failure(Exception("API down"))
        
        val viewModel = InterceptorViewModel(unshortenRepository, historyRepository)

        // Act
        viewModel.processUrls(urls)

        // Assert
        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is UiState.Success)
        val data = (state as UiState.Success).data
        assertTrue(data["http://bad.link"]?.isFailure == true)
        assertEquals("API down", data["http://bad.link"]?.exceptionOrNull()?.message)
    }

    @Test
    fun `processUrls finishes with empty list if no URLs provided`() = runTest {
        // Arrange
        val viewModel = InterceptorViewModel(unshortenRepository, historyRepository)

        // Act
        viewModel.processUrls(emptyList())

        // Assert
        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is UiState.Success)
        assertTrue((state as UiState.Success).data.isEmpty())
    }
}
