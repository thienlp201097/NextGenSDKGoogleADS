package com.ads.detech.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd

open class RewardHolderAdmob(var ads: String) {
    var inter: RewardedAd? = null
    val mutable: MutableLiveData<RewardedAd> = MutableLiveData(null)
    var isLoading = false
}