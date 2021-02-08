/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room.adapter_items

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bandyer.core_av.OnStreamListener
import com.bandyer.core_av.Stream
import com.bandyer.core_av.capturer.Capturer
import com.bandyer.core_av.capturer.isCamera
import com.bandyer.core_av.capturer.video.provider.FrameQuality
import com.bandyer.core_av.publisher.*
import com.bandyer.core_av.capturer.video.provider.camera.CameraFrameProvider
import com.bandyer.core_av.publisher.*
import com.bandyer.core_av.subscriber.VideoFpsQuality
import com.bandyer.core_av.subscriber.VideoQuality
import com.bandyer.core_av.subscriber.VideoResolutionQuality
import com.bandyer.core_av.view.OnViewStatusObserver
import com.bandyer.core_av.view.StreamView
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.design.bottom_sheet.QualityPicker
import com.bandyer.demo_core_av.design.bottom_sheet.group_picker.BottomGroupPicker
import com.bandyer.demo_core_av.design.bottom_sheet.group_picker.BottomGroupPickerDelegate
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.viven.imagezoom.ImageZoomHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_publisher.view.*
import kotlinx.android.synthetic.main.item_publisher.view.captureFrameButton
import kotlinx.android.synthetic.main.item_publisher.view.changeQualityButton
import kotlinx.android.synthetic.main.item_publisher.view.preview
import kotlinx.android.synthetic.main.item_publisher.view.stream_id
import kotlinx.android.synthetic.main.item_publisher.view.videoButton
import kotlinx.android.synthetic.main.item_subscriber.view.*

/**
 * @author kristiyan
 */
class PublisherItem(val publisher: Publisher, val capturer: Capturer<*, *>) : AbstractItem<PublisherItem, PublisherItem.ViewHolder>() {

