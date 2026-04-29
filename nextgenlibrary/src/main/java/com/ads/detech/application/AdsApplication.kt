package com.ads.detech.application

import android.app.Activity
import android.app.Application
import android.os.Bundle

abstract class AdsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        onCreateApplication()
        registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
    }

    abstract fun onCreateApplication()

    private class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}