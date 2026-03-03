package `in`.bitmaskers.unshortenit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository
import `in`.bitmaskers.unshortenit.ui.screens.DashboardScreen
import `in`.bitmaskers.unshortenit.ui.screens.InterceptorScreen
import `in`.bitmaskers.unshortenit.ui.theme.MyApplicationTheme
import `in`.bitmaskers.unshortenit.ui.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {

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
        val historyRepository = HistoryRepository(dbHelper)
        val unshortenRepository = UnshortenRepository()
        val viewModelFactory = AppViewModelFactory(historyRepository, unshortenRepository)

        setContent {
            MyApplicationTheme {
                if (extractedUrls.isEmpty()) {
                    DashboardScreen(
                        viewModel = viewModel(factory = viewModelFactory),
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