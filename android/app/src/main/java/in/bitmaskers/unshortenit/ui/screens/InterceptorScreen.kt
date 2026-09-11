package `in`.bitmaskers.unshortenit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.bitmaskers.unshortenit.ui.components.BadgeContainer
import `in`.bitmaskers.unshortenit.ui.components.LabelWithDot
import `in`.bitmaskers.unshortenit.ui.components.UrlBox
import `in`.bitmaskers.unshortenit.ui.viewmodel.InterceptorViewModel
import `in`.bitmaskers.unshortenit.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterceptorScreen(
    urlsToProcess: List<String>,
    viewModel: InterceptorViewModel,
    onFinish: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(true) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(urlsToProcess) {
        viewModel.processUrls(urlsToProcess)
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
                    .fillMaxHeight(0.8f) // ensure it doesn't take full screen to act as bottom sheet but enough for long lists
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

                when (val state = uiState) {
                    is UiState.Loading -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            `in`.bitmaskers.unshortenit.ui.components.ShimmerCard()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Analyzing ${urlsToProcess.size} link(s)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is UiState.Error -> { // In case the entire processing failed
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    is UiState.Success -> {
                        val processedLinks = state.data
                        if (processedLinks.isEmpty()) {
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
                        } else {
                            val context = LocalContext.current
                            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
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
                                            LabelWithDot(text = "Original Link", color = Color(0xFFF59E0B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                originalUrl,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowDownward,
                                                    contentDescription = "To",
                                                    modifier = Modifier.size(20.dp).padding(start = 2.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))

                                            result.onSuccess { response ->
                                                if (response.security?.isSafe == false) {
                                                    Surface(
                                                        color = if (isDark) Color(0xFF331014) else Color(0xFFFEF2F2),
                                                        shape = RoundedCornerShape(14.dp),
                                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(14.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Warning,
                                                                contentDescription = "Security Alert",
                                                                tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column {
                                                                Text(
                                                                    text = "Security Warning",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                                                    fontSize = 14.sp
                                                                )
                                                                Text(
                                                                    text = "Flagged as ${response.security?.threatType?.replace("_", " ") ?: "a threat"}. Do not visit.",
                                                                    color = if (isDark) Color(0xFFF87171) else Color(0xFF7F1D1D),
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(14.dp))
                                                }

                                                LabelWithDot(text = "Full URL", color = Color(0xFF10B981))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                UrlBox(
                                                    url = response.finalUrl,
                                                    backgroundColor = if (isDark) Color(0xFF062817) else Color(0xFFF0FDF4),
                                                    borderColor = if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0),
                                                    textColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF1E293B),
                                                    label = "Full URL"
                                                )

                                                if (response.cleanedUrl != response.finalUrl) {
                                                    Spacer(modifier = Modifier.height(14.dp))
                                                    LabelWithDot(text = "Cleaned URL (Trackers Removed)", color = Color(0xFF3B82F6))
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    UrlBox(
                                                        url = response.cleanedUrl,
                                                        backgroundColor = if (isDark) Color(0xFF0B2240) else Color(0xFFEFF6FF),
                                                        borderColor = if (isDark) Color(0xFF1E40AF) else Color(0xFFBFDBFE),
                                                        textColor = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E293B),
                                                        label = "Cleaned URL"
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

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
                                            }.onFailure { error ->
                                                Text(
                                                    "Failed to unshorten",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    error.message ?: "Unknown Error",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
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
