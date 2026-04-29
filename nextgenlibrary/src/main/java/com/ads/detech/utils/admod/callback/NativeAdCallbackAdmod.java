package com.ads.detech.utils.admod.callback;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;

public interface NativeAdCallbackAdmod {
    void onLoadedAndGetNativeAd(NativeAd ad );
    void onNativeAdLoaded();
    void onAdFail();
    void onAdPaid(AdValue adValue);
}
