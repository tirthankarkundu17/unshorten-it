package `in`.bitmaskers.unshortenit.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Determine ad size (you could also use AdSize.BANNER or get the true anchored adaptive size)
                setAdSize(AdSize.BANNER)
                
                // Using the banner ad unit ID from BuildConfig
                adUnitId = in.bitmaskers.unshortenit.BuildConfig.ADMOB_AD_UNIT_ID
                
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
                
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
