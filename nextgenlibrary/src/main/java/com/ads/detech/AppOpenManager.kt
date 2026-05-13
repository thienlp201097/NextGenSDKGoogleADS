package com.ads.detech

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.Window
import android.widget.LinearLayout
import androidx.lifecycle.*
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.reyun.solar.engine.AdType
import java.util.*

class AppOpenManager private constructor() :
    Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    interface FullScreenContentCallback {
        fun onAdDismissedFullScreenContent()

        fun onAdFailedToShowFullScreenContent(
            adError: FullScreenContentError
        )

        fun onAdShowedFullScreenContent()
    }

    companion object {

        private const val TAG = "AppOpenManager"

        private const val TIMEOUT_MSG = 11

        @Volatile
        private var INSTANCE: AppOpenManager? = null

        @JvmStatic
        fun getInstance(): AppOpenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppOpenManager().also {
                    INSTANCE = it
                }
            }
        }

        var isShowingAd = false
    }

    private var appResumeAd: AppOpenAd? = null

    private var splashAd: AppOpenAd? = null

    private var fullScreenContentCallback: FullScreenContentCallback? = null

    private var appResumeAdId: String? = null

    private var currentActivity: Activity? = null

    private var myApplication: Application? = null

    val isShowingAd: Boolean
        get() = AppOpenManager.isShowingAd

    var isShowingAdsOnResume = false

    var isShowingAdsOnResumeBanner = false

    private var appResumeLoadTime = 0L

    private var splashLoadTime = 0L

    private var splashTimeout = 0

    var timeToBackground = 0L

    private var waitingTime = 30000L

    var isInitialized = false
        private set

    private var isTestAds = false

    var isAppResumeEnabled = true

    private val disabledAppOpenList = ArrayList<Class<*>>()

    private var splashActivity: Class<*>? = null

    private var isTimeout = false

    private var dialogFullScreen: Dialog? = null

    private var isLoading = false

    var isDismiss = false

    private var adRequest: AdRequest? = null

    private val timeoutHandler = Handler(Looper.getMainLooper()) { msg ->

        if (msg.what == TIMEOUT_MSG) {
            isTimeout = true
        }

        false
    }

    fun setTestAds(isTestAds: Boolean) {
        this.isTestAds = isTestAds
    }

    fun setWaitingTime(waitingTime: Long) {
        this.waitingTime = waitingTime
    }

    fun init(
        application: Application,
        appOpenAdId: String
    ) {

        isInitialized = true

        myApplication = application

        this.appResumeAdId =
            if (AdmobUtils.isTesting) {
                application.getString(R.string.test_ads_admob_app_open_new)
            } else {
                appOpenAdId
            }

        initAdRequest()

        myApplication?.registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(this)

        if (!isAdAvailable(false) && appOpenAdId.isNotEmpty()) {
            fetchAd(false)
        }
    }

    fun initAdRequest() {

        if (appResumeAdId == null) {
            return
        }

        adRequest = AdRequest.Builder(appResumeAdId!!).build()
    }

    fun disableAppResumeWithActivity(
        activityClass: Class<*>
    ) {

        Log.d(
            TAG,
            "disableAppResumeWithActivity: ${activityClass.name}"
        )

        disabledAppOpenList.add(activityClass)
    }

    fun enableAppResumeWithActivity(
        activityClass: Class<*>
    ) {

        Log.d(
            TAG,
            "enableAppResumeWithActivity: ${activityClass.name}"
        )

        Handler(Looper.getMainLooper()).postDelayed({

            disabledAppOpenList.remove(activityClass)

        }, 40)
    }

    fun setAppResumeAdId(appResumeAdId: String) {
        this.appResumeAdId = appResumeAdId
    }

    fun setFullScreenContentCallback(
        callback: FullScreenContentCallback
    ) {
        this.fullScreenContentCallback = callback
    }

    fun removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null
    }

    fun fetchAd(isSplash: Boolean) {

        if (isTestAds) {

            Log.d(
                TAG,
                "This is a Test Ads, Ads will not be shown"
            )

            return
        }

        Log.d(TAG, "fetchAd: isSplash = $isSplash")

        if (
            isAdAvailable(isSplash)
            || appResumeAdId == null
            || appResumeAd != null
        ) {

            Log.d(
                TAG,
                "AppOpenManager: Ad is ready or id = null"
            )

            return
        }

        if (!isLoading) {

            Log.d(TAG, "===fetchAd: Loading")

            isLoading = true

            if (adRequest == null) {
                initAdRequest()
            }

            if (adRequest == null) {

                isLoading = false

                return
            }

            AppOpenAd.load(
                adRequest!!,
                object : AdLoadCallback<AppOpenAd> {

                    override fun onAdLoaded(
                        ad: AppOpenAd
                    ) {

                        Log.d(
                            TAG,
                            "AppOpenManager: Loaded"
                        )

                        appResumeAd = ad

                        appResumeLoadTime = Date().time

                        isLoading = false
                    }

                    override fun onAdFailedToLoad(
                        loadAdError: LoadAdError
                    ) {

                        isLoading = false

                        Log.d(
                            TAG,
                            "AppOpenManager: onAdFailedToLoad"
                        )
                    }
                }
            )
        }
    }

    private fun wasLoadTimeLessThanNHoursAgo(
        loadTime: Long,
        numHours: Long
    ): Boolean {

        val dateDifference =
            Date().time - loadTime

        val numMilliSecondsPerHour = 3600000L

        return dateDifference <
                (numMilliSecondsPerHour * numHours)
    }

    fun isAdAvailable(
        isSplash: Boolean
    ): Boolean {

        val loadTime =
            if (isSplash) {
                splashLoadTime
            } else {
                appResumeLoadTime
            }

        val valid =
            wasLoadTimeLessThanNHoursAgo(
                loadTime,
                4
            )

        Log.d(TAG, "isAdAvailable: $valid")

        return (
                if (isSplash) {
                    splashAd != null
                } else {
                    appResumeAd != null
                }
                ) && valid
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onActivityStarted(
        activity: Activity
    ) {

        Log.d(
            "===ADS",
            "${activity.javaClass}|${AdActivity::class.java}"
        )

        currentActivity = activity

        Log.d("===ADS", "Running")
    }

    override fun onActivityResumed(
        activity: Activity
    ) {

        currentActivity = activity

        if (splashActivity == null) {

            if (
                activity.javaClass.name !=
                AdActivity::class.java.name
            ) {
                fetchAd(false)
            }

        } else {

            if (
                activity.javaClass.name != splashActivity!!.name
                && activity.javaClass.name !=
                AdActivity::class.java.name
            ) {

                fetchAd(false)
            }
        }
    }

    override fun onActivityStopped(
        activity: Activity
    ) {
    }

    override fun onActivityPaused(
        activity: Activity
    ) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        bundle: Bundle
    ) {
    }

    override fun onActivityDestroyed(
        activity: Activity
    ) {

        currentActivity = null

        if (dialogFullScreen?.isShowing == true) {
            dialogFullScreen?.dismiss()
        }
    }

    fun showAdIfAvailable(
        isSplash: Boolean
    ) {

        if (
            !ProcessLifecycleOwner.get()
                .lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        ) {

            Log.d("===Onresume", "STARTED")

            if (fullScreenContentCallback != null) {

                try {

                    dialogFullScreen?.dismiss()

                    dialogFullScreen = null

                } catch (_: Exception) {
                }

                fullScreenContentCallback
                    ?.onAdDismissedFullScreenContent()
            }

            return
        }

        Log.d(
            "===Onresume",
            "FullScreenContentCallback"
        )

        if (!isShowingAd && isAdAvailable(isSplash)) {

            isDismiss = true

            val callback =
                object : FullScreenContentCallback {

                    override fun onAdDismissedFullScreenContent() {

                        Handler(Looper.getMainLooper()).post {

                            Log.d(
                                "==TestAOA==",
                                "onResume: true"
                            )

                            Handler(Looper.getMainLooper())
                                .postDelayed({

                                    isDismiss = false

                                    Log.d(
                                        "==TestAOA==",
                                        "onResume: false"
                                    )

                                }, 200)

                            isLoading = false

                            Log.d(
                                TAG,
                                "onAdShowedFullScreenContent: Dismiss"
                            )

                            try {

                                if (
                                    dialogFullScreen != null
                                    && dialogFullScreen!!.isShowing
                                ) {

                                    dialogFullScreen?.dismiss()
                                }

                                dialogFullScreen = null

                            } catch (_: Exception) {
                            }

                            appResumeAd = null

                            fullScreenContentCallback
                                ?.onAdDismissedFullScreenContent()

                            AppOpenManager.isShowingAd = false

                            fetchAd(isSplash)
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        adError: FullScreenContentError
                    ) {

                        Handler(Looper.getMainLooper()).post {

                            isLoading = false

                            isDismiss = false

                            Log.d(
                                TAG,
                                "onAdShowedFullScreenContent: Show false"
                            )

                            try {

                                if (
                                    dialogFullScreen != null
                                    && dialogFullScreen!!.isShowing
                                ) {

                                    dialogFullScreen?.dismiss()
                                }

                                dialogFullScreen = null

                            } catch (_: Exception) {
                            }

                            fullScreenContentCallback
                                ?.onAdFailedToShowFullScreenContent(adError)

                            fetchAd(isSplash)
                        }
                    }

                    override fun onAdShowedFullScreenContent() {

                        Handler(Looper.getMainLooper()).post {

                            Log.d(
                                TAG,
                                "onAdShowedFullScreenContent: Show"
                            )

                            AppOpenManager.isShowingAd = true

                            appResumeAd = null
                        }
                    }
                }

            showAdsResume(isSplash, callback)

        } else {

            Log.d(TAG, "Ad is not ready")

            if (!isSplash) {
                fetchAd(false)
            }
        }
    }

    private fun showAdsResume(
        isSplash: Boolean,
        callback: FullScreenContentCallback
    ) {

        if (
            ProcessLifecycleOwner.get()
                .lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        ) {

            Handler(Looper.getMainLooper())
                .postDelayed({

                    if (appResumeAd != null) {

                        val ad = appResumeAd!!

                        ad.adEventCallback =
                            object : AppOpenAdEventCallback {

                                override fun onAdPaid(
                                    adValue: AdValue
                                ) {

                                    AdmobUtils.adImpressionSolarEngineSDK(
                                        adValue,
                                        appResumeAdId.toString(),
                                        AdType.Splash.value,
                                        ad.getResponseInfo()
                                    )

                                    currentActivity?.let {
                                        AdmobUtils.adImpressionTenjin(
                                            adValue,
                                            appResumeAdId ?: "",
                                            AdType.Splash.value,
                                            ad.getResponseInfo()
                                        )
                                        AdmobUtils.adImpressionFacebookSDK(
                                            it,
                                            adValue
                                        )
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {

                                    callback.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {

                                    callback.onAdFailedToShowFullScreenContent(
                                        fullScreenContentError
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {

                                    callback.onAdShowedFullScreenContent()
                                }
                            }

                        if (currentActivity != null) {

                            showDialog(currentActivity!!)

                            ad.show(currentActivity!!)
                        }
                    }

                }, 100)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected fun onMoveToForeground() {

        Handler(Looper.getMainLooper())
            .postDelayed({

                Log.d(
                    "===OnStart",
                    (System.currentTimeMillis() - timeToBackground).toString()
                )

                if (
                    System.currentTimeMillis() - timeToBackground
                    < waitingTime
                ) {
                    return@postDelayed
                }

                if (isTestAds) {

                    Log.d(
                        TAG,
                        "This is a Test Ads, Ads will not be shown"
                    )

                    return@postDelayed
                }

                if (currentActivity == null) {
                    return@postDelayed
                }

                if (
                    currentActivity!!.javaClass ==
                    AdActivity::class.java
                ) {
                    return@postDelayed
                }

                if (AdmobUtils.isAdShowing) {
                    return@postDelayed
                }

                if (!AdmobUtils.isShowAds) {
                    return@postDelayed
                }

                if (!isAppResumeEnabled) {

                    Log.d(
                        "===Onresume",
                        "isAppResumeEnabled"
                    )

                    return@postDelayed

                } else {

                    if (
                        AdmobUtils.dialog != null
                        && AdmobUtils.dialog!!.isShowing
                    ) {

                        AdmobUtils.dialog!!.dismiss()
                    }
                }

                for (activity in disabledAppOpenList) {

                    if (
                        activity.name ==
                        currentActivity!!.javaClass.name
                    ) {

                        Log.d(
                            TAG,
                            "onStart: activity is disabled"
                        )

                        return@postDelayed
                    }
                }

                showAdIfAvailable(false)

            }, 30)
    }

    fun showDialog(context: Context) {

        isShowingAdsOnResume = true

        isShowingAdsOnResumeBanner = true

        dialogFullScreen = Dialog(context)

        dialogFullScreen?.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        dialogFullScreen?.setContentView(
            R.layout.dialog_onresume
        )

        dialogFullScreen?.setCancelable(false)

        dialogFullScreen?.window?.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )

        dialogFullScreen?.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        try {

            if (
                currentActivity?.isFinishing == false
                && dialogFullScreen != null
                && dialogFullScreen?.isShowing == false
            ) {

                dialogFullScreen?.show()
            }

        } catch (_: Exception) {
        }
    }
}