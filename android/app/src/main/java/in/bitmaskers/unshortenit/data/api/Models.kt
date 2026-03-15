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

data class PagePreview(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class SecurityCheck(
    @SerializedName("is_safe") val isSafe: Boolean,
    @SerializedName("threat_type") val threatType: String? = null
)

data class UnshortenResponse(
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("final_url") val finalUrl: String,
    @SerializedName("cleaned_url") val cleanedUrl: String,
    @SerializedName("redirect_chain") val redirectChain: List<String>?,
    @SerializedName("response_time_ms") val responseTimeMs: Double,
    @SerializedName("cached") val cached: Boolean = false,
    @SerializedName("preview") val preview: PagePreview? = null,
    @SerializedName("security") val security: SecurityCheck? = null
)