    //The layout to be used for this type of item
    override fun getLayoutRes(): Int = R.layout.item_publisher

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)
    override fun getIdentifier(): Long = publisher.id.hashCode().toLong()
    override fun getType(): Int = R.id.publisher_item

    /**
     * our ViewHolder
     */
    class ViewHolder internal constructor(override val containerView: View) : FastAdapter.ViewHolder<PublisherItem>(containerView), LayoutContainer {
        private var audioMuted = false
        private var videoMuted = false
        private var capturer: Capturer<*, *>? = null
        private var publisher: Publisher? = null
        private var screenShotDialog: Dialog? = null

        private val viewStatusObserver: OnViewStatusObserver = object : OnViewStatusObserver {
            override fun onRenderingPaused() {
                Log.d("PubView", "onRenderingPaused")
            }

            override fun onRenderingStopped() {
                Log.d("PubView", "onRenderingStopped")
            }

            override fun onRenderingStarted() {
                Log.d("PubView", "onRenderingStarted")
            }

            override fun onViewSizeChanged(width: Int, height: Int, rotationDegree: Int) {
                Log.d("PubView", "w " + width + " h " + height + "r " + rotationDegree)
            }

            override fun onFirstFrameRendered() {
                Log.d("PubView", "frameRendered")
            }

            override fun onFrameCaptured(bitmap: Bitmap?) {
                showImage(bitmap)
            }
        }

        override fun bindView(item: PublisherItem, payloads: List<Any>) {
            capturer = item.capturer
            publisher = item.publisher
            containerView.preview.removeViewStatusObserver(viewStatusObserver)
            containerView.preview.addViewStatusObserver(viewStatusObserver)
            item.publisher.setView(containerView.preview, object : OnStreamListener {
                override fun onReadyToPlay(view: StreamView, stream: Stream) {
                    updateAudioVideoButton(stream.hasVideo,
                            stream.hasAudio,
                            stream.isAudioMuted,
                            stream.isVideoMuted)
                    view.play(stream)
                }
            })
            containerView.stream_id.text = item.publisher.id
            containerView.stream_id!!.isSelected = true

            containerView.micButton.setOnClickListener {
                audioMuted = !audioMuted
                containerView.micButton.setImageResource(if (audioMuted) R.drawable.ic_mic_off else R.drawable.ic_mic)
                containerView.preview.disableAudioPlaying(audioMuted)
                publisher!!.disableAudio(audioMuted)
            }

            containerView.videoButton.setOnClickListener {
                videoMuted = !videoMuted
                containerView.videoButton.setImageResource(if (videoMuted) R.drawable.ic_videocam_off else R.drawable.ic_videocam)
                containerView.preview.disableVideoRendering(videoMuted)
                publisher!!.disableVideo(videoMuted)
            }
            capturer!!.video.isCamera {
                containerView.switchCameraButton.setOnClickListener { frameProvider.switchVideoFeeder() }
            }
            containerView.recordButton.setOnClickListener {
                recordListener ?: return@setOnClickListener
                publisher ?: return@setOnClickListener
                with(publisher!!) {
                    if (isRecording) stopRecording(recordListener!!) else startRecording(recordListener!!)
                }
            }
            containerView.captureFrameButton.setOnClickListener { containerView.preview.captureFrame() }
            containerView.broadcastButton.setOnClickListener {
                broadcastListener ?: return@setOnClickListener
                publisher ?: return@setOnClickListener
                with(publisher!!) {
                    if (isBroadcasting) stopBroadcast(broadcastListener!!) else startBroadcast(broadcastListener!!)
                }
            }

            containerView.changeQualityButton.visibility = if (capturer?.video == null) View.GONE else View.VISIBLE
            containerView.changeQualityButton.setOnClickListener { v -> onChangeQuality(v.context as BaseActivity, item) }
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

        override fun unbindView(item: PublisherItem) {
            item.publisher.releaseView()
            containerView.stream_id!!.text = null
            containerView.preview.stop()
            containerView.switchCameraButton!!.visibility = View.VISIBLE
            containerView.videoButton!!.visibility = View.VISIBLE
            containerView.micButton!!.visibility = View.VISIBLE
            audioMuted = false
            videoMuted = false
            capturer = null
            publisher = null
            broadcastListener = null
            recordListener = null
            if (screenShotDialog != null) screenShotDialog!!.dismiss()
        }

        fun updateAudioVideoButton(previewHasVideo: Boolean?, previewHasAudio: Boolean?, audioMuted: Boolean, videoMuted: Boolean) {
            this.audioMuted = audioMuted
            this.videoMuted = videoMuted
            if (!previewHasVideo!!) {
                containerView.videoButton!!.visibility = View.GONE
                containerView.switchCameraButton!!.visibility = View.GONE
            }
            if (!previewHasAudio!!) containerView.micButton!!.visibility = View.GONE
            containerView.micButton!!.setImageResource(if (audioMuted) R.drawable.ic_mic_off else R.drawable.ic_mic)
            containerView.videoButton!!.setImageResource(if (videoMuted) R.drawable.ic_videocam_off else R.drawable.ic_videocam)
        }

        var recordListener: RecordingListener? = object : RecordingListener {
            override fun onSuccess(recordId: String, isRecording: Boolean) {
                val message = if (isRecording) "Started" else "Stopped"
                Toast.makeText(containerView.preview.context, "$message recording", Toast.LENGTH_SHORT).show()
                ImageViewCompat.setImageTintList(containerView.recordButton!!, ColorStateList.valueOf(if (isRecording) Color.RED else Color.WHITE))
            }

            override fun onError(recordId: String?, isRecording: Boolean, reason: RecordingException) {
                ImageViewCompat.setImageTintList(containerView.recordButton!!, ColorStateList.valueOf(if (isRecording) Color.RED else Color.WHITE))
            }
        }

        var broadcastListener: BroadcastingListener? = object : BroadcastingListener {

            override fun onSuccess(broadcastId: String, isBroadcasting: Boolean) {
                val message = if (isBroadcasting) "Started" else "Stopped"
                Toast.makeText(containerView.preview.context, "$message broadcasting", Toast.LENGTH_SHORT).show()
                ImageViewCompat.setImageTintList(containerView.broadcastButton!!, ColorStateList.valueOf(if (isBroadcasting) Color.RED else Color.WHITE))
            }

            override fun onError(broadcastId: String?, isBroadcasting: Boolean, reason: BroadcastingException) {
                ImageViewCompat.setImageTintList(containerView.broadcastButton!!, ColorStateList.valueOf(if (isBroadcasting) Color.RED else Color.WHITE))
            }
        }

        private fun onChangeQuality(activity: BaseActivity, item: PublisherItem) {
            val qualityPicker = QualityPicker(activity)
            qualityPicker.onChanged { width, height, fps, optimized ->
                val frameQuality = FrameQuality(width, height, fps)
                item.capturer.video?.frameProvider?.frameQuality = frameQuality
                item.capturer.video?.isCamera {
                    if (optimized) frameProvider.getNearestCaptureQualitySupported(frameQuality)
                }
            }.show()
        }

        init {
            ImageZoomHelper.setViewZoomable(containerView.preview)
        }
    }

    class PublisherItemClickListener : ClickEventHook<PublisherItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is ViewHolder) viewHolder.itemView else null
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<PublisherItem>, item: PublisherItem) {
            v.publishing_options.visibility = if (v.publishing_options.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }
}