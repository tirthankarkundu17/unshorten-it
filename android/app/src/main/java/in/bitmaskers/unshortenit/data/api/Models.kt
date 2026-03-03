package `in`.bitmaskers.unshortenit.data.api

import com.google.gson.annotations.SerializedName

data class ErrorDetail(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("detail") val detail: String? = null
)

data class ErrorResponse(
    @SerializedName("error") val error: ErrorDetail?,
    @SerializedName("detail") val detail: String? = null
)

data class UnshortenRequest(
    @SerializedName("url") val url: String
)

data class UnshortenResponse(
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("final_url") val finalUrl: String,
    @SerializedName("redirect_chain") val redirectChain: List<String>?,
    @SerializedName("response_time_ms") val responseTimeMs: Double
)
