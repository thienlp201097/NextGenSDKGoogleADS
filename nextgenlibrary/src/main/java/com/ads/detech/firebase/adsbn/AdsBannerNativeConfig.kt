package com.ads.detech.firebase.adsbn

data class AdsBannerNativeConfig(
    val ads_type: String,
    val native_type: String,
    val organic: Boolean,
    val units: BannerNativeUnits,
)