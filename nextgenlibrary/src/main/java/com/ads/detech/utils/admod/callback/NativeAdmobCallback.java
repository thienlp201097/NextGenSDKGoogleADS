package com.ads.detech.utils.admod.callback;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;

public interface NativeAdmobCallback {
    void onLoadedAndGetNativeAd(NativeAd ad );
    void onNativeAdLoaded();
    void onAdFail(String error);
    void onPaid(AdValue adValue, String adUnitAds);
}
