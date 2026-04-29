package com.ads.detech.utils.admod.callback;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;

public interface RewardAdCallback {
    void onAdClosed();
    void onAdShowed();
    void onAdFail(String message);
    void onEarned();
    void onPaid(AdValue adValue, String adUnitAds);

}
