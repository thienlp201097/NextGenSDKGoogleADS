package com.ads.detech.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

class NativeHolderAdmob(var ads: String){
    var nativeAd : NativeAd?= null
    var isLoad = false
    var native_mutable: MutableLiveData<NativeAd> = MutableLiveData()
}