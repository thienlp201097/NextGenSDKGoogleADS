package com.ads.detech

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.common.VideoController
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class NativeFunc {
    companion object {

        fun populateNativeAdView(
            nativeAd: NativeAd,
            adView: NativeAdView,
            size: GoogleENative
        ) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            val adMediaView = adView.findViewById<MediaView>(R.id.ad_media)
            if (nativeAd.mediaContent != null && size == GoogleENative.UNIFIED_MEDIUM) {
                adMediaView?.mediaContent = nativeAd.mediaContent
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE

            }else{
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = nativeAd.callToAction
            }


            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd!!.icon!!.drawable
                    )
                    adView!!.iconView!!.visibility = View.VISIBLE
                }
            }

            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }

            adMediaView?.let { mv ->
                adView.registerNativeAd(nativeAd, mv)
                val mc = nativeAd.mediaContent
                val videoController = mc?.videoController
                if (videoController != null && mc != null && mc.hasVideoContent) {
                    videoController.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }
        }

        fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
            val adMediaView = adView.findViewById<MediaView>(R.id.ad_media)

            // Set other ad assets.
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            val imageView = adView.findViewById<ImageView>(R.id.ad_app_icon)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imageView.clipToOutline = true
            }
            adView.iconView = imageView

            // The headline and mediaContent are guaranteed to be in every NativeAd.
            (adView.headlineView as TextView?)!!.text = nativeAd.headline
            adMediaView?.mediaContent = nativeAd.mediaContent

            // These assets aren't guaranteed to be in every NativeAd, so it's important to
            // check before trying to display them.
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView?)!!.text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE
            } else {
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button?)!!.text = nativeAd.callToAction
            }
            if (nativeAd.icon == null) {
                adView.iconView!!.visibility = View.GONE
            } else {
                (adView.iconView as ImageView?)!!.setImageDrawable(
                    nativeAd.icon!!.drawable
                )
                adView.iconView!!.visibility = View.VISIBLE
            }

            // This method tells the Google Mobile Ads SDK that you have finished populating your
            // native ad view with this native ad.

            // Get the video controller for the ad. One will always be provided,
            // even if the ad doesn't have a video asset.
            adMediaView?.let { mv ->
                adView.registerNativeAd(nativeAd, mv)
                val videoController = nativeAd.mediaContent.videoController
                if (videoController != null && nativeAd.mediaContent.hasVideoContent) {
                    videoController.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }
        }

        fun populateNativeAdViewNoBtn(
            nativeAd: NativeAd,
            adView: NativeAdView,
            size: GoogleENative
        ) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            val adMediaView = adView.findViewById<MediaView>(R.id.ad_media)
            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            if (nativeAd.mediaContent != null) {
                if (size == GoogleENative.UNIFIED_MEDIUM) {
                    adMediaView?.let { container ->
//                        it.setImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
                        val mediaContent = nativeAd.mediaContent
                        if (mediaContent != null && mediaContent.hasVideoContent) {
                            val nestedMediaView = MediaView(container.context)
                            nestedMediaView.mediaContent = mediaContent
                            container.addView(nestedMediaView)
                        }
                    }
                }
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            (adView.bodyView as TextView).text = nativeAd.body
            (adView.callToActionView as Button).text = nativeAd.callToAction

            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }

            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }

            adMediaView?.let { mv ->
                adView.registerNativeAd(nativeAd, mv)
                val mc = nativeAd.mediaContent
                val videoController = mc?.videoController
                if (videoController != null && mc != null && mc.hasVideoContent) {
                    videoController.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }
        }

        fun populateNativeAdViewClose(
            nativeAd: NativeAd,
            adView: NativeAdView,
            size: GoogleENative, nativeAdCallbackNew: AdmobUtils.NativeAdCallbackNew
        ) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            val adMediaView = adView.findViewById<MediaView>(R.id.ad_media)
            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            if (nativeAd.mediaContent != null && size == GoogleENative.UNIFIED_MEDIUM) {
                adMediaView?.mediaContent = nativeAd.mediaContent
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE

            }else{
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = nativeAd.callToAction
            }


            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }
            adView.findViewById<ImageView>(R.id.ivClose)?.let { ivClose ->
                ivClose.setOnClickListener {
                    nativeAdCallbackNew.onClickAds()
                    it.visibility = View.GONE
                    adView.findViewById<MediaView>(R.id.ad_media)?.let { ad_media ->
                        ad_media.visibility = View.GONE
                    }
                }
            }
            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }

            adMediaView?.let { mv ->
                adView.registerNativeAd(nativeAd, mv)
                val mc = nativeAd.mediaContent
                val videoController = mc?.videoController
                if (videoController != null && mc != null && mc.hasVideoContent) {
                    videoController.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }
        }


    }

}



fun Int.dpToPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}