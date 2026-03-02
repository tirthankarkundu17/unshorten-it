package `in`.bitmaskers.unshortenit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Replace with your actual backend URL or use 10.0.2.2 for local emulator
    private val backendUrl = "https://unshortenit-backend.onrender.com/api/v1/unshorten"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var incomingUrl = intent?.data?.toString() ?: intent?.getStringExtra(Intent.EXTRA_TEXT)

        // If we got the text from a Share intent, it might contain extra text (e.g. "Check this out: https://bit.ly/xyz")
        // Try to extract just the URL using a regex.
        if (incomingUrl != null && !incomingUrl.startsWith("http")) {
            val urlRegex = "(https?://[a-zA-Z0-9./_?=&-]+)".toRegex()
            val match = urlRegex.find(incomingUrl)
            if (match != null) {
                incomingUrl = match.value
            } else {
                incomingUrl = "http://$incomingUrl" // Fallback, let the backend try
            }
        }

        setContent {
            MyApplicationTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var expandedUrl by remember { mutableStateOf<String?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(incomingUrl != null) }
                var showSheet by remember { mutableStateOf(true) }

                val scope = rememberCoroutineScope()

                LaunchedEffect(incomingUrl) {
                    if (incomingUrl != null) {
                        scope.launch {
                            try {
                                val result = unshortenUrl(incomingUrl)
                                expandedUrl = result
                            } catch (e: Exception) {
                                error = e.localizedMessage ?: "Failed to unshorten link"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        isLoading = false
                        error = "No URL provided."
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
                                Text("Analyzing link...")
                            } else if (error != null) {
                                Text(
                                    text = "Error: $error",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { finish() }) {
                                    Text("Close")
                                }
                            } else {
                                Text("Original Link:", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    incomingUrl ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Safe Destination:", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    expandedUrl ?: "Unknown",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(onClick = { finish() }) {
                                        Text("Cancel")
                                    }
                                    Button(onClick = {
                                        expandedUrl?.let {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                            startActivity(browserIntent)
                                            finish()
                                        }
                                    }) {
                                        Text("Open Link")
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
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                var errorMessage = "Server returned ${response.code}"
                try {
                    if (!errorBody.isNullOrEmpty()) {
                        val errorJson = JSONObject(errorBody)
                        val errorObject = errorJson.optJSONObject("error")
                        if (errorObject != null) {
                            errorMessage = errorObject.optString("message", errorMessage)
                            if (errorObject.has("detail")) {
                                errorMessage = errorObject.optString("detail", errorMessage)
                            }
                        } else {
                            errorMessage = errorJson.optString("detail", errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    // fallback to the default error
                }
                throw Exception(errorMessage)
            }
            
            val responseBody = response.body?.string()
            if (responseBody == null) {
                throw Exception("Empty response body")
            }
            
            val jsonResponse = JSONObject(responseBody)
            val destinations = jsonResponse.optJSONArray("redirect_chain")
            if (destinations != null && destinations.length() > 0) {
                // The API changed; redirect_chain is now a List[str], not a List containing JSONObjects
                val lastDest = destinations.getString(destinations.length() - 1)
                return@withContext lastDest
            }
            
            // Try to falback if unshorten-it API shape has changed
            return@withContext jsonResponse.optString("final_url", shortUrl)
        }
    }
}