/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.room;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.material.snackbar.Snackbar;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.bandyer.android_audiosession.AudioOutputDeviceType;
import com.bandyer.android_audiosession.AudioSession;
import com.bandyer.android_audiosession.AudioSessionOptions;
import com.bandyer.android_audiosession.audiosession.AudioSessionListener;
import com.bandyer.android_common.proximity_listener.ProximitySensorListener;
import com.bandyer.core_av.Stream;
import com.bandyer.core_av.capturer.Capturer;
import com.bandyer.core_av.capturer.CapturerException;
import com.bandyer.core_av.capturer.CapturerObserver;
import com.bandyer.core_av.capturer.CapturerOptions;
import com.bandyer.core_av.capturer.mix.CapturerAudioVideo;
import com.bandyer.core_av.capturer.video.screen.CapturerScreenVideo;
import com.bandyer.core_av.publisher.Publisher;
import com.bandyer.core_av.publisher.PublisherObserver;
import com.bandyer.core_av.publisher.PublisherState;
import com.bandyer.core_av.room.Room;
import com.bandyer.core_av.room.RoomObserver;
import com.bandyer.core_av.room.RoomState;
import com.bandyer.core_av.room.RoomToken;
import com.bandyer.core_av.room.RoomUser;
import com.bandyer.core_av.subscriber.Subscriber;
import com.bandyer.core_av.subscriber.SubscriberObserver;
import com.bandyer.core_av.subscriber.SubscriberState;
import com.bandyer.core_av.utils.logging.InternalStatsLogger;
import com.bandyer.core_av.utils.logging.InternalStatsTypes;
import com.bandyer.demo_core_av.App;
import com.bandyer.demo_core_av.BaseActivity;
import com.bandyer.demo_core_av.R;
import com.bandyer.demo_core_av.StatsPage;
import com.bandyer.demo_core_av.StatsPagerAdapter;
import com.bandyer.demo_core_av.room.adapter_items.PublisherItem;
import com.bandyer.demo_core_av.room.adapter_items.SubscriberItem;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnLongClickListener;
import com.viven.imagezoom.ImageZoomHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author kristiyan
 **/
public class AutoPubSubRoomActivity extends BaseActivity implements RoomObserver, SubscriberObserver, PublisherObserver, InternalStatsLogger, CapturerObserver {

    public static final String ROOM_TOKEN = "token";
    public static final String ROOM_AUDIO_MUTED = "audio_muted";

    @BindView(R.id.pubsubs)
    RecyclerView pubSubs;

    private FastItemAdapter pubSubsAdapter = new FastItemAdapter();

    private Room room;

    private ImageZoomHelper imageZoomHelper;

    private Snackbar snackbar;

    public static void show(BaseActivity activity, String token, boolean roomAudioMuted) {
        Intent intent = new Intent(activity, AutoPubSubRoomActivity.class);
        intent.putExtra(ROOM_TOKEN, token);
        intent.putExtra(ROOM_AUDIO_MUTED, roomAudioMuted);
        activity.startActivityForResult(intent, 0);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_pubsub_room);

        String token = getIntent().getStringExtra(ROOM_TOKEN);

