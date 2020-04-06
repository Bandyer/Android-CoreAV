/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room.adapter_items

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bandyer.core_av.OnStreamListener
import com.bandyer.core_av.Stream
import com.bandyer.core_av.subscriber.Subscriber
import com.bandyer.core_av.subscriber.VideoFpsQuality
import com.bandyer.core_av.subscriber.VideoQuality
import com.bandyer.core_av.subscriber.VideoResolutionQuality
import com.bandyer.core_av.view.OnViewStatusObserver
import com.bandyer.core_av.view.ScaleType
import com.bandyer.core_av.view.StreamView
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.design.bottom_sheet.group_picker.BottomGroupPicker
import com.bandyer.demo_core_av.design.bottom_sheet.group_picker.BottomGroupPickerDelegate
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.viven.imagezoom.ImageZoomHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_subscriber.view.*

/**
 * @author kristiyan
 */
class SubscriberItem(val subscriber: Subscriber) : AbstractItem<SubscriberItem, SubscriberItem.ViewHolder>() {

    //The layout to be used for this type of item
    override fun getLayoutRes(): Int = R.layout.item_subscriber

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun getIdentifier(): Long = subscriber.id.hashCode().toLong()
    override fun getType(): Int = R.id.subscriber_item

    /**
     * our ViewHolder
     */
    class ViewHolder internal constructor(override val containerView: View) : FastAdapter.ViewHolder<SubscriberItem>(containerView), LayoutContainer {

        var screenShotDialog: Dialog? = null
        var subscriber: Subscriber? = null
        var scaleType: ScaleType = ScaleType.BALANCED
        var audioMuted = false
        var videoMuted = false
        var snackbar: Snackbar? = null
        var videoResolutionQuality: VideoResolutionQuality = VideoResolutionQuality.AUTO
        var videoFpsQuality: VideoFpsQuality = VideoFpsQuality.AUTO

        private val viewStatusObserver: OnViewStatusObserver = object : OnViewStatusObserver {
            override fun onRenderingPaused() {
                Log.d("SubView", "onRenderingPaused")
            }

            override fun onRenderingStopped() {
                Log.d("SubView", "onRenderingStopped")
            }

            override fun onRenderingStarted() {
                Log.d("SubView", "onRenderingStarted")
            }

            override fun onFirstFrameRendered() {
                Log.d("SubView", "frameRendered")
            }

            override fun onViewSizeChanged(width: Int, height: Int, rotationDegree: Int) {
                Log.d("SubView", "w " + width + " h " + height + "r " + rotationDegree)
            }

            override fun onFrameCaptured(bitmap: Bitmap?) {
                showImage(bitmap)
            }
        }

