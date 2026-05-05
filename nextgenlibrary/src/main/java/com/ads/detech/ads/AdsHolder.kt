package com.ads.detech.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AOAManager
import com.ads.detech.AdmobUtils
import com.ads.detech.AdmobUtils.BannerCollapsibleAdCallback
import com.ads.detech.AdmobUtils.adImpressionFacebookSDK
import com.ads.detech.AdmobUtils.adImpressionSolarEngineSDK
import com.ads.detech.AdmobUtils.isTestDevice
import com.ads.detech.AppOpenManager
import com.ads.detech.GoogleENative
import com.ads.detech.R
import com.ads.detech.activity.NativeFullActivity
import com.ads.detech.utils.admod.BannerHolderAdmob
import com.ads.detech.utils.admod.InterHolderAdmob
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.ads.detech.utils.admod.callback.AdsInterCallBack
import com.ads.detech.utils.admod.callback.NativeAdmobCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

object AdsHolder {
    var NATIVE_FULL = NativeHolderAdmob("")
    var NATIVE = NativeHolderAdmob("")

    const val TAG = "==AdsManager=="

    private val countMap = mutableMapOf<String, Int>()

    val bannerCollapsibleMap = mutableMapOf<String, BannerHolderAdmob>()
    val nativePreloadMap = mutableMapOf<String, NativeHolderAdmob>()

    fun getOrCreateBannerHolder(key: String, unitId: String): BannerHolderAdmob {
        return bannerCollapsibleMap.getOrPut(key) {
            BannerHolderAdmob(unitId)
        }
    }

    fun getOrCreateNativeHolder(key: String, unitId: String): NativeHolderAdmob {
        return nativePreloadMap.getOrPut(key) {
            NativeHolderAdmob(unitId)
        }
    }


    fun clearBannerHolder(key: String) {
        bannerCollapsibleMap.remove(key)
    }

    fun clearAllBannerHolders() {
        bannerCollapsibleMap.clear()
    }

    fun increaseAndCheck(key: String, interval: Int): Boolean {
        if (interval == 0){
            return false
        }
        val current = countMap.getOrDefault(key, 0)
        countMap[key] = current + 1
        return current % interval == 0
    }


    fun showAOA(activity: AppCompatActivity, adUnit: String, onAction: () -> Unit) {
        val manager = AOAManager(activity, adUnit, 20000, object : AOAManager.AppOpenAdsListener {
            override fun onAdsClose() = onAction()
            override fun onAdsFailed(message: String) {
                Log.d(TAG, "AOA Fail: $message")
                onAction()
            }

            override fun onAdPaid(adValue: AdValue, adUnitAds: String) {
                adImpressionFacebookSDK(activity, adValue)
            }
            override fun onAdsLoaded() {}
        })
        manager.loadAoA()
    }
    private var lastClosedTime = 0L

