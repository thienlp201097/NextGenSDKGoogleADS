package com.lib.dktechads.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.ads.detech.AdmobUtils
import com.ads.detech.AdmobUtils.AdsNativeCallBackAdmod
import com.ads.detech.AppOpenManager
import com.ads.detech.GoogleENative
import com.ads.detech.utils.Utils
import com.ads.detech.utils.admod.InterHolderAdmob
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.ads.detech.utils.admod.callback.AdCallBackInterLoad
import com.ads.detech.utils.admod.callback.AdsInterCallBack
import com.ads.detech.utils.admod.callback.NativeAdmobCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.lib.dktechads.R

object AdsManagerAdmod {
    var nativeHolder = NativeHolderAdmob("ca-app-pub-3940256099942544/2247696110")
    var nativeHolderFull = NativeHolderAdmob("ca-app-pub-3940256099942544/7342230711")
    var interholder = InterHolderAdmob("ca-app-pub-3940256099942544/1033173712")

    fun loadInter(context: Context, interHolder: InterHolderAdmob) {

    }


    fun showInter(
        context: Context,
        interHolder: InterHolderAdmob,
        adListener: AdListener,
        enableLoadingDialog: Boolean
    ) {

    }

    fun loadAdsNativeNew(context: Context, holder: NativeHolderAdmob) {
        AdmobUtils.loadAndGetNativeAds(
            context,
            holder,
            object : NativeAdmobCallback {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {
                }

                override fun onNativeAdLoaded() {
                }

                override fun onAdFail(error: String?) {
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }
            })
    }

    fun showNative(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeAdsWithLayout(activity, holder, viewGroup, R.layout.ad_unified_medium, GoogleENative.UNIFIED_BANNER, object : AdmobUtils.AdsNativeCallBackAdmod {
            override fun NativeLoaded() {
                Utils.getInstance().showMessenger(activity, "onNativeShow")
            }

            override fun NativeFailed(massage: String) {
                Utils.getInstance().showMessenger(activity, "onAdsFailed")
            }

            override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

            }
        })
    }

    fun showAdsNativeFullScreen(activity: Activity, nativeHolder: NativeHolderAdmob,viewGroup: ViewGroup){
        AdmobUtils.showNativeFullScreenAdsWithLayout(activity,nativeHolder,viewGroup,
            R.layout.ad_native_fullscreen,object :
                AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    Log.d("==full==", "NativeLoaded: ")
                }

                override fun NativeFailed(massage: String) {
                    Log.d("==full==", "NativeFailed: $massage")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

            })
    }



    interface AdListener {
        fun onAdClosed()
        fun onFailed()
    }
}
