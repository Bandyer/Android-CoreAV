/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.bandyer.android_audiosession.AudioOutputDeviceType
import com.bandyer.android_audiosession.AudioSession
import com.bandyer.android_audiosession.AudioSessionOptions
import com.bandyer.android_audiosession.audiosession.AudioSessionListener
import com.bandyer.android_common.proximity_listener.ProximitySensorListener
import com.bandyer.core_av.Stream
import com.bandyer.core_av.capturer.Capturer
import com.bandyer.core_av.capturer.CapturerException
import com.bandyer.core_av.capturer.CapturerObserver
import com.bandyer.core_av.capturer.CapturerOptions
import com.bandyer.core_av.capturer.mix.CapturerAudioVideo
import com.bandyer.core_av.capturer.video.screen.CapturerScreenVideo
import com.bandyer.core_av.publisher.Publisher
import com.bandyer.core_av.publisher.PublisherObserver
import com.bandyer.core_av.publisher.PublisherState
import com.bandyer.core_av.room.*
import com.bandyer.core_av.subscriber.Subscriber
import com.bandyer.core_av.subscriber.SubscriberObserver
import com.bandyer.core_av.subscriber.SubscriberState
import com.bandyer.core_av.utils.logging.InternalStatsLogger
import com.bandyer.core_av.utils.logging.InternalStatsTypes
import com.bandyer.demo_core_av.App
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.StatsPagerAdapter
import com.bandyer.demo_core_av.room.adapter_items.PublisherItem
import com.bandyer.demo_core_av.room.adapter_items.SubscriberItem
import com.bandyer.demo_core_av.room.utils.ScreenSharingUtils
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.viven.imagezoom.ImageZoomHelper
import kotlinx.android.synthetic.main.activity_auto_pubsub_room.*
import java.util.*

/**
 * @author kristiyan
 */
class AutoPubSubRoomActivity : BaseActivity(), RoomObserver, SubscriberObserver, PublisherObserver, InternalStatsLogger, CapturerObserver {

