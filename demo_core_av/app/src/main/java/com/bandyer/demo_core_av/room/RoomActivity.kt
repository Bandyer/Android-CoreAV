/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
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
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPicker
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPickerItem
import com.bandyer.demo_core_av.room.adapter_items.PublisherItem
import com.bandyer.demo_core_av.room.adapter_items.StreamItem
import com.bandyer.demo_core_av.room.adapter_items.SubscriberItem
import com.bandyer.demo_core_av.room.utils.ScreenSharingUtils
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.viven.imagezoom.ImageZoomHelper
import kotlinx.android.synthetic.main.activity_room.*
import java.util.*

/**
 * @author kristiyan
 */
class RoomActivity : BaseActivity(), RoomObserver, SubscriberObserver, PublisherObserver, InternalStatsLogger, CapturerObserver {

    private var room: Room? = null
    private val streamAdapter = FastItemAdapter<StreamItem>()
    private val pubSubsAdapter = FastItemAdapter<IItem<*, *>>()
    private var pagerAdapter: StatsPagerAdapter? = null
    private var imageZoomHelper: ImageZoomHelper? = null
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
        imageZoomHelper = ImageZoomHelper(this)
        val token = intent.getStringExtra(ROOM_TOKEN)!!
        val layoutManager = GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int { // if is the first item then it weights 4 else 2
                return if (position == 0) 4 else 2
            }
        }
        pubsubs!!.layoutManager = layoutManager
        pubsubs!!.adapter = pubSubsAdapter
        streams!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        streams!!.adapter = streamAdapter
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
        room = Room.Registry.get(RoomToken(token)).apply {
            addRoomObserver(this@RoomActivity)
            muteAllSubscribersAudio(intent.getBooleanExtra(ROOM_AUDIO_MUTED, false))
            join()
        }

        add_publish.setOnClickListener { addPublisher() }
    }

    override fun onResume() {
        super.onResume()
        Capturer.Registry.capturers.forEach { it.resume() }
    }

    override fun onPause() {
        super.onPause()
        Capturer.Registry.capturers.forEach { capturer ->
            if (capturer is CapturerScreenVideo) return@forEach
            if (capturer is CapturerAudioVideo) capturer.pause(video = true, audio = false) else capturer.pause()
        }
    }

    override fun onRoomEnter() {
        Log.d("RoomActivity", "onRoomEnter")
    }

    override fun onRoomExit() {
        Log.d("RoomActivity", "exit")
    }

    override fun onRoomStateChanged(state: RoomState) {
        Log.d("RoomActivity", "onRoomStateChanged $state")
    }

    override fun onRoomError(reason: String) {
        Log.e("RoomActivity", "onRoomError $reason")
    }

    override fun onRoomReconnecting() {
        Log.d("RoomActivity", "reconnecting...")
    }

    override fun onRemotePublisherJoined(stream: Stream) {
        Log.d("RoomActivity", "onRemotePublisherJoined")
        val streamItem = StreamItem(stream)
        streamAdapter.add(0, streamItem)
    }

    override fun onRemotePublisherLeft(stream: Stream) {
        Log.d("RoomActivity", "onRemotePublisherLeft")
        streamAdapter.itemAdapter.removeByIdentifier(stream.streamId.hashCode().toLong())
    }

    override fun onLocalPublisherConnected(publisher: Publisher, connected: Boolean) {
        Log.d("RoomActivity", "onLocalPublisherConnected $connected")
    }

    override fun onLocalSubscriberConnected(subscriber: Subscriber, connected: Boolean) {
        Log.d("RoomActivity", "onLocalSubscriberConnected $connected")
    }

    override fun onRemotePublisherUpdateStream(stream: Stream) {
        Log.d("RoomActivity", "onRemotePublisherUpdateStream $stream")
    }

    override fun onLocalPublisherAudioMuted(publisher: Publisher, muted: Boolean) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherAudioMuted " + muted)
    }

    override fun onLocalPublisherUpdateStream(publisher: Publisher) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherUpdateStream")
    }

    override fun onLocalPublisherVideoMuted(publisher: Publisher, muted: Boolean) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherVideoMuted " + muted)
    }

    override fun onRoomActorUpdateStream(roomActor: RoomActor) {
        Log.d("RoomActivity", "roomActor" + roomActor.id + " onRoomActorUpdateStream ")
    }

    private fun setAdapterListeners() {
        pubSubsAdapter.withEventHook(PublisherItem.PublisherItemClickListener() as EventHook<IItem<*, *>>)
        pubSubsAdapter.withEventHook(SubscriberItem.SubscriberItemClickListener() as EventHook<IItem<*, *>>)
        pubSubsAdapter.withOnLongClickListener { v: View?, adapter: IAdapter<*>?, item: IItem<*, *>?, position: Int ->
            if (item is PublisherItem) {
                room?.unpublish(item.publisher)
            } else if (item is SubscriberItem) {
                room?.unsubscribe(item.subscriber)
                onRemotePublisherJoined(item.subscriber.stream)
            }
            pubSubsAdapter.remove(position)
            false
        }

        streamAdapter.withOnClickListener { v: View?, adapter: IAdapter<StreamItem>?, item: StreamItem, position: Int ->
            addNewSubscriber(item.stream)
            streamAdapter.remove(position)
            false
        }
    }

    private fun addPublisher() {
        val joinType = BottomListPicker<String>(this, getString(R.string.room_join_title))
        val types: List<String> = object : ArrayList<String>() {
            init {
                add("Audio Call")
                add("VideoOnly Call")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    add("ScreenShare")
                    add("Audio&ScreenShare")
                }
                add("Audio&Video Call")
            }
        }
        joinType.setItems(types, -1, object : BottomListPickerItem.Delegate<String> {
            override fun onClicked(adapterPosition: Int, item: String?) {
                onAddPublish(item)
                joinType.dismiss()
            }
        }).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (snackbar != null) snackbar!!.dismiss()
        Capturer.Registry.destroy()
        Room.Registry.destroyAll()
        ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.information) showInfo() else if (id == R.id.internal_stats) {
            showInternalStats()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showInfo() {
        if (room?.roomState !== RoomState.CONNECTED) return
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
        InformationActivity.Companion.show(this, headers, items)
    }

    private fun addNewSubscriber(stream: Stream) {
        val subscriber: Subscriber = room!!.create(stream)
        subscriber.addSubscribeObserver(this@RoomActivity)
        room!!.subscribe(subscriber)
        pubSubsAdapter.add(0, SubscriberItem(subscriber))
    }


    fun showInternalStats() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.alert_internals_stats)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            InternalStatsLogger.Companion.stop()
            InternalStatsLogger.Companion.removeStatsObserver(this@RoomActivity)
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return imageZoomHelper != null && (imageZoomHelper!!.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev))
    }

    fun onAddPublish(capturerType: String?) {
        if (room?.roomState != RoomState.CONNECTED) return
        var capturer: Capturer? = null
        when (capturerType) {
            "Audio Call" -> capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withAudio())
            "VideoOnly Call" -> capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withCamera())
            "ScreenShare" -> capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withScreenShare())
            "Audio&ScreenShare" -> capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withAudio().withScreenShare())
            "Audio&Video Call" -> capturer = Capturer.Registry.get(this, CapturerOptions.Builder().withAudio().withCamera())
        }
        // on Android Q before launching a screenShare a notification MUST be shown as foreground service with mediaProjection
        if (capturer is CapturerScreenVideo) ScreenSharingUtils.showScreenShareNotification(this)
        val publisher: Publisher = room!!.create(RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
        publisher.addPublisherObserver(this@RoomActivity)
                .setCapturer(capturer!!)
        room!!.publish(publisher)
        pubSubsAdapter.add(PublisherItem(publisher, capturer))
        capturer.start()
    }

    override fun onCapturerStarted(capturer: Capturer, stream: Stream) {
        Log.d("RoomActivity", "onCapturerStarted")
    }

    override fun onCapturerResumed(capturer: Capturer) {
        Log.d("RoomActivity", "onCapturerResumed " + capturer.id)
    }

    override fun onCapturerError(capturer: Capturer, error: CapturerException) {
        Log.e("RoomActivity", "onCapturerError " + error.localizedMessage)
        if (capturer is CapturerScreenVideo) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onCapturerPaused(capturer: Capturer) {
        Log.d("RoomActivity", "onCapturerPaused")
    }

    // LOCAL PUBLISHER
    override fun onLocalPublisherJoined(publisher: Publisher) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherJoined")
    }

    override fun onLocalPublisherRemoved(publisher: Publisher) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherRemoved")
        pubSubsAdapter.itemAdapter.removeByIdentifier(publisher.id.hashCode().toLong())
        if (publisher.stream!!.isScreenshare) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onLocalPublisherError(publisher: Publisher, reason: String) {
        Log.e("RoomActivity", "publisher" + publisher.id + " onLocalPublisherError: " + reason)
        pubSubsAdapter.itemAdapter.removeByIdentifier(publisher.id.hashCode().toLong())
        if (publisher.stream!!.isScreenshare) ScreenSharingUtils.hideScreenShareNotification()
    }

    override fun onLocalPublisherStateChanged(publisher: Publisher, state: PublisherState) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherStateChanged" + state)
    }

    override fun onLocalPublisherAdded(publisher: Publisher) {
        Log.d("RoomActivity", "publisher" + publisher.id + " onLocalPublisherAdded")
    }

    // LOCAL SUBSCRIBER
    override fun onLocalSubscriberJoined(subscriber: Subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberJoined")
    }

    override fun onLocalSubscriberAdded(subscriber: Subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberAdded")
    }

    override fun onLocalSubscriberError(subscriber: Subscriber, reason: String) {
        Log.e("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberError " + reason)
        pubSubsAdapter.itemAdapter.removeByIdentifier(subscriber.id.hashCode().toLong())
    }

    override fun onLocalSubscriberStateChanged(subscriber: Subscriber, state: SubscriberState) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberStateChanged " + state)
    }

    override fun onLocalSubscriberRemoved(subscriber: Subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberRemoved")
        pubSubsAdapter.itemAdapter.removeByIdentifier(subscriber.id.hashCode().toLong())
    }

    override fun onLocalSubscriberUpdateStream(subscriber: Subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberUpdateStream")
    }

    override fun onLocalSubscriberAudioMuted(subscriber: Subscriber, muted: Boolean) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberAudioMuted " + muted)
    }

    override fun onLocalSubscriberVideoMuted(subscriber: Subscriber, muted: Boolean) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberVideoMuted " + muted)
    }

    override fun onLocalSubscriberStartedScreenSharing(subscriber: Subscriber, started: Boolean) {
        Log.d("RoomActivity", "subscriber" + subscriber.id + " onLocalSubscriberStartedScreenSharing " + started)
    }

    companion object {
        const val ROOM_TOKEN = "token"
        const val ROOM_AUDIO_MUTED = "audio_muted"
        fun show(activity: BaseActivity, token: String?, roomAudioMuted: Boolean) {
            val intent = Intent(activity, RoomActivity::class.java)
            intent.putExtra(ROOM_TOKEN, token)
            intent.putExtra(ROOM_AUDIO_MUTED, roomAudioMuted)
            activity.startActivity(intent)
        }
    }
}