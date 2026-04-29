package com.ads.detech.utils.admod.callback;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;

public interface AdCallbackNew {
    void onAdClosed();
    void onEventClickAdClosed();
    void onAdShowed();
    void onAdLoaded();
    void onAdFail();
    void onPaid(AdValue adValue, String adUnitAds);
}
