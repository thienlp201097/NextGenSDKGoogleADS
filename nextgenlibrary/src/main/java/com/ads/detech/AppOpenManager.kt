package com.ads.detech;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.reyun.solar.engine.AdType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    /**
     * Optional callbacks for app open presentation (legacy-style API for this manager).
     */
    public interface FullScreenContentCallback {
        void onAdDismissedFullScreenContent();

        void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError adError);

        void onAdShowedFullScreenContent();
    }

    private static final String TAG = "AppOpenManager";
    private static volatile AppOpenManager INSTANCE;
    private AppOpenAd appResumeAd = null;
    private AppOpenAd splashAd = null;
    private FullScreenContentCallback fullScreenContentCallback;
    private String appResumeAdId;
    private Activity currentActivity;
    private Application myApplication;
    private static boolean isShowingAd = false;
    public boolean isShowingAdsOnResume = false;
    public boolean isShowingAdsOnResumeBanner = false;
    private long appResumeLoadTime = 0;
    private long splashLoadTime = 0;
    private int splashTimeout = 0;
    public long timeToBackground = 0;
    private long waitingTime = 30000L;
    private boolean isInitialized = false;
    private boolean isTestAds = false;
    public boolean isAppResumeEnabled = true;
    private final List<Class> disabledAppOpenList;
    private Class splashActivity;
    private boolean isTimeout = false;
    private static final int TIMEOUT_MSG = 11;
    private Dialog dialogFullScreen;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == TIMEOUT_MSG) {
            isTimeout = true;
        }
        return false;
    });

    public void setTestAds(Boolean isTestAds) {
        this.isTestAds = isTestAds;
    }

    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }

    public AppOpenManager() {
        disabledAppOpenList = new ArrayList<>();
    }

    public static synchronized AppOpenManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppOpenManager();
        }
        return INSTANCE;
    }

    public void init(Application application, String appOpenAdId) {
        this.isInitialized = true;
        this.myApplication = application;
        if (AdmobUtils.isTesting) {
            this.appResumeAdId = application.getString(R.string.test_ads_admob_app_open_new);
        } else {
            this.appResumeAdId = appOpenAdId;
        }
        initAdRequest();
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        if (!isAdAvailable(false) && appOpenAdId != null) {
            fetchAd(false);
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private AdRequest adRequest;

    public void initAdRequest() {
        if (appResumeAdId == null) {
            return;
        }
        adRequest = new AdRequest.Builder(appResumeAdId).build();
    }

    public boolean isShowingAd() {
        return isShowingAd;
    }

    public boolean isShowingAdsOnResume() {
        return isShowingAdsOnResume;
    }

    public void disableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.add(activityClass);
    }

    public void enableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.getName());
        new Handler(Looper.getMainLooper()).postDelayed(() -> disabledAppOpenList.remove(activityClass), 40);
    }

    public void setAppResumeAdId(String appResumeAdId) {
        this.appResumeAdId = appResumeAdId;
    }

    public void setFullScreenContentCallback(FullScreenContentCallback callback) {
        this.fullScreenContentCallback = callback;
    }

    public void removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null;
    }

    boolean isLoading = false;
    public boolean isDismiss = false;

    public void fetchAd(final boolean isSplash) {
        if (isTestAds) {
            Log.d(TAG, "This is a Test Ads, Ads will not be shown | isTestAds = " + isTestAds);
            return;
        }
        Log.d(TAG, "fetchAd: isSplash = " + isSplash);
        if (isAdAvailable(isSplash) || appResumeAdId == null || AppOpenManager.this.appResumeAd != null) {
            Log.d(TAG, "AppOpenManager: Ad is ready or id = null");
            return;
        }
        if (!isLoading) {
            Log.d(TAG, "===fetchAd: Loading");
            isLoading = true;
            if (adRequest == null) {
                initAdRequest();
            }
            if (adRequest == null) {
                isLoading = false;
                return;
            }
            AppOpenAd.load(
                    adRequest,
                    new AdLoadCallback<AppOpenAd>() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(TAG, "AppOpenManager: Loaded");
                            AppOpenManager.this.appResumeAd = ad;
                            AppOpenManager.this.appResumeLoadTime = (new Date()).getTime();
                            isLoading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoading = false;
                            Log.d(TAG, "AppOpenManager: onAdFailedToLoad");
                        }
                    });
        }
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long loadTime, long numHours) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isAdAvailable(boolean isSplash) {
        long loadTime = isSplash ? splashLoadTime : appResumeLoadTime;
        boolean wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4);
        Log.d(TAG, "isAdAvailable: " + wasLoadTimeLessThanNHoursAgo);
        return (isSplash ? splashAd != null : appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d("===ADS", activity.getClass() + "|" + AdActivity.class);
        currentActivity = activity;
        Log.d("===ADS", "Running");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        if (splashActivity == null) {
            if (!activity.getClass().getName().equals(AdActivity.class.getName())) {
                fetchAd(false);
            }
        } else {
            if (!activity.getClass().getName().equals(splashActivity.getName())
                    && !activity.getClass().getName().equals(AdActivity.class.getName())) {
                fetchAd(false);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
        if (dialogFullScreen != null && dialogFullScreen.isShowing()) {
            dialogFullScreen.dismiss();
        }
    }

    public void showAdIfAvailable(final boolean isSplash) {
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.d("===Onresume", "STARTED");
            if (fullScreenContentCallback != null) {
                try {
                    dialogFullScreen.dismiss();
                    dialogFullScreen = null;
                } catch (Exception ignored) {
                }
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
            return;
        }
        Log.d("===Onresume", "FullScreenContentCallback");
        if (!isShowingAd && isAdAvailable(isSplash)) {
            isDismiss = true;
            FullScreenContentCallback callback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // App open callbacks may run on GMA background threads; UI and Handler need main looper.
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Log.d("==TestAOA==", "onResume: true");
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    isDismiss = false;
                                    Log.d("==TestAOA==", "onResume: false");
                                }, 200);
                                isLoading = false;
                                Log.d(TAG, "onAdShowedFullScreenContent: Dismiss");
                                try {
                                    if (dialogFullScreen != null && dialogFullScreen.isShowing()) {
                                        dialogFullScreen.dismiss();
                                    }
                                    dialogFullScreen = null;
                                } catch (Exception ignored) {
                                }
                                appResumeAd = null;
                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback.onAdDismissedFullScreenContent();
                                }
                                isShowingAd = false;
                                fetchAd(isSplash);
                            });
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError adError) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                isLoading = false;
                                isDismiss = false;
                                Log.d(TAG, "onAdShowedFullScreenContent: Show false");
                                try {
                                    if (dialogFullScreen != null && dialogFullScreen.isShowing()) {
                                        dialogFullScreen.dismiss();
                                    }
                                    dialogFullScreen = null;
                                } catch (Exception ignored) {
                                }

                                if (fullScreenContentCallback != null) {
                                    fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                                }
                                fetchAd(isSplash);
                            });
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Log.d(TAG, "onAdShowedFullScreenContent: Show");
                                isShowingAd = true;
                                appResumeAd = null;
                            });
                        }
                    };
            showAdsResume(isSplash, callback);

        } else {
            Log.d(TAG, "Ad is not ready");
            if (!isSplash) {
                fetchAd(false);
            }
        }
    }

    private void showAdsResume(final boolean isSplash, final FullScreenContentCallback callback) {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (appResumeAd != null) {
                    final AppOpenAd ad = appResumeAd;
                    ad.setAdEventCallback(
                            new AppOpenAdEventCallback() {
                                @Override
                                public void onAdPaid(@NonNull AdValue adValue) {
                                    AdmobUtils.adImpressionSolarEngineSDK(adValue,appResumeAdId, AdType.Splash.value,appResumeAd.getResponseInfo());
                                    AdmobUtils.adImpressionFacebookSDK(currentActivity, adValue);
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    callback.onAdDismissedFullScreenContent();
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(
                                        @NonNull FullScreenContentError fullScreenContentError) {
                                    callback.onAdFailedToShowFullScreenContent(fullScreenContentError);
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    callback.onAdShowedFullScreenContent();
                                }
                            });
                    if (currentActivity != null) {
                        showDialog(currentActivity);
                        ad.show(currentActivity);
                    }
                }
            }, 100);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("===OnStart", (System.currentTimeMillis() - timeToBackground) + "");

                if (System.currentTimeMillis() - timeToBackground < waitingTime) {
                    return;
                }
                if (isTestAds) {
                    Log.d(TAG, "This is a Test Ads, Ads will not be shown | isTestAds = " + isTestAds);
                    return;
                }
                if (currentActivity == null) {
                    return;
                }
                if (currentActivity.getClass() == AdActivity.class) {
                    return;
                }

                if (AdmobUtils.isAdShowing) {
                    return;
                }
                if (!AdmobUtils.isShowAds) {
                    return;
                }

                if (!isAppResumeEnabled) {
                    Log.d("===Onresume", "isAppResumeEnabled");
                    return;
                } else {
                    if (AdmobUtils.dialog != null && AdmobUtils.dialog.isShowing()) {
                        AdmobUtils.dialog.dismiss();
                    }
                }

                for (Class activity : disabledAppOpenList) {
                    if (activity.getName().equals(currentActivity.getClass().getName())) {
                        Log.d(TAG, "onStart: activity is disabled");
                        return;
                    }
                }
                showAdIfAvailable(false);
            }
        }, 30);
    }

    public void showDialog(Context context) {
        isShowingAdsOnResume = true;
        isShowingAdsOnResumeBanner = true;
        dialogFullScreen = new Dialog(context);
        dialogFullScreen.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogFullScreen.setContentView(R.layout.dialog_onresume);
        dialogFullScreen.setCancelable(false);
        dialogFullScreen.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialogFullScreen.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        try {
            if (!currentActivity.isFinishing() && dialogFullScreen != null && !dialogFullScreen.isShowing()) {
                dialogFullScreen.show();
            }
        } catch (Exception ignored) {
        }
    }
}
