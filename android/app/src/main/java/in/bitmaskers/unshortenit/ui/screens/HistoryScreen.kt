package `in`.bitmaskers.unshortenit.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.bitmaskers.unshortenit.data.model.HistoryItem
import `in`.bitmaskers.unshortenit.ui.viewmodel.DashboardViewModel
import `in`.bitmaskers.unshortenit.ui.viewmodel.UiState
import `in`.bitmaskers.unshortenit.ui.components.UrlBox
import `in`.bitmaskers.unshortenit.ui.components.UrlFlowConnector
import `in`.bitmaskers.unshortenit.ui.components.LabelWithDot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: DashboardViewModel, innerPadding: PaddingValues) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Loading) {
            isRefreshing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Redesigned History Header (Local) to match Dashboard theme
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "History",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val count = if (uiState is UiState.Success) (uiState as UiState.Success).data.size else 0
                    Text(
                        text = "$count URLs unshortened",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                
                if (uiState is UiState.Success && (uiState as UiState.Success).data.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear History",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear History") },
                text = { Text("Are you sure you want to clear all history? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearHistory()
                            showClearDialog = false
                        }
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // History Content
        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4F46E5))
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No history yet",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.loadHistory(isRefresh = true)
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.data, key = { it.id }) { item ->
                                    HistoryCard(item, onDelete = {
                                        viewModel.deleteHistoryItem(item.id)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun HistoryCard(item: HistoryItem, onDelete: (() -> Unit)? = null) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Unshortened Link",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getTimeAgo(item.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Short URL
            LabelWithDot(text = "Short URL", color = Color(0xFFF59E0B))
            Spacer(modifier = Modifier.height(6.dp))
            UrlBox(
                url = item.originalUrl,
                backgroundColor = if (isDark) Color(0xFF261D08) else Color(0xFFFFFBEB),
                borderColor = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A),
                textColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF1E293B),
                label = "Short URL"
            )

            // Flow Connector
            UrlFlowConnector()

            // Destination / Full URL
            LabelWithDot(text = "Destination URL", color = Color(0xFF10B981))
            Spacer(modifier = Modifier.height(6.dp))
            UrlBox(
                url = item.finalUrl,
                backgroundColor = if (isDark) Color(0xFF062817) else Color(0xFFF0FDF4),
                borderColor = if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0),
                textColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF1E293B),
                label = "Destination URL"
            )

            if (!item.isSafe) {
                Spacer(modifier = Modifier.height(14.dp))
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
                            contentDescription = "Threat Warning",
                            tint = Color(0xFFEF4444),
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
                                text = "Flagged as ${item.threatType?.replace("_", " ") ?: "a threat"}. Do not visit.",
                                color = if (isDark) Color(0xFFF87171) else Color(0xFF7F1D1D),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (item.cleanedUrl != item.finalUrl) {
                Spacer(modifier = Modifier.height(14.dp))
                LabelWithDot(text = "Cleaned URL (Trackers Removed)", color = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.height(6.dp))
                UrlBox(
                    url = item.cleanedUrl,
                    backgroundColor = if (isDark) Color(0xFF0B2240) else Color(0xFFEFF6FF),
                    borderColor = if (isDark) Color(0xFF1E40AF) else Color(0xFFBFDBFE),
                    textColor = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E293B),
                    label = "Cleaned URL"
                )
            }

            if (item.title != null || item.description != null || item.imageUrl != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (item.imageUrl != null) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = "Page Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (item.title != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (item.description != null) {
                            if (item.title != null) Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // StatsBadge & Actions Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.responseTime.toInt()} ms",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete Item",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getTimeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

