package `in`.bitmaskers.unshortenit.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UnshortenApiService {
    @POST("api/v1/unshorten")
    suspend fun unshortenUrl(@Body request: UnshortenRequest): Response<UnshortenResponse>
}
