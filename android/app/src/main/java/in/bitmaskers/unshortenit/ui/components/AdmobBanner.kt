package `in`.bitmaskers.unshortenit.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
fun AdmobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Remember the AdView instance to prevent thrashing / recreation during recomposition
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = `in`.bitmaskers.unshortenit.BuildConfig.ADMOB_AD_UNIT_ID

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("Admob", "AdmobBanner: Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        "Admob",
                        "AdmobBanner: Ad failed to load. Code: ${error.code}, Message: ${error.message}, Domain: ${error.domain}"
                    )
                }
            }

            try {
                loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e("Admob", "Failed to load AdMob banner", e)
            }
        }
    }

    // Bind AdView to lifecycle so resources are properly paused and released
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        adView.resume()
                    } catch (e: Exception) {
                        Log.w("Admob", "Error resuming AdView", e)
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

    // Reserve fixed height (50dp for AdSize.BANNER) so parent layout never resizes or thrashes insets
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.size(width = 320.dp, height = 50.dp),
            factory = { adView }
        )
    }
}
