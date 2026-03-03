package `in`.bitmaskers.unshortenit.data.api

import `in`.bitmaskers.unshortenit.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Add trailing slash in case the URL doesn't have it
    private val baseUrl = if (BuildConfig.BACKEND_URL.endsWith("/")) {
        BuildConfig.BACKEND_URL
    } else {
        BuildConfig.BACKEND_URL + "/"
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val unshortenService: UnshortenApiService by lazy {
        retrofit.create(UnshortenApiService::class.java)
    }
}
