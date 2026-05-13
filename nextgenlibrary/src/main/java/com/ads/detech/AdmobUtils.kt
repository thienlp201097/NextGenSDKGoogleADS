package com.ads.detech

import ads_mobile_sdk.ca
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.detech.NativeFunc.Companion.populateNativeAdView
import com.ads.detech.NativeFunc.Companion.populateNativeAdViewClose
import com.ads.detech.ads.AdsHolder.TAG
import com.ads.detech.track.TenjinSDKUtil
import com.ads.detech.track.TiktokSDKUtils
import com.ads.detech.utils.SweetAlert.SweetAlertDialog
import com.ads.detech.utils.admod.BannerHolderAdmob
import com.ads.detech.utils.admod.InterHolderAdmob
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.ads.detech.utils.admod.callback.AdsInterCallBack
import com.ads.detech.utils.admod.callback.MobileAdsListener
import com.ads.detech.utils.admod.callback.NativeAdmobCallback
import com.ads.detech.utils.admod.callback.RewardAdCallback
import com.airbnb.lottie.LottieAnimationView
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.mediation.admob.AdMobAdapter
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.reyun.solar.engine.AdType
import com.reyun.solar.engine.SolarEngineManager
import com.reyun.solar.engine.infos.SEAdImpEventModel
import com.tiktok.TikTokBusinessSdk
import com.tiktok.appevents.base.TTAdRevenueEvent
import com.tiktok.appevents.base.TTBaseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Date

