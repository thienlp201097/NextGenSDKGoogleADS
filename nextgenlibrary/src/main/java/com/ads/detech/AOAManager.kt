package com.ads.detech

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.ads.detech.AdmobUtils.adImpressionSolarEngineSDK
import com.ads.detech.ads.AdsHolder.TAG
import com.airbnb.lottie.LottieAnimationView
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class AOAManager(private val activity: Activity,val appOpen: String,val timeOut: Long, val appOpenAdsListener: AppOpenAdsListener) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    private var appOpenAd: AppOpenAd? = null
    var isShowingAd = true
    var isLoading = true
    var dialogFullScreen: Dialog? = null
    var isStart = true
    private var isLoadAndShow = true
    private val isAdAvailable: Boolean
        get() = appOpenAd != null

    fun loadAoA() {
        Log.d("===Load","id1")
        var idAoa = appOpen
        if (AdmobUtils.isTesting){
            idAoa = activity.getString(R.string.test_ads_admob_app_open_new)
        }
        if (!AdmobUtils.isShowAds){
            appOpenAdsListener.onAdsFailed("isShowAds false")
            return
        }
        //Check timeout show inter
        val job = CoroutineScope(Dispatchers.Main).launch{
            delay(timeOut)
            if (isLoading && isStart) {
                isStart = false
                isLoading = false
                onAoaDestroyed()
                appOpenAdsListener.onAdsFailed("Time out")
                Log.d("====Timeout", "TimeOut")
            }
        }
        if (isAdAvailable) {
            job.cancel()
            appOpenAdsListener.onAdsFailed("isAdAvailable true")
            return
        } else {
            Log.d("====Timeout", "fetching... ")
            isShowingAd = false

            AppOpenAd.load(
                AdRequest.Builder(idAoa).build(),
                object : AdLoadCallback<AppOpenAd> {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        runOnMainThread {
                            // Called when an ad has loaded.
                            appOpenAd = ad
                            appOpenAdsListener.onAdsLoaded()
                            job.cancel()
                            Log.d("====Timeout", "isAdAvailable = true")
                            if (!AppOpenManager.getInstance().isShowingAd && !isShowingAd && isLoadAndShow) {
                                showAdIfAvailable()
                            }
                            // [END_EXCLUDE]
                        }
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnMainThread {
                            isLoading = false
                            if (isStart) {
                                isStart = false
                                appOpenAdsListener.onAdsFailed(loadAdError.message)
                            }
                            job.cancel()
                            // [END_EXCLUDE]
                        }
                    }
                },
            )
        }
    }

    fun showAdIfAvailable() {
        runOnMainThread {
        Log.d("====Timeout", "$isShowingAd - $isAdAvailable")
        if (!isShowingAd && isAdAvailable && isLoading) {
            isLoading = false
            if (AppOpenManager.getInstance().isInitialized) {
                AppOpenManager.getInstance().isAppResumeEnabled = false
            }
            Log.d("====Timeout", "will show ad ")

            appOpenAd?.run {
                adEventCallback =
                    object : AppOpenAdEventCallback {
                        override fun onAdShowedFullScreenContent() {
                            runOnMainThread {
                                isShowingAd = true
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnMainThread {
                                try {
                                    dialogFullScreen?.dismiss()
                                } catch (ignored: Exception) {
                                }
                                appOpenAd = null
                                isShowingAd = true
                                Log.d("====Timeout", "Dismiss... ")
                                if (isStart) {
                                    isStart = false
                                    appOpenAdsListener.onAdsClose()
                                }
                                if (AppOpenManager.getInstance().isInitialized) {
                                    AppOpenManager.getInstance().isAppResumeEnabled = true
                                }
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(
                            fullScreenContentError: FullScreenContentError
                        ) {
                            runOnMainThread {
                                try {
                                    dialogFullScreen?.dismiss()
                                } catch (ignored: Exception) {
                                }
                                isShowingAd = true
                                if (isStart) {
                                    isStart = false
                                    appOpenAdsListener.onAdsFailed(fullScreenContentError.message)
                                    Log.d("====Timeout", "Failed... $fullScreenContentError")
                                }
                                if (AppOpenManager.getInstance().isInitialized) {
                                    AppOpenManager.getInstance().isAppResumeEnabled = true
                                }
                            }
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "App open ad recorded an impression.")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "App open ad recorded a click.")
                        }

                        override fun onAdPaid(value: AdValue) {
                            super.onAdPaid(value)
                            runOnMainThread {
                                adImpressionSolarEngineSDK(value, appOpen, 6, appOpenAd?.getResponseInfo())
                                appOpenAdsListener.onAdPaid(value, appOpen)
                            }
                        }
                    }
                dialogFullScreen = Dialog(activity)
                dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
                dialogFullScreen?.setCancelable(false)
                dialogFullScreen?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                dialogFullScreen?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
                img?.setAnimation(R.raw.gifloading)
                try {
                    if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                        dialogFullScreen?.show()
                    }
                } catch (ignored: Exception) {
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!AppOpenManager.getInstance().isShowingAd && !isShowingAd){
                        Log.d("===AOA","Show")
                        try {
                            val txt = dialogFullScreen?.findViewById<TextView>(R.id.txtLoading)
                            img?.visibility = View.INVISIBLE
                            txt?.visibility = View.INVISIBLE
                        } catch (ignored: Exception) {
                        }
                        show(activity)
                    }else{
                        appOpenAdsListener.onAdsFailed("AOA can't show")
                    }
                }, 800)
            }
        }else{
            appOpenAdsListener.onAdsFailed("AOA can't show in background!")
        }
        }
    }

    fun onAoaDestroyed() {
        runOnMainThread {
            isShowingAd = true
            isLoading = false
            try {
                if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                    dialogFullScreen?.dismiss()
                }
                appOpenAd?.adEventCallback?.onAdDismissedFullScreenContent()
            } catch (ignored: Exception) {
            }
        }
    }

    fun setLoadAndShow(loadAndShow: Boolean){
        isLoadAndShow = loadAndShow
    }
    interface AppOpenAdsListener {
        fun onAdsClose()
        fun onAdsLoaded()
        fun onAdsFailed(message : String)
        fun onAdPaid(adValue: AdValue, adUnitAds : String)
    }

}