    private val pubSubsAdapter: FastItemAdapter<IItem<*, *>> = FastItemAdapter()
    private var room: Room? = null
    private var imageZoomHelper: ImageZoomHelper? = null
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_pubsub_room)
        val token = intent.getStringExtra(ROOM_TOKEN)!!
        imageZoomHelper = ImageZoomHelper(this)
        val layoutManager = GridLayoutManager(this, 4, LinearLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int { // if is the first item then it weights 4 else 2
                return if (position == 0) 4 else 2
            }
        }
        pubsubs!!.layoutManager = layoutManager
        pubsubs!!.adapter = pubSubsAdapter
        setAdapterListeners()
        AudioSession.getInstance().startWithOptions(
                this,
                AudioSessionOptions.Builder()
                        .withDefaultSpeakerPhoneOutputHardWareDevice()
                        .build(),
                object : AudioSessionListener {
                    override fun onOutputDeviceConnected(oldAudioOutputDevice: AudioOutputDeviceType, connectedAudioOutputDevice: AudioOutputDeviceType, availableOutputs: List<AudioOutputDeviceType>) {
                        Log.d("AudioSession", "changed from old: " + oldAudioOutputDevice.name + " to connected: " + connectedAudioOutputDevice.name)
                        if (snackbar != null) snackbar!!.dismiss()
                        snackbar = Snackbar.make(pubsubs!!, connectedAudioOutputDevice.name, Snackbar.LENGTH_SHORT)
                        snackbar!!.show()
                    }

                    override fun onOutputDeviceAttached(currentAudioOutputDevice: AudioOutputDeviceType, attachedAudioOutputDevice: AudioOutputDeviceType, availableOutputs: List<AudioOutputDeviceType>) {
                        Log.d("AudioSession", "current: " + currentAudioOutputDevice.name + " attached audioDevice: " + attachedAudioOutputDevice.name)
                    }

                    override fun onOutputDeviceDetached(currentAudioOutputDevice: AudioOutputDeviceType, detachedAudioOutputDevice: AudioOutputDeviceType, availableOutputs: List<AudioOutputDeviceType>) {
                        Log.d("AudioSession", "current: " + currentAudioOutputDevice.name + " detached audioDevice: " + detachedAudioOutputDevice.name)
                    }
                }, object : ProximitySensorListener {
            override fun onProximitySensorChanged(isNear: Boolean) {
                Log.d("ProximitySensor", "proximity triggered: $isNear")
            }
        })
        Capturer.Registry.addCapturerObserver(this)
        room = Room.Registry.get(RoomToken(token))
        room!!.addRoomObserver(this)
        room!!.muteAllSubscribersAudio(intent.getBooleanExtra(ROOM_AUDIO_MUTED, false))
        room!!.join()


        close_room.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        screenShare.setOnClickListener {
            if (room?.roomState !== RoomState.CONNECTED) return@setOnClickListener
            // on Android Q before launching a screenShare a notification MUST be shown as foreground service with mediaProjection
            ScreenSharingUtils.showScreenShareNotification(this)
            val capturerScreenVideo: Capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withAudio().withScreenShare())
            capturerScreenVideo.start()
        }
    }

    override fun onResume() {
        super.onResume()
        Capturer.Registry.capturers.forEach { it.resume() }
    }

    override fun onPause() {
        super.onPause()
        for (capturer in Capturer.Registry.capturers) {
            if (capturer is CapturerScreenVideo) continue
            if (capturer is CapturerAudioVideo) capturer.pause(video = true, audio = false) else capturer.pause()
        }
    }

    override fun onRoomActorUpdateStream(roomActor: RoomActor) {
        Log.d("AutoPubSubRoomActivity", "roomActor" + roomActor.id + " onRoomActorUpdateStream ")
    }

    override fun onLocalPublisherAudioMuted(publisher: Publisher, muted: Boolean) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherAudioMuted " + muted)
    }

    override fun onLocalPublisherUpdateStream(publisher: Publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherUpdateStream")
    }

    override fun onLocalPublisherVideoMuted(publisher: Publisher, muted: Boolean) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherVideoMuted " + muted)
    }

    override fun onRoomEnter() {
        val capturerCameraAV: Capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withAudio().withCamera())
        capturerCameraAV.start()
    }

    override fun onRoomExit() {
        Log.d("AutoPubSubRoomActivity", "onRoomExit")
    }

    override fun onRoomReconnecting() {
        Log.d("AutoPubSubRoomActivity", "reconnecting")
    }

    override fun onRoomStateChanged(state: RoomState) {
        Log.d("AutoPubSubRoomActivity", "onRoomStateChanged $state")
    }

    override fun onRoomError(reason: String) {
        Log.e("AutoPubSubRoomActivity", "onRoomError $reason")
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onRemotePublisherJoined(stream: Stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherJoined")
        val subscriber: Subscriber = room!!.create(stream)
        subscriber.addSubscribeObserver(this)
        room!!.subscribe(subscriber)
        pubSubsAdapter.add(SubscriberItem(subscriber))
    }

    override fun onRemotePublisherLeft(stream: Stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherLeft")
    }

    override fun onRemotePublisherUpdateStream(stream: Stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherUpdateStream $stream")
    }

    private fun setAdapterListeners() {
        pubSubsAdapter.withEventHook(PublisherItem.PublisherItemClickListener() as EventHook<IItem<*, *>>)
        pubSubsAdapter.withEventHook(SubscriberItem.SubscriberItemClickListener() as EventHook<IItem<*, *>>)
        pubSubsAdapter.withOnLongClickListener { v, adapter, item, position ->
            if (item is PublisherItem) {
                room?.unpublish(item.publisher)
            } else if (item is SubscriberItem) {
                room?.unsubscribe(item.subscriber)
            }
            pubSubsAdapter.remove(position)
            pubSubsAdapter.notifyAdapterItemRemoved(position)
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.information) {
            showInfo()
        } else if (id == R.id.internal_stats) {
            showInternalStats()
        }
        return super.onOptionsItemSelected(item)
    }

    fun showInfo() {
        if (room?.roomState != RoomState.CONNECTED) return
        Handler().post {
            val headers: ArrayList<String?> = object : ArrayList<String?>() {
                init {
                    add("Room Properties")
                    add("Remote Streams")
                    add("Subscribers")
                    add("Publishers")
                }
            }
            val items: ArrayList<String?> = object : ArrayList<String?>() {
                init {
                    room?.let {
                        add(App.gson!!.toJson(it.roomInfo))
                        add(App.gson!!.toJson(it.getStreams()))
                        add(App.gson!!.toJson(it.getSubscribers()))
                        add(App.gson!!.toJson(it.getPublishers()))
                    }
                }
            }
            InformationActivity.Companion.show(this@AutoPubSubRoomActivity, headers, items)
        }
    }

    private var pagerAdapter: StatsPagerAdapter? = null

    private fun showInternalStats() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.alert_internals_stats)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            InternalStatsLogger.Companion.stop()
            InternalStatsLogger.Companion.removeStatsObserver(this@AutoPubSubRoomActivity)
        }
        dialog.show()
        val mPager: ViewPager = dialog.findViewById(R.id.pager)
        pagerAdapter = StatsPagerAdapter(this)
        mPager.adapter = pagerAdapter
        InternalStatsLogger.Companion.addStatsObserver(this)
        InternalStatsLogger.Companion.start(this)
    }

    override fun onStats(id: String, stats: HashMap<InternalStatsTypes, String>) {
        if (pagerAdapter == null) return
        val page = pagerAdapter!!.addOrGetPage(id)
        page!!.updateStats(stats)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (snackbar != null) snackbar!!.dismiss()
        Capturer.Registry.destroy()
        Room.Registry.destroyAll()
        ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return imageZoomHelper != null && (imageZoomHelper!!.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev))
    }

    override fun onCapturerStarted(capturer: Capturer, stream: Stream) {
        val publisher: Publisher = room!!.create(RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
        publisher.addPublisherObserver(this@AutoPubSubRoomActivity)
                .setCapturer(capturer)
        room!!.publish(publisher)
        pubSubsAdapter.add(PublisherItem(publisher, capturer))
    }

    override fun onCapturerResumed(capturer: Capturer) {
        Log.d("AutoPubSubRoomActivity", "onCapturerResumed " + capturer.id)
    }

    override fun onCapturerError(capturer: Capturer, error: CapturerException) {
        if (capturer is CapturerScreenVideo) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onCapturerPaused(capturer: Capturer) {}
    // LOCAL PUBLISHER
    override fun onLocalPublisherJoined(publisher: Publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherJoined")
    }

    override fun onLocalPublisherRemoved(publisher: Publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherRemoved")
        pubSubsAdapter.itemAdapter.removeByIdentifier(publisher.id.hashCode().toLong())
        if (publisher.stream!!.isScreenshare) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onLocalPublisherError(publisher: Publisher, reason: String) {
        Log.e("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherError: " + reason)
        pubSubsAdapter.itemAdapter.removeByIdentifier(publisher.id.hashCode().toLong())
        if (publisher.stream!!.isScreenshare) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onLocalPublisherStateChanged(publisher: Publisher, state: PublisherState) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherStateChanged" + state)
    }

    override fun onLocalPublisherAdded(publisher: Publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.id + " onLocalPublisherAdded")
    }

    override fun onLocalPublisherConnected(publisher: Publisher, connected: Boolean) {
        Log.d("AutoPubSubRoomActivity", "onLocalPublisherConnected $connected")
    }

    override fun onLocalSubscriberConnected(subscriber: Subscriber, connected: Boolean) {
        Log.d("AutoPubSubRoomActivity", "onLocalSubscriberConnected $connected")
    }

    // LOCAL SUBSCRIBER
    override fun onLocalSubscriberJoined(subscriber: Subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberJoined")
    }

    override fun onLocalSubscriberAdded(subscriber: Subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberAdded")
    }

    override fun onLocalSubscriberError(subscriber: Subscriber, reason: String) {
        Log.e("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberError " + reason)
        pubSubsAdapter.itemAdapter.removeByIdentifier(subscriber.id.hashCode().toLong())
    }

    override fun onLocalSubscriberStateChanged(subscriber: Subscriber, state: SubscriberState) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberStateChanged " + state)
    }

    override fun onLocalSubscriberRemoved(subscriber: Subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberRemoved")
        pubSubsAdapter.itemAdapter.removeByIdentifier(subscriber.id.hashCode().toLong())
    }

    override fun onLocalSubscriberUpdateStream(subscriber: Subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberUpdateStream")
    }

    override fun onLocalSubscriberAudioMuted(subscriber: Subscriber, muted: Boolean) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberAudioMuted " + muted)
    }

    override fun onLocalSubscriberVideoMuted(subscriber: Subscriber, muted: Boolean) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberUpdateStream " + muted)
    }

    override fun onLocalSubscriberStartedScreenSharing(subscriber: Subscriber, started: Boolean) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberStartedScreenSharing " + started)
    }

    companion object {
        const val ROOM_TOKEN = "token"
        const val ROOM_AUDIO_MUTED = "audio_muted"
        fun show(activity: BaseActivity, token: String?, roomAudioMuted: Boolean) {
            val intent = Intent(activity, AutoPubSubRoomActivity::class.java)
            intent.putExtra(ROOM_TOKEN, token)
            intent.putExtra(ROOM_AUDIO_MUTED, roomAudioMuted)
            activity.startActivityForResult(intent, 0)
        }
    }
}