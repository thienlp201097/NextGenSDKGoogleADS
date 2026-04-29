package com.ads.detech.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ads.detech.BuildConfig
import com.ads.detech.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.xmlpull.v1.XmlPullParser


object FireBaseConfig {

    private val configMap: MutableMap<String, String> = mutableMapOf()

    fun initRemoteConfig(remote_config_defaults : Int,completeListener: CompleteListener) {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(remote_config_defaults)

        mFirebaseRemoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                mFirebaseRemoteConfig.activate().addOnCompleteListener {
                    completeListener.onComplete()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
            }
        })

        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Handler(Looper.getMainLooper()).postDelayed({
                    completeListener.onComplete()
                }, 2000)
            }
        }
    }

    interface CompleteListener {
        fun onComplete()
    }

    fun getValue(key: String): String {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        if (BuildConfig.DEBUG) {
            Log.d("==FireBaseConfig==", "getValue: $key ${mFirebaseRemoteConfig.getString(key)}")
        }
        return mFirebaseRemoteConfig.getString(key)
    }

    @SuppressLint("MissingPermission")
    fun logEvent(context: Context, eventName : String, version : Int){
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString("onEvent", context.javaClass.simpleName)
        firebaseAnalytics.logEvent(eventName + "_" + version, bundle)
        Log.d("===Event", eventName + "_" + version)
    }
}