object AdmobUtils {
    //Dialog loading
    @SuppressLint("StaticFieldLeak")
    @JvmField
    var dialog: SweetAlertDialog? = null
    var dialogFullScreen: Dialog? = null
    // Biến check lần cuối hiển thị quảng cáo
    var lastTimeShowInterstitial: Long = 0
    //check ADS test
    var isTestDevice = false
    var isCheckTestDevice = false
    //Check quảng cáo đang show hay không
    @JvmField
    var isAdShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())
    /** Next Gen GMA may invoke ad callbacks on background threads; UI and app callbacks must run on main. */
    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    private val mobileAdsReadyLock = Any()
    @Volatile
    private var mobileAdsInitRequested = false
    @Volatile
    private var mobileAdsSdkReady = false
    private val pendingAfterMobileAdsReady = mutableListOf<() -> Unit>()

    private fun markMobileAdsInitializationComplete() {
        val tasks: List<() -> Unit>
        synchronized(mobileAdsReadyLock) {
            if (mobileAdsSdkReady) {
                return
            }
            mobileAdsSdkReady = true
            tasks = pendingAfterMobileAdsReady.toList()
            pendingAfterMobileAdsReady.clear()
        }
        tasks.forEach { mainHandler.post(it) }
    }

    /**
     * Runs [block] after Mobile Ads initialization completes (or immediately if already complete).
     * If [initAdmob] was never called, invokes [onMissingInit] and skips loading.
     */
    private fun runAfterMobileAdsInitialized(onMissingInit: () -> Unit, block: () -> Unit) {
        if (!mobileAdsInitRequested) {
            Log.e(TAG, "Chưa gọi AdmobUtils.initAdmob trước khi load ads")
            onMissingInit()
            return
        }
        synchronized(mobileAdsReadyLock) {
            if (mobileAdsSdkReady) {
                mainHandler.post(block)
            } else {
                pendingAfterMobileAdsReady.add(block)
            }
        }
    }

    //Ẩn hiện quảng cáo
    @JvmField
    var isShowAds = true
    //Dùng ID Test để hiển thị quảng cáo
    @JvmField
    var isTesting = false
    //List device test
    var testDevices: MutableList<String> = ArrayList()
    // Waiting InterPreload
    private var waitAdHandler: Handler? = null
    private var waitAdRunnable: Runnable? = null
    //Reward Ads
    @JvmField
    var mRewardedAd: RewardedAd? = null
    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null
    var referrerUrl: String = "abc"


    //Hàm Khởi tạo admob
    @JvmStatic
    fun initAdmob(
        context: Context,APP_ID : String,
        isDebug: Boolean,
        isEnableAds: Boolean,
        isCheckTestDevice: Boolean,
        mobileAdsListener: MobileAdsListener
    ) {
        this.isCheckTestDevice = isCheckTestDevice
        isTesting = isDebug
        isShowAds = isEnableAds
        val appContext = context.applicationContext
        mobileAdsInitRequested = true
        MobileAds.initialize(
            appContext,
            InitializationConfig.Builder(APP_ID).setNativeValidatorDisabled().build(),
        ) {
            markMobileAdsInitializationComplete()
            runOnMainThread {
                val referrerClient = InstallReferrerClient.newBuilder(appContext).build()
                referrerClient.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val resultUrl = withContext(Dispatchers.IO) {
                                        referrerClient.installReferrer.installReferrer
                                    }
                                    referrerUrl = resultUrl
                                    Log.d("==Check Organic==", referrerUrl)
                                } catch (_: Exception) {
                                    Log.d("==Check Organic==", "onInstallReferrerSetupFinished: Error")
                                }
                                mobileAdsListener.onSuccess()
                            }
                        } else {
                            mobileAdsListener.onSuccess()
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        Log.d("==Check Organic==", "onInstallReferrerServiceDisconnected:")
                        mobileAdsListener.onSuccess()
                    }
                })
            }
        }
    }
    @JvmStatic
    fun adImpressionFacebookSDK(context: Context, ad: AdValue) {
        if (!FacebookSdk.isInitialized()) {
            return
        }
        try {
            val logger = AppEventsLogger.newLogger(context.applicationContext)
            val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, ad.currencyCode)
            logger.logEvent(
                AppEventsConstants.EVENT_NAME_AD_IMPRESSION,
                ad.valueMicros / 1000000.0,
                params
            )
        } catch (e: Exception) {
            Log.d(TAG, "adImpressionFacebookSDK: ${e.message}")
        }
    }
    @JvmStatic
    fun adImpressionSolarEngineSDK(adValue: AdValue, adUnitId: String, adFormat: Int, responseInfo : ResponseInfo?){
        //solar
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val precision = adValue.precisionType
        val loadedAdapter = responseInfo?.loadedAdSourceResponseInfo

        val adSourceName = loadedAdapter?.name ?: "unknown"
        val adSourceId = loadedAdapter?.id ?: "unknown"
        val adSourceInstanceId = loadedAdapter?.instanceId ?: ""
        val adSourceInstanceName = loadedAdapter?.instanceName ?: ""

        Log.d("==SolarEngine==", "==============================")
        Log.d("==SolarEngine==", "valueMicros=$valueMicros")
        Log.d("==SolarEngine==", "currencyCode=$currencyCode")
        Log.d("==SolarEngine==", "precision=$precision")
        Log.d("==SolarEngine==", "revenue=${valueMicros / 1_000_000.0}")
        Log.d("==SolarEngine==", "ecpm=${valueMicros / 1000.0}")
        Log.d("==SolarEngine==", "adUnitId=$adUnitId")
        Log.d("==SolarEngine==", "adSourceName=$adSourceName")
        Log.d("==SolarEngine==", "adSourceInstanceId=$adSourceInstanceId")
        Log.d("==SolarEngine==", "adSourceInstanceName=$adSourceInstanceName")
        Log.d("==SolarEngine==", "responseInfo=$responseInfo")
        Log.d("==SolarEngine==", "==============================")

        val model = SEAdImpEventModel()
        model.setAdNetworkPlatform(adSourceName)
        model.setMediationPlatform("admob")
        model.setAdType(adFormat)
        model.setAdNetworkAppID(adSourceId) // hoặc SolarEngine appKey nếu doc/project yêu cầu
        model.setAdNetworkADID(adUnitId)
        model.setEcpm(valueMicros / 1000.0)
        model.setCurrencyType(currencyCode)
        model.setRenderSuccess(true)

        SolarEngineManager.getInstance().trackAdImpression(model)
    }


    fun adImpressionTenjin(adValue: AdValue, adUnitId: String, adFormat: Int, responseInfo : ResponseInfo?){
        val valueMicros: Long = adValue.valueMicros
        val currencyCode: String = adValue.currencyCode
        val adSourceName: String = responseInfo?.loadedAdSourceResponseInfo?.name.toString()
        val adSourceId: String = responseInfo?.loadedAdSourceResponseInfo?.id.toString()
        val adRevenueJson = JSONObject()
        val value = adValue.valueMicros
        val precisionType = adValue.precisionType
        val adUnitId = adUnitId
        var adSourceInstanceName = ""
        var adSourceInstanceId = ""
        responseInfo?.loadedAdSourceResponseInfo?.let {
            adSourceInstanceName = it.instanceName
            adSourceInstanceId = it.instanceId
        }
        try {
            adRevenueJson.put("value", value)
            adRevenueJson.put("currency_code", currencyCode)
            adRevenueJson.put("precision_type", precisionType)
            adRevenueJson.put("ad_unit_id", adUnitId)
            adRevenueJson.put("ad_source_name", adSourceName)
            adRevenueJson.put("ad_source_id", adSourceId)
            adRevenueJson.put("ad_source_instance_name", adSourceInstanceName)
            adRevenueJson.put("ad_source_instance_id", adSourceInstanceId)
            adRevenueJson.put("device_ad_mediation_platform", "admob_sdk")
            adRevenueJson.put("mediation_adapter_class_name", responseInfo?.loadedAdSourceResponseInfo?.adapterClassName)
            adRevenueJson.put("value_micros", valueMicros)
            adRevenueJson.put("response_id", adSourceId)
            val adFormat2 = when(adFormat){
                2->{
                    "splash"
                }
                5->{
                    "banner"
                }
                6,7->{
                    "native"
                }
                3->{
                    "inter"
                }
                else -> {
                    "ads"
                }
            }
            adRevenueJson.put("ad_format", adFormat2)
            TenjinSDKUtil.instance?.eventAdImpressionAdMob(adRevenueJson)
            // banner / interstitial / rewarded / rewarded_interstitial / native / splash

        } catch (e: Exception) {
            e.printStackTrace()
        }
// Make sure the App Events SDK has been initialized before calling this
//        val adRevenueInfo: TTBaseEvent = TTAdRevenueEvent.newBuilder(adRevenueJson).build()
//        TikTokBusinessSdk.trackTTEvent(adRevenueInfo)
    }

    fun initListIdTest() {
        testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
    }

    //check open network
    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        } catch (e: Exception) {
            return false
        }
    }

    interface BannerCallBack {
        fun onClickAds()
        fun onLoad()
        fun onFailed(message: String)
        fun onPaid(adValue: AdValue?, mAdView: AdView?)
    }

    @JvmStatic
    fun loadAdBanner(
        activity: Activity,
        bannerId: String?,
        viewGroup: ViewGroup,
        bannerAdCallback: BannerCallBack
    ) {

        if (isTestDevice){
            bannerAdCallback.onFailed("None Show")
            return
        }
        var bannerId: String = bannerId.toString()
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        val adSize = getAdSize(activity)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        mAdView.loadAd(
            BannerAdRequest.Builder(bannerId,adSize).build(),
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    ad.adEventCallback =
                        object : BannerAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "Banner ad recorded an impression.")
                            }

                            override fun onAdClicked() {
                                runOnMainThread {
                                    bannerAdCallback.onClickAds()
                                    Log.d(TAG, "Banner ad recorded a click.")
                                }
                            }

                            override fun onAdPaid(value: AdValue) {
                                super.onAdPaid(value)
                                runOnMainThread {
                                    adImpressionSolarEngineSDK(value,bannerId,AdType.Banner.value,ad.getResponseInfo())
                                    adImpressionTenjin(value,bannerId,AdType.Banner.value,ad.getResponseInfo())
                                    adImpressionFacebookSDK(activity,value)
                                    bannerAdCallback.onPaid(value,mAdView)
                                }
                            }
                        }
                    ad.bannerAdRefreshCallback =
                        object : BannerAdRefreshCallback {
                            override fun onAdRefreshed() {
                                Log.d(TAG, "Banner ad refreshed.")
                            }

                            override fun onAdFailedToRefresh(adError: LoadAdError) {
                                Log.w(TAG, "Banner ad failed to refresh: $adError")
                            }
                        }
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout?.stopShimmer()
                        viewGroup.removeView(tagView)
                        bannerAdCallback.onLoad()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "Banner ad failed to load: $adError")
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout?.stopShimmer()
                        viewGroup.removeView(tagView)
                        bannerAdCallback.onFailed(adError.message)
                    }
                }
            },
        )
    }

    interface BannerCollapsibleAdCallback {
        fun onClickAds()
        fun onBannerAdLoaded(adSize: AdSize)
        fun onAdFail(message: String)
        fun onAdPaid(adValue: AdValue, mAdView: AdView)
    }

    @JvmStatic
    fun loadAdBannerCollapsibleReload(
        activity: Activity,
        banner: BannerHolderAdmob,
        viewGroup: ViewGroup,
        callback: BannerCollapsibleAdCallback
    ) {
        if (isTestDevice){
            callback.onAdFail("is Test Device")
            return
        }
        var bannerId = banner.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        banner.mAdView?.destroy()
        banner.mAdView?.let {
            viewGroup.removeView(it)
        }
        banner.mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(banner.mAdView, 1)
        } catch (_: Exception) {

        }
        val adSize = getAdSize(activity)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        val extras = Bundle()
        extras.putString("collapsible", "bottom")
        val adRequest =
            BannerAdRequest.Builder(bannerId, adSize)
                .putAdSourceExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
        // Must load on banner.mAdView (added to viewGroup). Global mAdView is unrelated and often null → shimmer stuck forever.
        banner.mAdView!!.loadAd(
            adRequest,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    ad.adEventCallback =
                        object : BannerAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "Banner ad recorded an impression.")
                            }

                            override fun onAdClicked() {
                                runOnMainThread {
                                    callback.onClickAds()
                                    Log.d(TAG, "Banner ad recorded a click.")
                                }
                            }

                            override fun onAdPaid(value: AdValue) {
                                super.onAdPaid(value)
                                runOnMainThread {
                                    adImpressionSolarEngineSDK(value,bannerId,AdType.Banner.value,ad.getResponseInfo())
                                    adImpressionTenjin(value,bannerId,AdType.Banner.value,ad.getResponseInfo())
                                    adImpressionFacebookSDK(activity,value)
                                    callback.onAdPaid(value, banner.mAdView!!)
                                }
                            }
                        }
                    ad.bannerAdRefreshCallback =
                        object : BannerAdRefreshCallback {
                            override fun onAdRefreshed() {
                                Log.d(TAG, "Banner ad refreshed.")
                            }

                            override fun onAdFailedToRefresh(adError: LoadAdError) {
                                Log.w(TAG, "Banner ad failed to refresh: $adError")
                            }
                        }
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout?.stopShimmer()
                        viewGroup.removeView(tagView)
                        callback.onBannerAdLoaded(adSize)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "Banner ad failed to load: $adError")
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout?.stopShimmer()
                        viewGroup.removeView(tagView)
                        callback.onAdFail(adError.message)
                    }
                }
            },
        )
    }


    private fun getAdSize(context: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = context.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * Load and Show Native
     *
     *
     *
     */

    @JvmStatic
    fun loadAndGetNativeAds(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        adCallback: NativeAdmobCallback
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_id)
        }
        val videoOptions: VideoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adRequest = NativeAdRequest.Builder(nativeHolder.ads, listOf(NativeAd.NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build()
        nativeHolder.isLoad = true

        val nativeAdmobCallback = adCallback
// Define the callback to handle successful ad loading or failed ad loading.
        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "Native ad loaded.")
                    CoroutineScope(Dispatchers.Main).launch {
                        // Remove all old ad views when loading a new native ad.
                        nativeHolder.nativeAd = nativeAd
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = nativeAd
                        checkAdsTest(nativeAd)
                        nativeAd.apply {
                            Log.d(TAG, "Banner ad response info: ${nativeAd.getResponseInfo()}")
                            this.adEventCallback =
                                object : NativeAdEventCallback {
                                    override fun onAdImpression() {
                                        Log.d(TAG, "App Open ad recorded an impression.")
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        runOnMainThread {
                                            var native_type = AdType.Native.value
                                            nativeAd.let { it1 ->
                                                if (it1.mediaContent.hasVideoContent){
                                                    native_type = AdType.NativeVideo.value
                                                }
                                            }
                                            adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionTenjin(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionFacebookSDK(context,value)
                                            nativeAdmobCallback.onPaid(value, nativeHolder.ads)
                                        }
                                    }
                                }
                        }
                        nativeAdmobCallback.onLoadedAndGetNativeAd(nativeAd)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: $adError")
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.code)
                    CoroutineScope(Dispatchers.Main).launch {
                        nativeHolder.nativeAd = null
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = null
                        nativeAdmobCallback.onAdFail(adError.message)
                    }
                }
            }
        // Load the native ad with our request and callback.
        runAfterMobileAdsInitialized(
            onMissingInit = {
                runOnMainThread {
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    nativeAdmobCallback.onAdFail("Mobile Ads not initialized")
                }
            },
            block = {
                NativeAdLoader.load(adRequest, adCallback)
            },
        )
    }

    //Load native 2 in here
    interface AdsNativeCallBackAdmod {
        fun NativeLoaded()
        fun NativeFailed(massage: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    @JvmStatic
    fun showNativeAdsWithLayout(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        callback: AdsNativeCallBackAdmod
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            callback.NativeFailed("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                nativeHolder.nativeAd?.apply {
                    this.adEventCallback =
                        object : NativeAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "App Open ad recorded an impression.")
                            }

                            override fun onAdPaid(value: AdValue) {
                                runOnMainThread {
                                    Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                    var native_type = AdType.Native.value
                                    nativeHolder.nativeAd?.let { it1 ->
                                        if (it1.mediaContent.hasVideoContent){
                                            native_type = AdType.NativeVideo.value
                                        }
                                    }
                                    adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionFacebookSDK(activity,value)
                                    callback.onPaid(value, nativeHolder.ads)
                                }
                            }
                        }
                }

                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeHolder.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else if (size === GoogleENative.UNIFIED_SMALL) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeHolder.nativeAd?.apply {
                        this.adEventCallback =
                            object : NativeAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d(TAG, "App Open ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                        var native_type = AdType.Native.value
                                        nativeHolder.nativeAd?.let { it1 ->
                                            if (it1.mediaContent.hasVideoContent){
                                                native_type = AdType.NativeVideo.value
                                            }
                                        }
                                        adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionFacebookSDK(activity,value)
                                        callback.onPaid(value, nativeHolder.ads)
                                    }
                                }
                            }
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.NativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    @JvmStatic
    fun showNativeAdsWithLayoutCollapsible(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        callback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            callback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                nativeHolder.nativeAd?.apply {
                    this.adEventCallback =
                        object : NativeAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "App Open ad recorded an impression.")
                            }

                            override fun onAdPaid(value: AdValue) {
                                runOnMainThread {
                                    Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                    var native_type = AdType.Native.value
                                    nativeHolder.nativeAd?.let { it1 ->
                                        if (it1.mediaContent.hasVideoContent){
                                            native_type = AdType.NativeVideo.value
                                        }
                                    }
                                    adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionFacebookSDK(activity,value)
                                    callback.onAdPaid(value, nativeHolder.ads)
                                }
                            }
                        }
                }
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewClose(nativeHolder.nativeAd!!, adView, size,callback)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.onNativeAdLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.onAdFail("None Show")
            }
        } else {
            val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else if (size === GoogleENative.UNIFIED_SMALL) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeHolder.nativeAd?.apply {
                        this.adEventCallback =
                            object : NativeAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d(TAG, "App Open ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                        var native_type = AdType.Native.value
                                        nativeHolder.nativeAd?.let { it1 ->
                                            if (it1.mediaContent.hasVideoContent){
                                                native_type = AdType.NativeVideo.value
                                            }
                                        }
                                        adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionFacebookSDK(activity,value)
                                        callback.onAdPaid(value, nativeHolder.ads)
                                    }
                                }
                            }
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewClose(nativeAd, adView, size,callback)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.onNativeAdLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.onAdFail("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    // ads native
    interface NativeAdCallbackNew {
        fun onLoadedAndGetNativeAd(ad: NativeAd?)
        fun onNativeAdLoaded()
        fun onAdFail(error: String)
        fun onAdPaid(adValue: AdValue?, adUnitAds: String?)
        fun onClickAds()
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAds(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }

        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        if (isTesting) {
            nativeHolder.ads = activity.getString(R.string.test_ads_admob_native_id)
        }
        val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else if (size === GoogleENative.UNIFIED_SMALL) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        }

        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        val videoOptions: VideoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adRequest =
            NativeAdRequest.Builder(nativeHolder.ads, listOf(NativeAd.NativeAdType.NATIVE))
                .setVideoOptions(videoOptions)
                .build()

        val nativeAdCallbackNew = adCallback
        // Define the callback to handle successful ad loading or failed ad loading.
        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "Native ad loaded.")
                    CoroutineScope(Dispatchers.Main).launch {
                        // Remove all old ad views when loading a new native ad.
                        nativeAdCallbackNew.onNativeAdLoaded()
                        checkAdsTest(nativeAd)
                        val adView = activity.layoutInflater
                            .inflate(layout, null) as NativeAdView
                        populateNativeAdView(nativeAd, adView, size)
                        shimmerFrameLayout.stopShimmer()
                        try {
                            viewGroup.removeAllViews()
                            viewGroup.addView(adView)
                        } catch (_: Exception) {

                        }
                        nativeAd.adEventCallback =
                            object : NativeAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "Native ad showed full screen content.")
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "Native ad dismissed full screen content.")
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {
                                    Log.d(
                                        TAG,
                                        "Native ad failed to show full screen content with error: $fullScreenContentError",
                                    )
                                }

                                override fun onAdImpression() {
                                    Log.d(TAG, "Native ad recorded an impression.")
                                }

                                override fun onAdClicked() {
                                    runOnMainThread {
                                        Log.d(TAG, "Native ad recorded a click.")
                                    }
                                }
                            }
                        nativeAd.apply {
                            this.adEventCallback =
                                object : NativeAdEventCallback {
                                    override fun onAdImpression() {
                                        Log.d(TAG, "App Open ad recorded an impression.")
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        runOnMainThread {
                                            Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                            var native_type = AdType.Native.value
                                            nativeAd.let { it1 ->
                                                if (it1.mediaContent.hasVideoContent){
                                                    native_type = AdType.NativeVideo.value
                                                }
                                            }
                                            adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionTenjin(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionFacebookSDK(activity,value)
                                            adCallback.onAdPaid(value, nativeHolder.ads)
                                        }
                                    }
                                }
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: $adError")
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout.stopShimmer()
                        try {
                            viewGroup.removeAllViews()
                        } catch (_: Exception) {

                        }
                        nativeHolder.isLoad = false
                        nativeAdCallbackNew.onAdFail(adError.message)
                    }
                }

            }
        // Load the native ad with our request and callback.
        runAfterMobileAdsInitialized(
            onMissingInit = {
                runOnMainThread {
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {
                    }
                    nativeHolder.isLoad = false
                    nativeAdCallbackNew.onAdFail("Mobile Ads not initialized")
                }
            },
            block = {
                NativeAdLoader.load(adRequest, adCallback)
            },
        )
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsCollapsible(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        if (isTesting) {
            nativeHolder.ads = activity.getString(R.string.test_ads_admob_native_id)
        }
        val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        }
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        val videoOptions: VideoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adRequest =
            NativeAdRequest.Builder(nativeHolder.ads, listOf(NativeAd.NativeAdType.NATIVE))
                .setVideoOptions(videoOptions)
                .build()

        val nativeAdCallbackCollapsible = adCallback
        // Define the callback to handle successful ad loading or failed ad loading.
        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "Native ad loaded.")
                    CoroutineScope(Dispatchers.Main).launch {
                        // Remove all old ad views when loading a new native ad.
                        nativeAdCallbackCollapsible.onNativeAdLoaded()
                        checkAdsTest(nativeAd)
                        val adView = activity.layoutInflater
                            .inflate(layout, null) as NativeAdView
                        populateNativeAdViewClose(nativeAd, adView, size,nativeAdCallbackCollapsible)
                        shimmerFrameLayout.stopShimmer()
                        try {
                            viewGroup.removeAllViews()
                            viewGroup.addView(adView)
                        } catch (_: Exception) {

                        }
                        nativeAd.adEventCallback =
                            object : NativeAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "Native ad showed full screen content.")
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "Native ad dismissed full screen content.")
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {
                                    Log.d(
                                        TAG,
                                        "Native ad failed to show full screen content with error: $fullScreenContentError",
                                    )
                                }

                                override fun onAdImpression() {
                                    Log.d(TAG, "Native ad recorded an impression.")
                                }

                                override fun onAdClicked() {
                                    runOnMainThread {
                                        Log.d(TAG, "Native ad recorded a click.")
                                    }
                                }
                            }
                        nativeAd.apply {
                            this.adEventCallback =
                                object : NativeAdEventCallback {
                                    override fun onAdImpression() {
                                        Log.d(TAG, "App Open ad recorded an impression.")
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        runOnMainThread {
                                            Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                            var native_type = AdType.Native.value
                                            nativeAd.let { it1 ->
                                                if (it1.mediaContent.hasVideoContent){
                                                    native_type = AdType.NativeVideo.value
                                                }
                                            }
                                            adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionTenjin(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionFacebookSDK(activity,value)
                                            nativeAdCallbackCollapsible.onAdPaid(value, nativeHolder.ads)
                                        }
                                    }
                                }
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: $adError")
                    CoroutineScope(Dispatchers.Main).launch {
                        shimmerFrameLayout.stopShimmer()
                        try {
                            viewGroup.removeAllViews()
                        } catch (_: Exception) {

                        }
                        nativeHolder.isLoad = false
                        nativeAdCallbackCollapsible.onAdFail(adError.message)
                    }
                }

            }
        // Load the native ad with our request and callback.
        runAfterMobileAdsInitialized(
            onMissingInit = {
                runOnMainThread {
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {
                    }
                    nativeHolder.isLoad = false
                    nativeAdCallbackCollapsible.onAdFail("Mobile Ads not initialized")
                }
            },
            block = {
                NativeAdLoader.load(adRequest, adCallback)
            },
        )
    }

    /**
     * Load and Show Interstitial
     * Load and Show Interstitial
     * Load and Show Interstitial
     * Load and Show Interstitial
     */

    @JvmStatic
    fun loadAndShowAdInterstitial(
        activity: AppCompatActivity,
        admobHolder: InterHolderAdmob,
        adCallback: AdsInterCallBack,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        var admobId = admobHolder.ads
        mInterstitialAd = null
        isAdShowing = false

        val appOpenManager = AppOpenManager.getInstance()

        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback.onAdFail("No internet")
            return
        }

        if (appOpenManager.isInitialized && !appOpenManager.isAppResumeEnabled) {
            return
        } else if (appOpenManager.isInitialized) {
            appOpenManager.isAppResumeEnabled = false
        }

        if (enableLoadingDialog) {
            dialogLoading(activity)
        }

        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_id)
        }
        InterstitialAd.load(
            AdRequest.Builder(admobId).build(),
            object :
                com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    runOnMainThread {
                        adCallback.onAdLoaded()

                        Handler(Looper.getMainLooper()).postDelayed({
                            mInterstitialAd = ad

                            // Listen for ad events.
                            mInterstitialAd?.adEventCallback =
                                object : InterstitialAdEventCallback {
                                    override fun onAdShowedFullScreenContent() {
                                        runOnMainThread {
                                            adCallback.onAdShowed()
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                dismissAdDialog()
                                            }, 800)
                                            Log.d(TAG, "Interstitial ad showed.")
                                        }
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        runOnMainThread {
                                            mInterstitialAd = null
                                            lastTimeShowInterstitial = Date().time
                                            adCallback.onEventClickAdClosed()
                                            cleanupAfterAd(appOpenManager)
                                            Log.d(TAG, "Interstitial ad dismissed.")
                                        }
                                    }

                                    override fun onAdFailedToShowFullScreenContent(
                                        fullScreenContentError: FullScreenContentError
                                    ) {
                                        runOnMainThread {
                                            mInterstitialAd = null
                                            handleAdFailure(fullScreenContentError.message, adCallback, appOpenManager)
                                            Log.w(TAG, "Interstitial ad failed to show: $fullScreenContentError")
                                        }
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Interstitial ad recorded an impression.")
                                    }

                                    override fun onAdClicked() {
                                        runOnMainThread {
                                            adCallback.onClickAds()
                                            Log.d(TAG, "Interstitial ad recorded a click.")
                                        }
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        super.onAdPaid(value)
                                        runOnMainThread {
                                            adImpressionSolarEngineSDK(value, admobId,AdType.Interstitial.value,mInterstitialAd?.getResponseInfo())
                                            adImpressionTenjin(value, admobId,AdType.Interstitial.value,mInterstitialAd?.getResponseInfo())
                                            adImpressionFacebookSDK(activity, value)
                                            adCallback.onPaid(value,admobId)
                                        }
                                    }
                                }
                            adCallback.onStartAction()
                            mInterstitialAd?.show(activity)
                            isAdShowing = true
                        }, 800)
                        Log.d(TAG, "Interstitial ad loaded.")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    handleAdFailure(adError.message, adCallback, appOpenManager)
                    Log.w(TAG, "Interstitial ad failed to load: $adError")
                }
            },
        )
    }

    @JvmStatic
    fun loadInterstitialPreload(
        activity: AppCompatActivity,
        admobHolder: InterHolderAdmob,
    ) {
        if (isTestDevice){
            Log.d(TAG, "is Test Device")
            return
        }
        isAdShowing = false

        if (!isShowAds || !isNetworkConnected(activity)) {
            Log.d(TAG, "No internet")
            return
        }
        var AD_UNIT_ID = admobHolder.ads

        if (isTesting) {
            AD_UNIT_ID = activity.getString(R.string.test_ads_admob_inter_id)
        }
        val adRequest: AdRequest = AdRequest.Builder(AD_UNIT_ID).build()
        val preloadConfig = PreloadConfiguration(adRequest)

        val preloadCallback =
            // [Important] Do not call preload start or poll ad within the callback.
            object : PreloadCallback {
                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    Log.i(TAG, ("Interstitial preload ad failed to load with error: " + adError.message),)
                }

                override fun onAdsExhausted(preloadId: String) {
                    Log.i(TAG, "Interstitial preload ad is not available.")
                }

                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    Log.i(TAG, "Interstitial preload ad is available.")

                }
            }

        // Start the preloading with a given preload ID, preload configuration, and callback.
        InterstitialAdPreloader.start(AD_UNIT_ID, preloadConfig, preloadCallback)
    }

    fun showInterstitialAdPreload(activity: Activity, admobHolder: InterHolderAdmob, adCallback: AdsInterCallBack, enableLoadingDialog: Boolean){
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        isAdShowing = false

        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback.onAdFail("No internet")
            return
        }

        val appOpenManager = AppOpenManager.getInstance()

        if (appOpenManager.isInitialized && !appOpenManager.isAppResumeEnabled) {
            return
        } else if (appOpenManager.isInitialized) {
            appOpenManager.isAppResumeEnabled = false
        }
        var AD_UNIT_ID = admobHolder.ads

        if (isTesting) {
            AD_UNIT_ID = activity.getString(R.string.test_ads_admob_inter_id)
        }

        Log.d(TAG, "ID InterstitialAdPreload: $AD_UNIT_ID")

        if (enableLoadingDialog) {
            dialogLoading(activity)
            if (InterstitialAdPreloader.isAdAvailable(AD_UNIT_ID)) {
                val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
                Log.d(TAG, "ID InterstitialAdPreload: isAdAvailable")
                Handler(Looper.getMainLooper()).postDelayed({
                    ad?.apply {
                        Log.d(TAG, "Interstitial ad response info: ${this.getResponseInfo()}")
                        this.adEventCallback =
                            object : InterstitialAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    super.onAdShowedFullScreenContent()
                                    runOnMainThread {
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 300)
                                    }
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    runOnMainThread {
                                        adCallback.onClickAds()
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                    runOnMainThread {
                                        lastTimeShowInterstitial = Date().time
                                        adCallback.onEventClickAdClosed()
                                        cleanupAfterAd(appOpenManager)
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                                    super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                                    runOnMainThread {
                                        handleAdFailure(fullScreenContentError.message, adCallback, appOpenManager)
                                    }
                                }
                                override fun onAdImpression() {
                                    Log.d(TAG, "Interstitial ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(
                                            TAG,
                                            "Interstitial ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}",
                                        )
                                        adImpressionSolarEngineSDK(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionTenjin(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionFacebookSDK(activity, value)
                                        adCallback.onPaid(value,admobHolder.ads)
                                    }
                                }
                            }

                        // Show the ad.
                        adCallback.onStartAction()
                        show(activity)
                        isAdShowing = true
                    }
                }, 300)
            }else{
                Log.d(TAG, "ID InterstitialAdPreload: waitUntilAdAvailable")
                waitUntilAdAvailable(AD_UNIT_ID, onAvailable = {
                    val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
                    ad?.apply {
                        Log.d(TAG, "Interstitial ad response info: ${this.getResponseInfo()}")
                        this.adEventCallback =
                            object : InterstitialAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    super.onAdShowedFullScreenContent()
                                    runOnMainThread {
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 300)
                                    }
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    runOnMainThread {
                                        adCallback.onClickAds()
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                    runOnMainThread {
                                        lastTimeShowInterstitial = Date().time
                                        adCallback.onEventClickAdClosed()
                                        cleanupAfterAd(appOpenManager)
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                                    super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                                    runOnMainThread {
                                        handleAdFailure(fullScreenContentError.message, adCallback, appOpenManager)
                                    }
                                }
                                override fun onAdImpression() {
                                    Log.d(TAG, "Interstitial ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(
                                            TAG,
                                            "Interstitial ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}",
                                        )
                                        adImpressionSolarEngineSDK(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionTenjin(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionFacebookSDK(activity, value)
                                        adCallback.onPaid(value,admobHolder.ads)
                                    }
                                }
                            }

                        // Show the ad.
                        adCallback.onStartAction()
                        show(activity)
                        isAdShowing = true
                    }
                }, onTimeout = {
                    handleAdFailure("mInterstitialAd null", adCallback, appOpenManager)
                })
            }
        }else{
            if (InterstitialAdPreloader.isAdAvailable(AD_UNIT_ID)) {
                val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
                Log.d(TAG, "ID InterstitialAdPreload: isAdAvailable")
                ad?.apply {
                    Log.d(TAG, "Interstitial ad response info: ${this.getResponseInfo()}")
                    this.adEventCallback =
                        object : InterstitialAdEventCallback {
                            override fun onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent()
                                runOnMainThread {
                                    adCallback.onAdShowed()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        dismissAdDialog()
                                    }, 300)
                                }
                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                runOnMainThread {
                                    adCallback.onClickAds()
                                }
                            }

                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                runOnMainThread {
                                    lastTimeShowInterstitial = Date().time
                                    adCallback.onEventClickAdClosed()
                                    cleanupAfterAd(appOpenManager)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                                super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                                runOnMainThread {
                                    handleAdFailure(fullScreenContentError.message, adCallback, appOpenManager)
                                }
                            }
                            override fun onAdImpression() {
                                Log.d(TAG, "Interstitial ad recorded an impression.")
                            }

                            override fun onAdPaid(value: AdValue) {
                                runOnMainThread {
                                    Log.d(
                                        TAG,
                                        "Interstitial ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}",
                                    )
                                    adImpressionSolarEngineSDK(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                    adImpressionTenjin(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                    adImpressionFacebookSDK(activity, value)
                                    adCallback.onPaid(value,AD_UNIT_ID)
                                }
                            }
                        }

                    // Show the ad.
                    adCallback.onStartAction()
                    show(activity)
                    isAdShowing = true
                }
            }else{
                dialogLoading(activity)
                Log.d(TAG, "ID InterstitialAdPreload: waitUntilAdAvailable")
                waitUntilAdAvailable(AD_UNIT_ID, onAvailable = {
                    val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
                    ad?.apply {
                        Log.d(TAG, "Interstitial ad response info: ${this.getResponseInfo()}")
                        this.adEventCallback =
                            object : InterstitialAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    super.onAdShowedFullScreenContent()
                                    runOnMainThread {
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 300)
                                    }
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    runOnMainThread {
                                        adCallback.onClickAds()
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                    runOnMainThread {
                                        lastTimeShowInterstitial = Date().time
                                        adCallback.onEventClickAdClosed()
                                        cleanupAfterAd(appOpenManager)
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                                    super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                                    runOnMainThread {
                                        handleAdFailure(fullScreenContentError.message, adCallback, appOpenManager)
                                    }
                                }
                                override fun onAdImpression() {
                                    Log.d(TAG, "Interstitial ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(
                                            TAG,
                                            "Interstitial ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}",
                                        )
                                        adImpressionSolarEngineSDK(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionTenjin(value, AD_UNIT_ID,AdType.Interstitial.value,ad.getResponseInfo())
                                        adImpressionFacebookSDK(activity, value)
                                        adCallback.onPaid(value,AD_UNIT_ID)
                                    }
                                }
                            }

                        // Show the ad.
                        adCallback.onStartAction()
                        show(activity)
                        isAdShowing = true
                    }
                }, onTimeout = {
                    handleAdFailure("mInterstitialAd null", adCallback, appOpenManager)
                })
            }
        }

    }
    private fun waitUntilAdAvailable(
        adUnitId: String,
        onAvailable: () -> Unit,
        onTimeout: () -> Unit,
        timeoutMs: Long = 8000
    ) {
        val startTime = System.currentTimeMillis()
        waitAdHandler = Handler(Looper.getMainLooper())

        waitAdRunnable = object : Runnable {
            override fun run() {
                // Nếu có ad → gọi callback → stop loop
                if (InterstitialAdPreloader.isAdAvailable(adUnitId)) {
                    onAvailable()
                    Log.d(TAG, "WaitUntilAdAvailable: isAdAvailable")
                    stopWaitAd()
                    return
                }

                // Timeout → dừng
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    stopWaitAd()
                    onTimeout()
                    return
                }

                // Tiếp tục loop mỗi 500ms
                waitAdHandler?.postDelayed(this, 500)
            }
        }

        waitAdHandler?.post(waitAdRunnable!!)
    }

    private fun handleAdFailure(
        message: String,
        adCallback: AdsInterCallBack,
        appOpenManager: AppOpenManager
    ) {
        runOnMainThread {
            mInterstitialAd = null
            isAdShowing = false
            if (appOpenManager.isInitialized) {
                appOpenManager.isAppResumeEnabled = true
            }
            dismissAdDialog()
            adCallback.onAdFail(message)
            Log.e("Admodfail", "Ad failed: $message")
        }
    }

    private fun cleanupAfterAd(appOpenManager: AppOpenManager) {
        runOnMainThread {
            mInterstitialAd = null
            isAdShowing = false
            if (appOpenManager.isInitialized) {
                appOpenManager.isAppResumeEnabled = true
            }
        }
    }


    //Show Inter in here

    @JvmStatic
    fun dismissAdDialog() {
        runOnMainThread {
            try {
                if (dialog != null && dialog!!.isShowing) {
                    dialog!!.dismiss()
                }
                if (dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                    dialogFullScreen?.dismiss()
                }
            } catch (_: Exception) {

            }
        }
    }

    /**
     * Load and Show Reward
     * Load and Show Reward
     * Load and Show Reward
     * Load and Show Reward
     */

    @JvmStatic
    fun loadAndShowAdRewardWithCallback(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardAdCallback,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback2.onAdFail("is Test Device")
            return
        }
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onAdClosed()
            return
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenManager.getInstance().isInitialized) {
            AppOpenManager.getInstance().isAppResumeEnabled = false
        }
        RewardedAd.load(
            AdRequest.Builder(admobId.toString()).build(),
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    runOnMainThread {
                    mRewardedAd = ad
                    if (mRewardedAd != null) {
                        mRewardedAd?.adEventCallback =
                            object : RewardedAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    runOnMainThread {
                                        Log.d(TAG, "Rewarded ad showed.")
                                        isAdShowing = true
                                        adCallback2.onAdShowed()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = false
                                        }
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    runOnMainThread {
                                        mRewardedAd = null
                                        isAdShowing = false
                                        adCallback2.onAdClosed()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        Log.d(TAG, "Rewarded ad dismissed.")
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {
                                    runOnMainThread {
                                        isAdShowing = false
                                        adCallback2.onAdFail(fullScreenContentError.message)
                                        mRewardedAd = null
                                        dismissAdDialog()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        Log.w(TAG, "Rewarded ad failed to show: $fullScreenContentError")
                                    }
                                }

                                override fun onAdImpression() {
                                    Log.d(TAG, "Rewarded ad recorded an impression.")
                                }

                                override fun onAdClicked() {
                                    Log.d(TAG, "Rewarded ad recorded a click.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    super.onAdPaid(value)
                                    runOnMainThread {
                                        adImpressionSolarEngineSDK(value, admobId!!,AdType.RewardVideo.value,ad.getResponseInfo())
                                        adImpressionTenjin(value, admobId!!,AdType.RewardVideo.value,ad.getResponseInfo())
                                        adImpressionFacebookSDK(activity, value)
                                        adCallback2.onPaid(value,admobId)
                                    }
                                }
                            }

                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedAd?.show(activity) { runOnMainThread { adCallback2.onEarned() } }
                            isAdShowing = true
                        } else {
                            mRewardedAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                    }
                    Log.d(TAG, "Rewarded ad loaded.")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    runOnMainThread {
                        mRewardedAd = null
                        adCallback2.onAdFail(adError.message)
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                        isAdShowing = false
                        Log.w(TAG, "Rewarded ad failed to load: $adError")
                    }
                }
            },
        )


    }


    /**
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     */
    @JvmStatic
    fun loadAndShowRewardedInterstitialAdWithCallback(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardAdCallback,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback2.onAdFail("is Test Device")
            return
        }
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onAdClosed()
            return
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenManager.getInstance().isInitialized) {
            AppOpenManager.getInstance().isAppResumeEnabled = false
        }

        RewardedInterstitialAd.load(
            AdRequest.Builder(admobId.toString()).build(),
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<RewardedInterstitialAd> {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    runOnMainThread {
                    mRewardedInterstitialAd = ad
                    if (mRewardedInterstitialAd != null) {
                        mRewardedInterstitialAd?.adEventCallback =
                            object : RewardedInterstitialAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    runOnMainThread {
                                        Log.d(TAG, "Rewarded ad showed.")
                                        isAdShowing = true
                                        adCallback2.onAdShowed()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = false
                                        }
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    runOnMainThread {
                                        mRewardedInterstitialAd = null
                                        isAdShowing = false
                                        adCallback2.onAdClosed()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        Log.d(TAG, "Rewarded ad dismissed.")
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {
                                    runOnMainThread {
                                        isAdShowing = false
                                        adCallback2.onAdFail(fullScreenContentError.message)
                                        mRewardedInterstitialAd = null
                                        dismissAdDialog()
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        Log.w(TAG, "Rewarded ad failed to show: $fullScreenContentError")
                                    }
                                }

                                override fun onAdImpression() {
                                    Log.d(TAG, "Rewarded ad recorded an impression.")
                                }

                                override fun onAdClicked() {
                                    Log.d(TAG, "Rewarded ad recorded a click.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    super.onAdPaid(value)
                                    runOnMainThread {
                                        adImpressionSolarEngineSDK(value, admobId!!,AdType.RewardVideo.value,ad.getResponseInfo())
                                        adImpressionTenjin(value, admobId!!,AdType.RewardVideo.value,ad.getResponseInfo())
                                        adImpressionFacebookSDK(activity, value)
                                        adCallback2.onPaid(value,admobId)
                                    }
                                }
                            }

                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedInterstitialAd?.show(activity) { runOnMainThread { adCallback2.onEarned() } }
                            isAdShowing = true
                        } else {
                            mRewardedInterstitialAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                    }
                    Log.d(TAG, "Rewarded ad loaded.")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    runOnMainThread {
                        mRewardedInterstitialAd = null
                        adCallback2.onAdFail(adError.message)
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                        isAdShowing = false
                        Log.w(TAG, "Rewarded ad failed to load: $adError")
                    }
                }
            },
        )

    }

    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }


    fun dialogLoading(context: Activity) {
        runOnMainThread {
            dialogFullScreen = Dialog(context)
            dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
            dialogFullScreen?.setCancelable(false)
            dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            dialogFullScreen?.window!!.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
            img?.setAnimation(R.raw.gifloading)
            try {
                if (!context.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                    dialogFullScreen?.show()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    @JvmStatic
    fun loadAndGetNativeFullScreenAds(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        val videoOptions: VideoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(true).build()
        val adRequest = NativeAdRequest.Builder(nativeHolder.ads, listOf(NativeAd.NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build()
        nativeHolder.isLoad = true

        val nativeFullscreenCallback = adCallback
// Define the callback to handle successful ad loading or failed ad loading.
        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "Native ad loaded.")
                    CoroutineScope(Dispatchers.Main).launch {
                        // Remove all old ad views when loading a new native ad.
                        nativeHolder.nativeAd = nativeAd
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = nativeAd
                        checkAdsTest(nativeAd)
                        nativeAd.apply {
                            Log.d(TAG, "Banner ad response info: ${nativeAd.getResponseInfo()}")
                            this.adEventCallback =
                                object : NativeAdEventCallback {
                                    override fun onAdImpression() {
                                        Log.d(TAG, "App Open ad recorded an impression.")
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        runOnMainThread {
                                            var native_type = AdType.Native.value
                                            nativeAd.let { it1 ->
                                                if (it1.mediaContent.hasVideoContent){
                                                    native_type = AdType.NativeVideo.value
                                                }
                                            }
                                            adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionTenjin(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionFacebookSDK(context,value)
                                            adCallback.onAdPaid(value,nativeHolder.ads)
                                        }
                                    }
                                }
                        }
                        nativeFullscreenCallback.onLoadedAndGetNativeAd(nativeAd)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: $adError")
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.code)
                    CoroutineScope(Dispatchers.Main).launch {
                        nativeHolder.nativeAd = null
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = null
                        nativeFullscreenCallback.onAdFail(adError.message)
                    }
                }
            }
        // Load the native ad with our request and callback.
        runAfterMobileAdsInitialized(
            onMissingInit = {
                runOnMainThread {
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    nativeFullscreenCallback.onAdFail("Mobile Ads not initialized")
                }
            },
            block = {
                NativeAdLoader.load(adRequest, adCallback)
            },
        )
    }

    @JvmStatic
    fun loadAndGetNativeFullScreenAdsWithInter(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        val videoOptions: VideoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(true).build()
        val adRequest = NativeAdRequest.Builder(nativeHolder.ads, listOf(NativeAd.NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build()
        nativeHolder.isLoad = true

        val nativeFullscreenInterCallback = adCallback
// Define the callback to handle successful ad loading or failed ad loading.
        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "Native ad loaded.")
                    CoroutineScope(Dispatchers.Main).launch {
                        // Remove all old ad views when loading a new native ad.
                        nativeHolder.nativeAd = nativeAd
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = nativeAd
                        checkAdsTest(nativeAd)
                        nativeAd.apply {
                            Log.d(TAG, "Banner ad response info: ${nativeAd.getResponseInfo()}")
                            this.adEventCallback =
                                object : NativeAdEventCallback {
                                    override fun onAdImpression() {
                                        Log.d(TAG, "App Open ad recorded an impression.")
                                    }

                                    override fun onAdPaid(value: AdValue) {
                                        runOnMainThread {
                                            var native_type = AdType.Native.value
                                            nativeAd.let { it1 ->
                                                if (it1.mediaContent.hasVideoContent){
                                                    native_type = AdType.NativeVideo.value
                                                }
                                            }
                                            adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionTenjin(value, nativeHolder.ads,native_type,nativeAd.getResponseInfo())
                                            adImpressionFacebookSDK(context,value)
                                            adCallback.onAdPaid(value,nativeHolder.ads)
                                        }
                                    }

                                    override fun onAdClicked() {
                                        runOnMainThread {
                                            super.onAdClicked()
                                            nativeFullscreenInterCallback.onClickAds()
                                        }
                                    }
                                }
                        }
                        nativeFullscreenInterCallback.onLoadedAndGetNativeAd(nativeAd)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: $adError")
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.code)
                    CoroutineScope(Dispatchers.Main).launch {
                        nativeHolder.nativeAd = null
                        nativeHolder.isLoad = false
                        nativeHolder.native_mutable.value = null
                        nativeFullscreenInterCallback.onAdFail(adError.message)
                    }
                }

            }
        // Load the native ad with our request and callback.
        runAfterMobileAdsInitialized(
            onMissingInit = {
                runOnMainThread {
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    nativeFullscreenInterCallback.onAdFail("Mobile Ads not initialized")
                }
            },
            block = {
                NativeAdLoader.load(adRequest, adCallback)
            },
        )




    }

    @JvmStatic
    fun showNativeFullScreenAdsWithLayout(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        callback: AdsNativeCallBackAdmod
    ) {

        if (isTestDevice){
            viewGroup.visibility = View.GONE
            callback.NativeFailed("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                nativeHolder.nativeAd?.apply {
                    this.adEventCallback =
                        object : NativeAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "App Open ad recorded an impression.")
                            }

                            override fun onAdPaid(value: AdValue) {
                                runOnMainThread {
                                    Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                    var native_type = AdType.Native.value
                                    nativeHolder.nativeAd?.let { it1 ->
                                        if (it1.mediaContent.hasVideoContent){
                                            native_type = AdType.NativeVideo.value
                                        }
                                    }
                                    adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                    adImpressionFacebookSDK(activity,value)
                                    callback.onPaid(value, nativeHolder.ads)
                                }
                            }
                        }
                }

                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeHolder.nativeAd!!, adView)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView = activity.layoutInflater.inflate(
                R.layout.layoutnative_loading_fullscreen,
                null,
                false
            )
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeHolder.nativeAd?.apply {
                        this.adEventCallback =
                            object : NativeAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d(TAG, "App Open ad recorded an impression.")
                                }

                                override fun onAdPaid(value: AdValue) {
                                    runOnMainThread {
                                        Log.d(TAG, "Native ad onPaidEvent: ${value.valueMicros} ${value.currencyCode}")
                                        var native_type = AdType.Native.value
                                        nativeHolder.nativeAd?.let { it1 ->
                                            if (it1.mediaContent.hasVideoContent){
                                                native_type = AdType.NativeVideo.value
                                            }
                                        }
                                        adImpressionSolarEngineSDK(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionTenjin(value, nativeHolder.ads,native_type,nativeHolder.nativeAd?.getResponseInfo())
                                        adImpressionFacebookSDK(activity,value)
                                        callback.onPaid(value, nativeHolder.ads)
                                    }
                                }
                            }
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.NativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }


    fun checkAdsTest(ad: NativeAd?) {
        if (isCheckTestDevice){
            try {
                val testAdResponse = ad?.headline.toString().replace(" ", "").split(":")[0]
                Log.d("===Native", ad?.headline.toString().replace(" ", "").split(":")[0])
                val testAdResponses = arrayOf(
                    "TestAd",
                    "Anunciodeprueba",
                    "Annuncioditesto",
                    "Testanzeige",
                    "TesIklan",
                    "Anúnciodeteste",
                    "Тестовоеобъявление",
                    "পরীক্ষামূলকবিজ্ঞাপন",
                    "जाँचविज्ञापन",
                    "إعلانتجريبي",
                    "Quảngcáothửnghiệm"
                )
                isTestDevice = testAdResponses.contains(testAdResponse)
            } catch (_: Exception) {
                isTestDevice = true
                Log.d("===Native", "Error")
            }
            AppOpenManager.getInstance().setTestAds(isTestDevice)
            Log.d("===TestDevice===", "isTestDevice: $isTestDevice")
        }else{
            isTestDevice = false
        }
    }
    private fun stopWaitAd() {
        waitAdHandler?.removeCallbacks(waitAdRunnable!!)
        waitAdHandler = null
        waitAdRunnable = null
    }
}