package com.lib.dktechads

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.GoogleENative
import com.ads.detech.ads.AdsHolder
import com.ads.detech.ads.AdsManager
import com.ads.detech.ads.AdsManager.TAG
import com.ads.detech.ads.AdsManager.dpToPx
import com.ads.detech.ads.gone
import com.ads.detech.ads.visible
import com.ads.detech.utils.admod.RewardHolderAdmob
import com.ads.detech.utils.admod.RewardedInterstitialHolderAdmob
import com.ads.detech.utils.admod.callback.AdLoadCallback
import com.ads.detech.utils.admod.callback.RewardAdCallback
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.common.AdValue

import com.lib.dktechads.databinding.ActivityMainBinding
import com.lib.dktechads.utils.AdsManagerAdmod

class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var bannerContainer: ViewGroup
    lateinit var nativeLoader: MaxNativeAdLoader
    var rewardInterHolder = RewardedInterstitialHolderAdmob("")
    var rewardHolder = RewardHolderAdmob("")

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bannerContainer = findViewById<FrameLayout>(R.id.banner_container)

        binding.btnLoadInter.setOnClickListener {
            AdsManager.loadAdsInterstitialPreload(this@MainActivity,"inter")
        }

        binding.btnShowInter.setOnClickListener {
            AdsManager.showAdsInterstitialPreload(this@MainActivity,"inter",R.layout.ad_native_fullscreen){
                Toast.makeText(this, "Action", Toast.LENGTH_SHORT).show()
            }
        }


        binding.loadAndShowNativeAdmob.setOnClickListener {
            AdsManager.showAdsBannerNative(this@MainActivity,"ads_banner_native",binding.bannerContainer)
        }

        binding.loadAndShowReward.setOnClickListener {
            AdmobUtils.loadAndShowRewardedInterstitialAdWithCallback(this,"",object : RewardAdCallback{
                override fun onAdClosed() {
                    
                }

                override fun onAdShowed() {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AdmobUtils.dismissAdDialog()
                    }, 200)
                }

                override fun onAdFail(message: String?) {
                    
                }

                override fun onEarned() {
                    
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    
                }

            },true)
        }

    }

    override fun onResume() {
        super.onResume()
    }
}