package com.lib.dktechads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lib.dktechads.databinding.ActivityMain2Binding
import com.lib.dktechads.utils.AdsManagerAdmod

class MainActivity2 : AppCompatActivity() {
    val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AdsManagerAdmod.showAdsNativeFullScreen(this,AdsManagerAdmod.nativeHolderFull,binding.bannerContainer)

    }
    override fun onResume() {
        super.onResume()
    }
}