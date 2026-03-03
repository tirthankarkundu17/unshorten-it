package `in`.bitmaskers.unshortenit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.bitmaskers.unshortenit.ui.viewmodel.DashboardViewModel

@Composable
fun MainScreen(viewModel: DashboardViewModel, onFinish: () -> Unit) {
    val navController = rememberNavController()
    
    // Top Header Background
    val headerColor = Color(0xFF070417)
    val headerOnColor = Color.White

    Scaffold(
        topBar = {
            // Custom Top App Bar to match design EXACTLY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circle icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Logo",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "URL Unshortener",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reveal full URLs",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigation(navController = navController)
        },
        containerColor = Color(0xFFF9FAFB)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard"
        ) {
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel, innerPadding = innerPadding)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel, innerPadding = innerPadding)
            }
        }
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val items = listOf("dashboard" to "Home", "history" to "History")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF1E293B)
    ) {
        items.forEach { (route, title) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (route == "dashboard") Icons.Rounded.Home else Icons.Rounded.History,
                        contentDescription = title
                    )
                },
                label = { Text(title) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
