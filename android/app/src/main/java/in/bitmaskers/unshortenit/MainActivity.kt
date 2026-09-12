package `in`.bitmaskers.unshortenit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepositoryImpl
import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository
import `in`.bitmaskers.unshortenit.ui.screens.MainScreen
import `in`.bitmaskers.unshortenit.ui.screens.InterceptorScreen
import `in`.bitmaskers.unshortenit.ui.theme.MyApplicationTheme
import `in`.bitmaskers.unshortenit.ui.viewmodel.AppViewModelFactory
import com.google.android.gms.ads.MobileAds

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import `in`.bitmaskers.unshortenit.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}

        var extractedUrls = emptyList<String>()
        val incomingText = intent?.data?.toString() ?: intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (incomingText != null) {
            val urlRegex = "(https?://[a-zA-Z0-9./_?=&-]+)".toRegex()
            extractedUrls = urlRegex.findAll(incomingText).map { it.value }.toList()
        }

        val historyRepository = HistoryRepositoryImpl(this)
        val unshortenRepository = UnshortenRepository()
        val appPreferencesRepository = `in`.bitmaskers.unshortenit.data.repository.AppPreferencesRepository(this)
        val viewModelFactory = AppViewModelFactory(appPreferencesRepository, historyRepository, unshortenRepository)

        setContent {
            val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
            val isDarkMode by dashboardViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                if (extractedUrls.isEmpty()) {
                    MainScreen(
                        viewModel = dashboardViewModel,
                        onFinish = { finish() }
                    )
                } else {
                    InterceptorScreen(
                        urlsToProcess = extractedUrls,
                        viewModel = viewModel(factory = viewModelFactory),
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}