    fun showInterstitial(activity: AppCompatActivity, adUnit: String,isDialog: Boolean, onAction: () -> Unit) {
        val inter = InterHolderAdmob(adUnit)
        AppOpenManager.getInstance().isAppResumeEnabled = true

        AdmobUtils.loadAndShowAdInterstitial(activity, inter, object : AdsInterCallBack {
            override fun onEventClickAdClosed() {
                val now = SystemClock.elapsedRealtime()
                if (now - lastClosedTime < 1000) {
                    Log.d(TAG, "Ignored duplicate close event")
                    return
                }
                lastClosedTime = now

                Log.d(TAG, "Interstitial closed")
                onAction()
            }

            override fun onAdShowed() {
                Log.d(TAG, "Interstitial shown")
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (_: Exception) {
                    }
                }, 800)
            }

            override fun onAdFail(p0: String?) {
                Log.d(TAG, "Interstitial fail: $p0")
                onAction()
            }

            override fun onStartAction() {}
            override fun onAdLoaded() {}
            override fun onClickAds() {}
            override fun onPaid(p0: AdValue?, p1: String?) {}
        }, isDialog)
    }

    fun showInterstitialWithNative(
        activity: AppCompatActivity,
        interAdUnit: String,
        nativeAdUnit: String,isDialog: Boolean,
        layout_native: Int,
        onAction: () -> Unit
    ) {
        val inter = InterHolderAdmob(interAdUnit)
        AppOpenManager.getInstance().isAppResumeEnabled = true

        val launcher = activity.activityResultRegistry.register(
            "splash_result_${System.currentTimeMillis()}",
            ActivityResultContracts.StartActivityForResult()
        ) {
            Log.d("showAdsSplash", "NativeFullActivity returned")
            val now = SystemClock.elapsedRealtime()
            if (now - lastClosedTime < 1000) {
                Log.d(TAG, "Ignored duplicate close event")
                return@register
            }
            lastClosedTime = now

            Log.d(TAG, "Interstitial closed")
            onAction()
        }

        NATIVE_FULL = NativeHolderAdmob(nativeAdUnit)
        AdmobUtils.loadAndGetNativeFullScreenAdsWithInter(
            activity, NATIVE_FULL,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onAdFail(error: String) = Unit
                override fun onNativeAdLoaded() = Unit
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) = Unit
                override fun onClickAds() = Unit
                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) = Unit
            })

        AdmobUtils.loadAndShowAdInterstitial(activity, inter, object : AdsInterCallBack {
            override fun onStartAction() {
                Log.d(TAG, "Start Native Full Activity")
                val intent = Intent(activity, NativeFullActivity::class.java)
                intent.putExtra("layout_native", layout_native)
                launcher.launch(intent)
            }

            override fun onAdShowed() {
                Log.d(TAG, "Interstitial shown (Native Full)")
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (_: Exception) {
                    }
                }, 800)
            }

            override fun onAdFail(p0: String?) {
                Log.d(TAG, "Interstitial fail: $p0")
                onAction()
            }

            override fun onEventClickAdClosed() {}
            override fun onAdLoaded() {}
            override fun onClickAds() {}
            override fun onPaid(p0: AdValue?, p1: String?) {}
        }, isDialog)
    }

    fun showInterstitialPreload(activity: AppCompatActivity, adUnit: String,isDialog: Boolean, onAction: () -> Unit) {
        val inter = InterHolderAdmob(adUnit)
        AppOpenManager.getInstance().isAppResumeEnabled = true

        AdmobUtils.showInterstitialAdPreload(activity, inter, object : AdsInterCallBack {
            override fun onEventClickAdClosed() {
                val now = SystemClock.elapsedRealtime()
                if (now - lastClosedTime < 1000) {
                    Log.d(TAG, "Ignored duplicate close event")
                    return
                }
                lastClosedTime = now

                Log.d(TAG, "Interstitial closed")
                onAction()
            }

            override fun onAdShowed() {
                Log.d(TAG, "Interstitial shown")
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (_: Exception) {
                    }
                }, 300)
            }

            override fun onAdFail(p0: String?) {
                Log.d(TAG, "Interstitial fail: $p0")
                onAction()
            }

            override fun onStartAction() {}
            override fun onAdLoaded() {}
            override fun onClickAds() {}
            override fun onPaid(p0: AdValue?, p1: String?) {}
        }, isDialog)
    }

    fun showInterstitialPreloadWithNative(
        activity: AppCompatActivity,
        interAdUnit: String,
        nativeAdUnit: String,isDialog: Boolean,
        layout_native: Int,
        onAction: () -> Unit
    ) {
        val inter = InterHolderAdmob(interAdUnit)
        AppOpenManager.getInstance().isAppResumeEnabled = true

        val launcher = activity.activityResultRegistry.register(
            "splash_result_${System.currentTimeMillis()}",
            ActivityResultContracts.StartActivityForResult()
        ) {
            Log.d("showAdsSplash", "NativeFullActivity returned")
            val now = SystemClock.elapsedRealtime()
            if (now - lastClosedTime < 1000) {
                Log.d(TAG, "Ignored duplicate close event")
                return@register
            }
            lastClosedTime = now

            Log.d(TAG, "Interstitial closed")
            onAction()
        }

        NATIVE_FULL = NativeHolderAdmob(nativeAdUnit)
        AdmobUtils.loadAndGetNativeFullScreenAdsWithInter(
            activity, NATIVE_FULL,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onAdFail(error: String) = Unit
                override fun onNativeAdLoaded() = Unit
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) = Unit
                override fun onClickAds() = Unit
                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) = Unit
            })

        AdmobUtils.showInterstitialAdPreload(activity, inter, object : AdsInterCallBack {
            override fun onStartAction() {
                Log.d(TAG, "Start Native Full Activity")
                val intent = Intent(activity, NativeFullActivity::class.java)
                intent.putExtra("layout_native", layout_native)
                launcher.launch(intent)
            }

            override fun onAdShowed() {
                Log.d(TAG, "Interstitial shown (Native Full)")
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (_: Exception) {
                    }
                }, 300)
            }

            override fun onAdFail(p0: String?) {
                Log.d(TAG, "Interstitial fail: $p0")
                onAction()
            }

            override fun onEventClickAdClosed() {}
            override fun onAdLoaded() {}
            override fun onClickAds() {}
            override fun onPaid(p0: AdValue?, p1: String?) {}
        }, isDialog)
    }


    fun loadAndShowNative(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,size: GoogleENative,
        nativeHolder: NativeHolderAdmob,onLoadedOrFail: () -> Unit
    ) {
        AdmobUtils.loadAndShowNativeAdsWithLayoutAds(
            activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            size,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    onLoadedOrFail()
                }

                override fun onAdFail(error: String) {
                    onLoadedOrFail()
                    nativeAdContainer.visibility = View.VISIBLE
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onClickAds() {
                }
            })
    }

    fun loadAndShowNativeCollapsible(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,size: GoogleENative,
        nativeHolder: NativeHolderAdmob,onLoadedOrFail: () -> Unit
    ) {
        AdmobUtils.loadAndShowNativeAdsWithLayoutAdsCollapsible(
            activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            size,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    onLoadedOrFail()
                }

                override fun onAdFail(error: String) {
                    onLoadedOrFail()
                    nativeAdContainer.visibility = View.VISIBLE
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onClickAds() {

                }
            })
    }

    fun loadAndShowNativeCollapsible(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,size: GoogleENative,
        nativeHolder: NativeHolderAdmob,onLoaded: () -> Unit,onClose: () -> Unit,onFail: () -> Unit
    ) {
        AdmobUtils.loadAndShowNativeAdsWithLayoutAdsCollapsible(
            activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            size,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    onLoaded()
                }

                override fun onAdFail(error: String) {
                    onFail()
                    nativeAdContainer.visibility = View.VISIBLE
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onClickAds() {
                    onClose()
                }
            })
    }



    fun showAdBanner(activity: Activity, adsEnum: String, view: ViewGroup) {
        AdmobUtils.loadAdBanner(activity, adsEnum, view, object :
            AdmobUtils.BannerCallBack {
            override fun onClickAds() {

            }

            override fun onFailed(message: String) {
                view.visibility = View.GONE
            }

            override fun onLoad() {

            }


            override fun onPaid(adValue: AdValue?, mAdView: AdView?) {
            }
        })
    }

    fun showAdBannerCollapsible(activity: Activity, adsEnum: BannerHolderAdmob, view: ViewGroup) {
        AdmobUtils.loadAdBannerCollapsibleReload(activity,
            adsEnum,
            view,
            object : AdmobUtils.BannerCollapsibleAdCallback {
                override fun onBannerAdLoaded(adSize: AdSize) {
                    val params: ViewGroup.LayoutParams = view.layoutParams
                    params.height = adSize.getHeightInPixels(activity)
                    view.layoutParams = params
                }

                override fun onClickAds() {
                }

                override fun onAdFail(message: String) {
                    view.visibility = View.GONE
                }

                override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                }
            })
    }

    fun loadNative(context: Context, nativeHolder: NativeHolderAdmob) {
        AdmobUtils.loadAndGetNativeAds(context, nativeHolder, object : NativeAdmobCallback {
            override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

            }

            override fun onNativeAdLoaded() {

            }

            override fun onAdFail(error: String?) {
                Log.e("Admob", "onAdFail: ${nativeHolder.ads}" + error)
            }

            override fun onPaid(p0: AdValue?, p1: String?) {

            }
        })
    }

    fun loadNativeFullscreen(context: Context, nativeHolder: NativeHolderAdmob,onFail: () -> Unit) {
        AdmobUtils.loadAndGetNativeFullScreenAds(
            context as Activity,
            nativeHolder,
            object :
                AdmobUtils.NativeAdCallbackNew {
                override fun onAdFail(error: String) {
                    Log.d("Admob", "onAdFail: ${nativeHolder.ads} $error")
                    onFail()
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                }

                override fun onClickAds() {
                }

                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {
                }

                override fun onNativeAdLoaded() {
                }
            })
    }

    fun showNativeFullscreen(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,
        nativeHolder: NativeHolderAdmob
    ) {
        AdmobUtils.showNativeFullScreenAdsWithLayout(
            activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            object :
                AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeFailed(massage: String) {
                    nativeAdContainer.gone()
                }

                override fun NativeLoaded() {
                    nativeAdContainer.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }
            })
    }

    fun showNative(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,size: GoogleENative,
        nativeHolder: NativeHolderAdmob,
    ) {
        AdmobUtils.showNativeAdsWithLayout(activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            size,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    nativeAdContainer.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                }

                override fun NativeFailed(massage: String) {
                    nativeAdContainer.gone()
                }
            })
    }

    fun showNativeCollapsible(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,size: GoogleENative,
        nativeHolder: NativeHolderAdmob,
    ) {
        AdmobUtils.showNativeAdsWithLayoutCollapsible(activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            size,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    nativeAdContainer.visible()
                }

                override fun onAdFail(error: String) {
                    nativeAdContainer.gone()
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    
                }

                override fun onClickAds() {
                    
                }
            })
    }

    fun showNativeSmall(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,
        nativeHolder: NativeHolderAdmob,
    ) {
        AdmobUtils.showNativeAdsWithLayout(activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            GoogleENative.UNIFIED_SMALL,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {

                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                }

                override fun NativeFailed(massage: String) {
                    nativeAdContainer.gone()
                }
            })
    }

    fun showNativeSmallBanner(
        activity: Activity,
        nativeAdContainer: ViewGroup,layout : Int,
        nativeHolder: NativeHolderAdmob,
    ) {
        AdmobUtils.showNativeAdsWithLayout(activity,
            nativeHolder,
            nativeAdContainer,
            layout,
            GoogleENative.UNIFIED_BANNER,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {

                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                }

                override fun NativeFailed(massage: String) {
                    nativeAdContainer.gone()
                }
            })
    }
}