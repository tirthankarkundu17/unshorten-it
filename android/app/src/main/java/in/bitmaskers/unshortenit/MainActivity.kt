package `in`.bitmaskers.unshortenit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import `in`.bitmaskers.unshortenit.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val backendUrl = BuildConfig.BACKEND_URL + "/api/v1/unshorten"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var extractedUrls = emptyList<String>()
        val incomingText = intent?.data?.toString() ?: intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (incomingText != null) {
            val urlRegex = "(https?://[a-zA-Z0-9./_?=&-]+)".toRegex()
            extractedUrls = urlRegex.findAll(incomingText).map { it.value }.toList()
        }

        val dbHelper = DatabaseHelper(this)

        setContent {
            MyApplicationTheme {
                if (extractedUrls.isEmpty()) {
                    DashboardScreen(dbHelper, onFinish = { finish() })
                } else {
                    InterceptorScreen(extractedUrls, dbHelper, onFinish = { finish() })
                }
            }
        }
    }

    @androidx.compose.ui.tooling.preview.Preview(showBackground = true)
    @Composable
    fun DashboardScreenPreview() {
        val mockItems = listOf(
            HistoryItem(
                id = 1,
                originalUrl = "https://bit.ly/456",
                finalUrl = "https://example.com/very/long/destination/path",
                timestamp = System.currentTimeMillis(),
                responseTime = 250.0,
                redirectChain = "[\"https://bit.ly/456\", \"https://example.com/very/long/destination/path\"]"
            ),
             HistoryItem(
                id = 2,
                originalUrl = "https://t.co/abc",
                finalUrl = "https://github.com/tirthankarkundu17",
                timestamp = System.currentTimeMillis() - 86400000,
                responseTime = 120.0,
                redirectChain = "[]"
            )
        )
        MyApplicationTheme {
            DashboardScreen(mockItems) {}
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(dbHelper: DatabaseHelper, onFinish: () -> Unit) {
        val historyItems = remember { mutableStateListOf<HistoryItem>() }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val dbItems = dbHelper.getAllHistory()
                historyItems.clear()
                historyItems.addAll(dbItems)
            }
        }
        DashboardScreen(historyItems, onFinish)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(historyItems: List<HistoryItem>, onFinish: () -> Unit) {

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "URL History",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No unshortened links yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 24.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryCard(item: HistoryItem) {
        val context = LocalContext.current
        val dateFormat =
            remember { SimpleDateFormat("MMM dd, yyyy \u2022 HH:mm", Locale.getDefault()) }
        val chainList = remember(item.redirectChain) { item.getChainList() }
        var isExpanded by remember { mutableStateOf(false) }

        ElevatedCard(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 8.dp
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header (Time and Options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chainList.isNotEmpty()) {
                            BadgeContainer(
                                icon = Icons.Rounded.Cable,
                                text = "${chainList.size} Hops",
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (item.responseTime > 0) {
                            BadgeContainer(
                                icon = Icons.Rounded.Timer,
                                text = "${item.responseTime.toInt()}ms",
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URLs
                Text(
                    text = item.finalUrl,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = "From",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.originalUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expanded Section
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (chainList.isNotEmpty()) {
                            Text(
                                "Redirect Chain",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            chainList.forEachIndexed { index, hop ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        hop,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                val browserIntent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(item.finalUrl))
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.OpenInNew,
                                contentDescription = "Open",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Destination")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BadgeContainer(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        color: Color,
        contentColor: Color
    ) {
        Row(
            modifier = Modifier
                .background(color, shape = CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun InterceptorScreen(
        extractedUrls: List<String>,
        dbHelper: DatabaseHelper,
        onFinish: () -> Unit
    ) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var processedLinks by remember {
            mutableStateOf<Map<String, Result<UnshortenResponse>>>(
                emptyMap()
            )
        }
        var isLoading by remember { mutableStateOf(extractedUrls.isNotEmpty()) }
        var showSheet by remember { mutableStateOf(true) }

        val scope = rememberCoroutineScope()

        LaunchedEffect(extractedUrls) {
            if (extractedUrls.isNotEmpty()) {
                scope.launch {
                    val tempResults = mutableMapOf<String, Result<UnshortenResponse>>()
                    extractedUrls.forEach { url ->
                        try {
                            val response = unshortenUrl(url)
                            withContext(Dispatchers.IO) {
                                dbHelper.insertHistory(
                                    url,
                                    response.finalUrl,
                                    response.responseTimeMs,
                                    response.redirectChain
                                )
                            }
                            tempResults[url] = Result.success(response)
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
                    onFinish()
                },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "URL Unshortener",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Analyzing ${extractedUrls.size} link(s)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (extractedUrls.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No links were found in the shared text.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { onFinish() }) {
                            Text("Close")
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            processedLinks.forEach { (originalUrl, result) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Original Link",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            originalUrl,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        result.onSuccess { response ->
                                            Text(
                                                "Safe Destination",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                response.finalUrl,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!response.redirectChain.isNullOrEmpty()) {
                                                    BadgeContainer(
                                                        icon = Icons.Rounded.Cable,
                                                        text = "${response.redirectChain.size} Hops",
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                                if (response.responseTimeMs > 0) {
                                                    BadgeContainer(
                                                        icon = Icons.Rounded.Timer,
                                                        text = "${response.responseTimeMs.toInt()}ms",
                                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    val browserIntent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(response.finalUrl)
                                                    )
                                                    startActivity(browserIntent)
                                                    onFinish()
                                                },
                                                modifier = Modifier.align(Alignment.End),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.OpenInNew,
                                                    contentDescription = "Open Link",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Open Link")
                                            }
                                        }.onFailure { error ->
                                            Text(
                                                "Failed to unshorten",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                error.localizedMessage ?: "Unknown Error",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { onFinish() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    private suspend fun unshortenUrl(shortUrl: String): UnshortenResponse =
        withContext(Dispatchers.IO) {
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val jsonObject = JSONObject().apply {
                put("url", shortUrl)
            }
            val requestBody = jsonObject.toString().toRequestBody(jsonMediaType)

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

                return@withContext pojoResponse
            }
        }
}