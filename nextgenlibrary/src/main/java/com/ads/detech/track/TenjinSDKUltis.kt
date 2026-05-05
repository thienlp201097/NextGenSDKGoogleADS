package com.ads.detech.track

import android.annotation.SuppressLint
import android.content.Context
import com.tenjin.android.TenjinSDK


object TenjinSDKUtil {
    @SuppressLint("StaticFieldLeak")
    var instance: TenjinSDK? = null
        private set

    fun init(context: Context, apiKey: String) {
        if (instance == null) {
            instance = TenjinSDK.getInstance(context.applicationContext, apiKey)
            instance?.setAppStore(TenjinSDK.AppStoreType.googleplay)
            instance?.connect()
        }
    }

}