package `in`.bitmaskers.unshortenit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.bitmaskers.unshortenit.ui.viewmodel.DashboardViewModel
import `in`.bitmaskers.unshortenit.ui.viewmodel.ReviewAction
import kotlinx.coroutines.launch

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import `in`.bitmaskers.unshortenit.R
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun MainScreen(viewModel: DashboardViewModel, onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val context = LocalContext.current
    val activity = context as? Activity

    val showReviewDialog by viewModel.showReviewDialog.collectAsState()

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onReviewAction(ReviewAction.REMIND_LATER)
            },
            title = {
                Text(
                    text = stringResource(R.string.review_prompt_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.review_prompt_message),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onReviewAction(ReviewAction.RATE_NOW)
                        if (activity != null) {
                            val reviewManager = ReviewManagerFactory.create(activity)
                            val request = reviewManager.requestReviewFlow()
                            request.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val reviewInfo = task.result
                                    reviewManager.launchReviewFlow(activity, reviewInfo)
                                } else {
                                    // Fallback to launching Play Store URL if In-App Review fails
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("market://details?id=${activity.packageName}")
                                            setPackage("com.android.vending")
                                        }
                                        activity.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
                                        }
                                        activity.startActivity(intent)
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5) // Matches modern Indigo accent
                    )
                ) {
                    Text(stringResource(R.string.review_prompt_rate_now), color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.onReviewAction(ReviewAction.NEVER)
                        }
                    ) {
                        Text(stringResource(R.string.review_prompt_no_thanks), color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.onReviewAction(ReviewAction.REMIND_LATER)
                        }
                    ) {
                        Text(stringResource(R.string.review_prompt_later), color = Color(0xFF4F46E5))
                    }
                }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadHistory(isRefresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            // Updated Header to be centered and transparent, matching modern aesthetics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unshorten It",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            )
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Reveal the destination before you click",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigation(pagerState = pagerState, coroutineScope = coroutineScope)
        },
        containerColor = Color(0xFFF9FAFB)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> DashboardScreen(viewModel = viewModel, innerPadding = innerPadding)
                1 -> HistoryScreen(viewModel = viewModel, innerPadding = innerPadding)
            }
        }
    }
}

@Composable
fun BottomNavigation(pagerState: PagerState, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    val items = listOf("Home", "History")
    val selectedIndex = pagerState.currentPage

    NavigationBar(
        containerColor = Color.Transparent,
        contentColor = Color(0xFF1E293B)
    ) {
        items.forEachIndexed { index, title ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (index == 0) Icons.Rounded.Home else Icons.Rounded.History,
                        contentDescription = title
                    )
                },
                label = { Text(title) },
                selected = selectedIndex == index,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4F46E5), // Indigo
                    selectedTextColor = Color(0xFF4F46E5),
                    indicatorColor = Color(0xFFE0E7FF),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}