        imageZoomHelper = new ImageZoomHelper(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4, LinearLayoutManager.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // if is the first item then it weights 4 else 2
                return position == 0 ? 4 : 2;
            }
        });
        pubSubs.setLayoutManager(layoutManager);
        pubSubs.setAdapter(pubSubsAdapter);

        setAdapterListeners();

        AudioSession.getInstance().startWithOptions(
                this,
                new AudioSessionOptions.Builder()
                        .withDefaultSpeakerPhoneOutputHardWareDevice()
                        .build(),
                new AudioSessionListener() {
                    @Override
                    public void onOutputDeviceConnected(@NonNull AudioOutputDeviceType oldAudioOutputDeviceType, @NonNull AudioOutputDeviceType connectedAudioOutputDevice, @NonNull List<? extends AudioOutputDeviceType> availableOutputs) {
                        Log.d("AudioSession", "changed from old: " + oldAudioOutputDeviceType.name() + " to connected: " + connectedAudioOutputDevice.name());
                        if (snackbar != null)
                            snackbar.dismiss();
                        snackbar = Snackbar.make(pubSubs, connectedAudioOutputDevice.name(), Snackbar.LENGTH_SHORT);
                        snackbar.show();
                    }

                    @Override
                    public void onOutputDeviceAttached(@NonNull AudioOutputDeviceType currentAudioOutputDevice, @NonNull AudioOutputDeviceType attachedAudioOutputDevice, @NonNull List<? extends AudioOutputDeviceType> availableOutputs) {
                        Log.d("AudioSession", "current: " + currentAudioOutputDevice.name() + " attached audioDevice: " + attachedAudioOutputDevice.name());
                    }

                    @Override
                    public void onOutputDeviceDetached(@NonNull AudioOutputDeviceType currentAudioOutputDevice, @NonNull AudioOutputDeviceType detachedAudioOutputDevice, @NonNull List<? extends AudioOutputDeviceType> availableOutputs) {
                        Log.d("AudioSession", "current: " + currentAudioOutputDevice.name() + " detached audioDevice: " + detachedAudioOutputDevice.name());
                    }
                }, new ProximitySensorListener() {
                    @Override
                    public void onProximitySensorChanged(boolean isNear) {
                        Log.d("ProximitySensor", "proximity triggered: " + isNear);
                    }
                });

        Capturer.Registry.addCapturerObserver(this);

        room = Room.Registry.get(new RoomToken(token));
        room.addRoomObserver(this);
        room.muteAllSubscribersAudio(getIntent().getBooleanExtra(ROOM_AUDIO_MUTED, false));
        room.join();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Capturer capturer : Capturer.Registry.getCapturers()) capturer.resume();
    }


    @Override
    protected void onStop() {
        super.onStop();
        for (Capturer capturer : Capturer.Registry.getCapturers()) {
            if (capturer instanceof CapturerScreenVideo) return;

            if (capturer instanceof CapturerAudioVideo)
                ((CapturerAudioVideo) capturer).pause(true, false);
            else
                capturer.pause();
        }
    }

    @Override
    public void onRoomEnter() {
        Capturer capturerCameraAV = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio().withCamera());
        capturerCameraAV.start();
    }

    @Override
    public void onRoomExit() {
        Log.d("AutoPubSubRoomActivity", "onRoomExit");
    }

    @Override
    public void onRoomReconnecting() {
        Log.d("AutoPubSubRoomActivity", "reconnecting");
    }

    @Override
    public void onRoomStateChanged(@NonNull RoomState state) {
        Log.d("AutoPubSubRoomActivity", "onRoomStateChanged " + state);
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("AutoPubSubRoomActivity", "onRoomError " + reason);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRemotePublisherJoined(@NonNull final Stream stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherJoined");
        final Subscriber subscriber = room.create(stream);
        subscriber.addSubscribeObserver(this);
        room.subscribe(subscriber);
        SubscriberItem item = new SubscriberItem(subscriber);
        pubSubsAdapter.add(item);
    }

    @Override
    public void onRemotePublisherLeft(@NonNull Stream stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherLeft");
    }

    @Override
    public void onRemotePublisherUpdateStream(@NotNull Stream stream) {
        Log.d("AutoPubSubRoomActivity", "onRemotePublisherUpdateStream " + stream);
    }

    @SuppressWarnings("unchecked")
    private void setAdapterListeners() {
        pubSubsAdapter.withEventHook(new PublisherItem.PublisherItemClickListener());
        pubSubsAdapter.withEventHook(new SubscriberItem.SubscriberItemClickListener());
        pubSubsAdapter.withOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(@NonNull View v, @NonNull IAdapter adapter, @NonNull IItem item, int position) {
                if (item instanceof PublisherItem) {
                    Publisher publisher = ((PublisherItem) item).getPublisher();
                    room.unpublish(publisher);
                } else if (item instanceof SubscriberItem) {
                    Subscriber subscriber = ((SubscriberItem) item).getSubscriber();
                    room.unsubscribe(subscriber);
                }
                pubSubsAdapter.remove(position);
                pubSubsAdapter.notifyAdapterItemRemoved(position);
                return true;
            }
        });
    }

    @OnClick(R.id.close_room)
    void closeRoom() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.information) {
            showInfo();
        } else if (id == R.id.internal_stats) {
            showInternalStats();
        }
        return super.onOptionsItemSelected(item);
    }


    public void showInfo() {
        if (room == null || room.getRoomState() != RoomState.CONNECTED)
            return;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> headers = new ArrayList<String>() {{
                    add("Room Properties");
                    add("Remote Streams");
                    add("Subscribers");
                    add("Publishers");
                }};

                ArrayList<String> items = new ArrayList<String>() {{
                    add(App.gson.toJson(room.getRoomInfo()));
                    add(App.gson.toJson(room.getStreams()));
                    add(App.gson.toJson(room.getSubscribers()));
                    add(App.gson.toJson(room.getPublishers()));
                }};
                InformationActivity.show(AutoPubSubRoomActivity.this, headers, items);
            }
        });
    }


    StatsPagerAdapter pagerAdapter;

    public void showInternalStats() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.alert_internals_stats);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(it -> {
            InternalStatsLogger.Companion.stop();
            InternalStatsLogger.Companion.removeStatsObserver(AutoPubSubRoomActivity.this);
        });

        dialog.show();

        ViewPager mPager = dialog.findViewById(R.id.pager);
        pagerAdapter = new StatsPagerAdapter(this);

        mPager.setAdapter(pagerAdapter);

        InternalStatsLogger.Companion.addStatsObserver(this);
        InternalStatsLogger.Companion.start(this);
    }

    @Override
    public void onStats(@NonNull String id, @NonNull final HashMap<InternalStatsTypes, String> stats) {
        if (pagerAdapter == null) return;
        StatsPage page = pagerAdapter.addOrGetPage(id);
        page.updateStats(stats);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snackbar != null) snackbar.dismiss();
        Capturer.Registry.destroy();
        Room.Registry.destroyAll();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper != null && (imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev));
    }

    @OnClick(R.id.screenShare)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void onScreenShare() {
        if (room == null || room.getRoomState() != RoomState.CONNECTED) return;
        Capturer capturerScreenVideo = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio().withScreenShare());
        capturerScreenVideo.start();
    }

    @Override
    public void onCapturerStarted(@NotNull Capturer capturer, @NotNull Stream stream) {
        Publisher publisher = room.create(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"));
        publisher.addPublisherObserver(AutoPubSubRoomActivity.this)
                .setCapturer(capturer);

        room.publish(publisher);

        PublisherItem item = new PublisherItem(publisher, capturer);
        pubSubsAdapter.add(item);
    }

    @Override
    public void onCapturerResumed(@NotNull Capturer capturer) {
        Log.d("AutoPubSubRoomActivity", "onCapturerResumed " + capturer.getId());
    }

    @Override
    public void onCapturerError(@NotNull Capturer capturer, @NotNull CapturerException error) {

    }

    @Override
    public void onCapturerPaused(@NotNull Capturer capturer) {

    }

    // LOCAL PUBLISHER

    @Override
    public void onLocalPublisherJoined(@NotNull Publisher publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.getId() + " onLocalPublisherJoined");
    }

    @Override
    public void onLocalPublisherRemoved(@NonNull Publisher publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.getId() + " onLocalPublisherRemoved");
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("AutoPubSubRoomActivity", "publisher" + publisher.getId() + " onLocalPublisherError: " + reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public void onLocalPublisherStateChanged(@NonNull Publisher publisher, @NonNull PublisherState publisherState) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.getId() + " onLocalPublisherStateChanged" + publisherState.name());
    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.d("AutoPubSubRoomActivity", "publisher" + publisher.getId() + " onLocalPublisherAdded");
    }

    // LOCAL SUBSCRIBER

    @Override
    public void onLocalSubscriberJoined(@NotNull Subscriber subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberJoined");
    }

    @Override
    public void onLocalSubscriberAdded(@NonNull Subscriber subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberError " + reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberStateChanged(@NonNull Subscriber subscriber, @NonNull SubscriberState subscriberState) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberStateChanged " + subscriberState);
    }

    @Override
    public void onLocalSubscriberRemoved(@NotNull Subscriber subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberRemoved");
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberUpdateStream(@NotNull Subscriber subscriber) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberUpdateStream");
    }

    @Override
    public void onLocalSubscriberAudioMuted(@NotNull Subscriber subscriber, boolean muted) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberAudioMuted " + muted);
    }

    @Override
    public void onLocalSubscriberVideoMuted(@NotNull Subscriber subscriber, boolean muted) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberUpdateStream " + muted);
    }

    @Override
    public void onLocalSubscriberStartedScreenSharing(@NotNull Subscriber subscriber, boolean started) {
        Log.d("AutoPubSubRoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberStartedScreenSharing " + started);
    }
}
