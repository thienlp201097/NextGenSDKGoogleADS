package com.ads.detech.track

import android.content.Context
import com.reyun.solar.engine.SolarEngineConfig
import com.reyun.solar.engine.SolarEngineManager

object SolarSDKUtils {
    fun initSdk(context: Context,key: String){
        val config = SolarEngineConfig.Builder().build()
        SolarEngineManager.getInstance().initialize(
            context, key, config
        ) { code ->
        }
    }

    fun preInitSolarSdk(context: Context,key: String){
        SolarEngineManager.getInstance().preInit(context, key);
    }
}
