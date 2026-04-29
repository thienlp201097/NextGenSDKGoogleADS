package com.ads.detech.track

import android.content.Context
import android.util.Log
import com.airbnb.lottie.animation.content.Content
import com.tiktok.TikTokBusinessSdk
import com.tiktok.appevents.base.EventName

object TiktokSDKUtils {
    fun initTiktokSDK(context: Context,accessToken : String, packageName : String, tiktokAppId : String){
        val ttConfig = TikTokBusinessSdk.TTConfig(context, accessToken)
            .setAppId(packageName) // Android package name hoặc iOS App Store ID
            .setTTAppId(tiktokAppId) // TikTok App ID (từ TikTok Events Manager)

        TikTokBusinessSdk.initializeSdk(ttConfig)

        TikTokBusinessSdk.initializeSdk(ttConfig, object : TikTokBusinessSdk.TTInitCallback {
            override fun success() {
                // ✅ SDK khởi tạo thành công
                Log.e("TikTokSDK", "Init Tiktok success")
            }

            override fun fail(code: Int, msg: String?) {
                // ❌ SDK khởi tạo thất bại
                Log.e("TikTokSDK", "Init Tiktok failed: $code - $msg")
            }
        })
        TikTokBusinessSdk.startTrack()
    }

    fun tiktokTrackEvent(){
        TikTokBusinessSdk.trackTTEvent(EventName.IN_APP_AD_IMPR)
    }
}