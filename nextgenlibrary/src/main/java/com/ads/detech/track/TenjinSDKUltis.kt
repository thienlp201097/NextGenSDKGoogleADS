package com.ads.detech.track

import android.annotation.SuppressLint
import android.content.Context
import com.tenjin.android.TenjinSDK


object TenjinSDKUtil {
    @SuppressLint("StaticFieldLeak")
    lateinit var instance: TenjinSDK
        private set

    fun init(context: Context, apiKey: String) {
        if (!::instance.isInitialized) {
            instance = TenjinSDK.getInstance(context.applicationContext, apiKey)
            instance.setAppStore(TenjinSDK.AppStoreType.googleplay)
            instance.connect()
        }
    }

}