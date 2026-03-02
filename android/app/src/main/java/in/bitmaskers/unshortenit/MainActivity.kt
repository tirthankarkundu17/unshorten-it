package `in`.bitmaskers.unshortenit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.bitmaskers.unshortenit.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
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

data class UnshortenResponse(
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("final_url") val finalUrl: String,
    @SerializedName("redirect_chain") val redirectChain: List<String>?,
    @SerializedName("response_time_ms") val responseTimeMs: Double
)

class MainActivity : ComponentActivity() {
    
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val backendUrl = "https://unshortenit-backend.onrender.com/api/v1/unshorten"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var extractedUrls = emptyList<String>()
        val incomingText = intent?.data?.toString() ?: intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (incomingText != null) {
            val urlRegex = "(https?://[a-zA-Z0-9./_?=&-]+)".toRegex()
            extractedUrls = urlRegex.findAll(incomingText).map { it.value }.toList()
        }

        setContent {
            MyApplicationTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                // We'll store a map of originalUrl -> Result (either Safe URL or Error)
                var processedLinks by remember { mutableStateOf<Map<String, Result<String>>>(emptyMap()) }
                var isLoading by remember { mutableStateOf(extractedUrls.isNotEmpty()) }
                var showSheet by remember { mutableStateOf(true) }

                val scope = rememberCoroutineScope()

                LaunchedEffect(extractedUrls) {
                    if (extractedUrls.isNotEmpty()) {
                        scope.launch {
                            val tempResults = mutableMapOf<String, Result<String>>()
                            // Unshorten all URLs
                            extractedUrls.forEach { url ->
                                try {
                                    val safeDest = unshortenUrl(url)
                                    tempResults[url] = Result.success(safeDest)
                                } catch (e: Exception) {
                                    tempResults[url] = Result.failure(e)
                                }
                            }
                            processedLinks = tempResults
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showSheet = false
                            finish()
                        },
                        sheetState = sheetState,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Unshorten It!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (isLoading) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Analyzing ${extractedUrls.size} link(s)...")
                            } else if (extractedUrls.isEmpty()) {
                                Text(
                                    text = "Error: No links found in text.",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { finish() }) {
                                    Text("Close")
                                }
                            } else {
                                // Display all discovered links
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    processedLinks.forEach { (originalUrl, result) ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Original Link:", style = MaterialTheme.typography.labelSmall)
                                                Text(originalUrl, style = MaterialTheme.typography.bodySmall)

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Safe Destination:", style = MaterialTheme.typography.labelMedium)
                                                
                                                result.onSuccess { safeUrl ->
                                                    Text(
                                                        safeUrl,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(
                                                        onClick = {
                                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                                            startActivity(browserIntent)
                                                            finish()
                                                        },
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        Text("Open Link")
                                                    }
                                                }.onFailure { error ->
                                                    Text(
                                                        "Error: ${error.localizedMessage}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(onClick = { finish() }) {
                                        Text("Close")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    private suspend fun unshortenUrl(shortUrl: String): String = withContext(Dispatchers.IO) {
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val jsonObject = JSONObject().apply {
            put("url", shortUrl)
        }
        val requestBody = jsonObject.toString().toRequestBody(jsonMediaType)
        print(requestBody.toString())

        val request = Request.Builder()
            .url(backendUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                var errorMessage = "Server returned ${response.code}"
                try {
                    if (!responseBody.isNullOrEmpty()) {
                        val errorJson = gson.fromJson(responseBody, ErrorResponse::class.java)
                        errorMessage = errorJson.error?.detail 
                            ?: errorJson.error?.message 
                            ?: errorJson.detail 
                            ?: errorMessage
                    }
                } catch (e: Exception) {
                    // fallback to the default error
                }
                throw Exception(errorMessage)
            }
            
            if (responseBody == null) {
                throw Exception("Empty response body")
            }
            
            Log.d("UnshortenIt", "Server Response: $responseBody")
            val pojoResponse = gson.fromJson(responseBody, UnshortenResponse::class.java)
            
            // Try to falback if unshorten-it API shape has changed
            return@withContext pojoResponse.finalUrl
        }
    }
}