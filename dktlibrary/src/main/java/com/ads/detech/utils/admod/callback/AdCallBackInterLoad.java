package com.ads.detech.utils.admod.callback;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;

public interface AdCallBackInterLoad {
    void onAdClosed();
    void onEventClickAdClosed();
    void onAdShowed();
    void onAdLoaded(InterstitialAd interstitialAd, boolean isLoading);
    void onAdFail(String message);
    void onPaid(AdValue adValue, String adUnitAds);
}