        override fun bindView(item: SubscriberItem, payloads: List<Any>) {
            subscriber = item.subscriber
            item.subscriber.setView(containerView.preview, object : OnStreamListener {
                override fun onReadyToPlay(view: StreamView, stream: Stream) {
                    updateAudioVideoButton(stream.hasVideo,
                            stream.hasAudio,
                            stream.isAudioMuted,
                            stream.isVideoMuted)
                    view.play(stream)
                }
            })
            containerView.preview.removeViewStatusObserver(viewStatusObserver)
            containerView.preview.addViewStatusObserver(viewStatusObserver)
            containerView.stream_id!!.text = item.subscriber.stream.streamId
            containerView.stream_id!!.isSelected = true

            ImageZoomHelper.setViewZoomable(containerView.preview)
            containerView.preview.bringToFront(true)

            containerView.changeQualityButton.setOnClickListener { v -> onChangeQuality(v.context as BaseActivity, item) }

            containerView.captureFrameButton.setOnClickListener { containerView.preview!!.captureFrame() }

            containerView.changeScaleTypeButton.setOnClickListener {
                val changed: ScaleType = when (scaleType) {
                    ScaleType.BALANCED -> ScaleType.FIT
                    ScaleType.FIT -> ScaleType.FILL
                    ScaleType.FILL -> ScaleType.BALANCED
                    else -> ScaleType.BALANCED
                }
                scaleType = changed
                containerView.preview!!.setScaleType(scaleType)
                snackbar = Snackbar.make(it, "Scale Type changed to: $changed", Snackbar.LENGTH_SHORT)
                snackbar!!.show()
            }

            containerView.videoButton.setOnClickListener {
                videoMuted = !videoMuted
                containerView.videoButton.setImageResource(if (videoMuted) R.drawable.ic_videocam_off else R.drawable.ic_videocam)
                containerView.preview!!.disableVideoRendering(videoMuted)
                subscriber!!.disableVideo(videoMuted)
            }

            containerView.audioButton.setOnClickListener {
                audioMuted = !audioMuted
                containerView.audioButton.setImageResource(if (audioMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
                containerView.preview!!.disableAudioPlaying(audioMuted)
                subscriber!!.disableAudio(audioMuted)
            }
        }

        private fun showImage(bitmap: Bitmap?) {
            if (bitmap == null) {
                Log.e("Snapshot", "failed")
                return
            }
            val context: Context = containerView.preview.context
            screenShotDialog = Dialog(context)
            screenShotDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            screenShotDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val imageView = ImageView(context)
            imageView.setImageBitmap(bitmap)
            screenShotDialog!!.addContentView(imageView, RelativeLayout.LayoutParams(bitmap.width, bitmap.height))
            screenShotDialog!!.show()
        }

        override fun unbindView(item: SubscriberItem) {
            item.subscriber.releaseView()
            audioMuted = false
            videoMuted = false
            scaleType = ScaleType.BALANCED
            containerView.stream_id!!.text = ""
            containerView.videoButton!!.visibility = View.VISIBLE
            containerView.audioButton!!.visibility = View.VISIBLE
            if (snackbar != null) snackbar!!.dismiss()
            if (screenShotDialog != null) screenShotDialog!!.dismiss()
            containerView.preview.removeViewStatusObserver(viewStatusObserver)
        }

        fun updateAudioVideoButton(previewHasVideo: Boolean?, previewHasAudio: Boolean?, audioMuted: Boolean, videoMuted: Boolean) {
            this.audioMuted = audioMuted
            this.videoMuted = videoMuted
            if (!previewHasVideo!!) containerView.videoButton!!.visibility = View.GONE
            if (!previewHasAudio!!) containerView.audioButton!!.visibility = View.GONE
            containerView.audioButton!!.setImageResource(if (audioMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
            containerView.videoButton!!.setImageResource(if (videoMuted) R.drawable.ic_videocam_off else R.drawable.ic_videocam)
        }

        private fun onChangeQuality(activity: BaseActivity, item: SubscriberItem) {
            val qualityPicker: BottomGroupPicker<VideoQuality> = BottomGroupPicker(activity, activity.getString(R.string.video_quality_picker_title), 2)
            val fps: List<VideoQuality> = VideoFpsQuality.values().toList()
            val resolutionQualities: List<VideoQuality> = VideoResolutionQuality.values().toList()
            qualityPicker
                    .addGroupItems(activity.getString(R.string.fps_title), fps, fps.indexOf(videoFpsQuality))
                    .addGroupItems(activity.getString(R.string.resolution_title), resolutionQualities, resolutionQualities.indexOf(videoResolutionQuality))
                    .setDelegate(object : BottomGroupPickerDelegate<VideoQuality> {
                        override fun onClicked(itemsSelected: List<VideoQuality>) {
                            val pos = if (itemsSelected[0] is VideoResolutionQuality) 0 else 1
                            val fpos = if (itemsSelected[0] is VideoFpsQuality) 0 else 1
                            videoResolutionQuality = itemsSelected[pos] as VideoResolutionQuality
                            videoFpsQuality = itemsSelected[fpos] as VideoFpsQuality
                            item.subscriber.setQuality(videoResolutionQuality, videoFpsQuality)
                        }
                    }).show()
        }

        companion object {
            private fun dp2px(ctx: Context, dp: Float): Int {
                val scale = ctx.resources.displayMetrics.density
                return (dp * scale + 0.5f).toInt()
            }
        }
    }

    class SubscriberItemClickListener : ClickEventHook<SubscriberItem>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is ViewHolder) viewHolder.itemView else null
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<SubscriberItem>, item: SubscriberItem) {
            v.subscription_options!!.visibility = if (v.subscription_options!!.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }
}