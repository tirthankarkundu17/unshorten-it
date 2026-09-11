package `in`.bitmaskers.unshortenit.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    onAdLoaded: () -> Unit = {},
    onAdFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAdVisible by remember { mutableStateOf(true) }
    var isAdLoaded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var lastLoadAttemptTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Remember the AdView instance to prevent thrashing / recreation during recomposition
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = `in`.bitmaskers.unshortenit.BuildConfig.ADMOB_AD_UNIT_ID

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("Admob", "AdmobBanner: Ad loaded successfully")
                    isAdLoaded = true
                    isAdVisible = true
                    isLoading = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        "Admob",
                        "AdmobBanner: Ad failed to load. Code: ${error.code}, Message: ${error.message}, Domain: ${error.domain}"
                    )
                    isAdLoaded = false
                    isAdVisible = false
                    isLoading = false
                    onAdFailed()
                }
            }

            try {
                loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e("Admob", "Failed to load AdMob banner", e)
                isAdLoaded = false
                isAdVisible = false
                isLoading = false
                onAdFailed()
            }
        }
    }

    // Bind AdView to lifecycle so resources are properly paused, resumed, and retried on app resume
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        adView.resume()
                        // If ad is not currently loaded and not in-flight, retry on returning to foreground
                        val now = System.currentTimeMillis()
                        if (!isAdLoaded && !isLoading && (now - lastLoadAttemptTime >= 5_000L)) {
                            Log.d("Admob", "Retrying AdMob banner load on app resume")
                            isLoading = true
                            lastLoadAttemptTime = now
                            adView.loadAd(AdRequest.Builder().build())
                        }
                    } catch (e: Exception) {
                        Log.w("Admob", "Error on resume/retry for AdView", e)
                        isLoading = false
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        adView.pause()
                    } catch (e: Exception) {
                        Log.w("Admob", "Error pausing AdView", e)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        adView.destroy()
                    } catch (e: Exception) {
                        Log.w("Admob", "Error destroying AdView", e)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                adView.destroy()
            } catch (e: Exception) {
                Log.w("Admob", "Error destroying AdView on dispose", e)
            }
        }
    }

    // Only render the container and divider if ad loading did not fail
    if (isAdVisible) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.size(width = 320.dp, height = 50.dp),
                        factory = { adView }
                    )
                }
            }
        }
    }
}
