package com.ads.detech.utils.admod.callback

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

public interface InterPreloadingCallback {
    fun onAdClosed()
    fun onEventClickAdClosed()
    fun onAdShowed()
    fun onAdLoaded()
    fun onAdFail(message: String?)
    fun onPaid(adValue: AdValue?, adUnitAds: String?)
}