package com.ads.detech.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

class InterHolderAdmob(var ads: String) {
    var inter: InterstitialAd? = null
    val mutable: MutableLiveData<InterstitialAd> = MutableLiveData()
    var check = false
}