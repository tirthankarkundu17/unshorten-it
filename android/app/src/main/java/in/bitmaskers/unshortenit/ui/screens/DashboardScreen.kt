package `in`.bitmaskers.unshortenit.ui.screens

import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.bitmaskers.unshortenit.ui.components.AdmobBanner
import `in`.bitmaskers.unshortenit.ui.viewmodel.DashboardViewModel
import `in`.bitmaskers.unshortenit.ui.viewmodel.UiState

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, innerPadding: PaddingValues) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUnshortening by viewModel.isUnshortening.collectAsStateWithLifecycle()
    val unshortenError by viewModel.unshortenError.collectAsStateWithLifecycle()

    var inputUrl by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(unshortenError) {
        unshortenError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Loading) {
            inputUrl = "" // clear on success
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Link Setup Banner (only on Android 12+ where manual enable is needed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val lifecycleOwner = LocalLifecycleOwner.current
            var isLinkHandlingAllowed by remember { mutableStateOf(true) }

            val checkLinkHandling: () -> Boolean = {
                try {
                    val manager = context.getSystemService(DomainVerificationManager::class.java)
                    val userState = manager?.getDomainVerificationUserState(context.packageName)
                    val hasSelectedDomains = userState?.hostToStateMap?.values?.any { state ->
                        state == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                                state == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
                    } == true
                    (userState?.isLinkHandlingAllowed == true) && hasSelectedDomains
                } catch (e: Exception) {
                    false
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isLinkHandlingAllowed = checkLinkHandling()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                // Initial check
                isLinkHandlingAllowed = checkLinkHandling()

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (!isLinkHandlingAllowed) {
                val prefs = context.getSharedPreferences("app_prefs", 0)
                var showBanner by remember {
                    mutableStateOf(
                        !prefs.getBoolean(
                            "link_setup_dismissed",
                            false
                        )
                    )
                }

                AnimatedVisibility(
                    visible = showBanner,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LinkSetupBanner(
                        onDismiss = {
                            prefs.edit().putBoolean("link_setup_dismissed", true).apply()
                            showBanner = false
                        }
                    )
                }
            }
        }

        // Input Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Enter Shortened URL",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                placeholder = { Text("e.g., bit.ly/abc123", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Color(0xFF94A3B8),
                    unfocusedTextColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFF334155)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.unshortenUrl(inputUrl)
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            val gradientAlpha = if (!isUnshortening) 1f else 0.6f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5).copy(alpha = gradientAlpha),
                                Color(0xFF7C3AED).copy(alpha = gradientAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .then(
                        if (!isUnshortening) Modifier.clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.unshortenUrl(inputUrl)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnshortening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Unshorten",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unshorten URL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Latest Result Feedback (If there are items)
        if (uiState is UiState.Success && (uiState as UiState.Success).data.isNotEmpty()) {
            val latestItem = (uiState as UiState.Success).data.first()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Most Recent Result",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Reuse the History Card logic for consistency
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    FlatHistoryCard(item = latestItem)
                }
            }
        }

        }

        // AdMob Banner anchored to the bottom before padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AdmobBanner()
        }
    }
}

@Composable
private fun LinkSetupBanner(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enable Link Interception",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To intercept shortened links when you tap them, please open Settings and check mark all the links to make sure the app is able to intercept them all.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
