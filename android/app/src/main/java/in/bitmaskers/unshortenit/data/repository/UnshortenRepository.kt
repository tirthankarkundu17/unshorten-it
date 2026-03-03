package `in`.bitmaskers.unshortenit.data.repository

import com.google.gson.Gson
import `in`.bitmaskers.unshortenit.data.api.ApiClient
import `in`.bitmaskers.unshortenit.data.api.ErrorResponse
import `in`.bitmaskers.unshortenit.data.api.UnshortenRequest
import `in`.bitmaskers.unshortenit.data.api.UnshortenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnshortenRepository {
    private val apiService = ApiClient.unshortenService
    private val gson = Gson()

    suspend fun unshortenUrl(url: String): Result<UnshortenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.unshortenUrl(UnshortenRequest(url))
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                var errorMessage = "Server returned ${response.code()}"
                try {
                    if (!errorBody.isNullOrEmpty()) {
                        val errorJson = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorMessage = errorJson.error?.detail
                            ?: errorJson.error?.message
                                    ?: errorJson.detail
                                    ?: errorMessage
                    }
                } catch (e: Exception) {
                    // Fallback to basic message
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
