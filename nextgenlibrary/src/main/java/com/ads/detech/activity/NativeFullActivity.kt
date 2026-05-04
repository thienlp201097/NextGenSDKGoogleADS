package com.ads.detech.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.ads.detech.AdmobUtils
import com.ads.detech.AdmobUtils.AdsNativeCallBackAdmod
import com.ads.detech.R
import com.ads.detech.ads.AdsHolder
import com.ads.detech.ads.inVisible
import com.ads.detech.ads.visible
import com.ads.detech.databinding.ActivityNativeFullBinding
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
fun Context.getStatusBarHeight(): Int {
    val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
}
class NativeFullActivity : AppCompatActivity() {
    val binding by lazy { ActivityNativeFullBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val statusHeight = getStatusBarHeight()

        binding.view3.layoutParams = binding.view3.layoutParams.apply {
            height = statusHeight
        }
        binding.btnClose.visible()

        binding.btnClose.setOnClickListener {
            finish()
        }

        val layout = intent.getIntExtra("layout_native",R.layout.ad_unified)
        AdmobUtils.showNativeFullScreenAdsWithLayout(this,AdsHolder.NATIVE_FULL,binding.flNative,layout,object :
            AdsNativeCallBackAdmod{
            override fun NativeLoaded() {
                
            }

            override fun NativeFailed(massage: String) {
                finish()
            }

            override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                
            }
        })
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    fun countDownFlow(from: Int = 8): Flow<Int> = flow {
        var current = from
        while (current > 0) {
            emit(current)
            delay(1000)
            current--
        }
    }
}