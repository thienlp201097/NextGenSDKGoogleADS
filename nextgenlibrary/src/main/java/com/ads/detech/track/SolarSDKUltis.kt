package com.ads.detech.track

import android.content.Context
import android.util.Log
import com.reyun.solar.engine.SolarEngineConfig
import com.reyun.solar.engine.SolarEngineManager

object SolarSDKUtils {
    fun initSdk(
        context: Context,
        key: String,
        onInitCompleted: ((Boolean) -> Unit)? = null
    ) {
        val config = SolarEngineConfig.Builder().build()
        SolarEngineManager.getInstance().initialize(
            context,
            key,
            config
        ) { code ->
            // tuỳ SDK SolarEngine, thường code == 0 là success
            val success = code == 0
            if (success) {
                Log.d("==SolarEngine==", "SDK init success")
            } else {
                Log.d("==SolarEngine==", "SDK init failed")
            }
            onInitCompleted?.invoke(success)
        }
    }

    fun preInitSolarSdk(context: Context,key: String){
        SolarEngineManager.getInstance().preInit(context, key);
    }
}
