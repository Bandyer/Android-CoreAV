/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.room;


import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

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
import com.bandyer.core_av.room.RoomActor;
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
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPicker;
import com.bandyer.demo_core_av.room.adapter_items.PublisherItem;
import com.bandyer.demo_core_av.room.adapter_items.StreamItem;
import com.bandyer.demo_core_av.room.adapter_items.SubscriberItem;
import com.bandyer.demo_core_av.room.utils.ScreenSharingUtils;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
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
public class RoomActivity extends BaseActivity implements RoomObserver, SubscriberObserver, PublisherObserver, InternalStatsLogger, CapturerObserver {

    public static final String ROOM_TOKEN = "token";
    public static final String ROOM_AUDIO_MUTED = "audio_muted";

    private Room room;

    @BindView(R.id.streams)
    RecyclerView streams;

    @BindView(R.id.pubsubs)
    RecyclerView pubSubs;

    private FastItemAdapter<StreamItem> streamAdapter = new FastItemAdapter<>();
    private FastItemAdapter pubSubsAdapter = new FastItemAdapter<>();

    private ImageZoomHelper imageZoomHelper;

    private Snackbar snackbar;

    public static void show(BaseActivity activity, String token, boolean roomAudioMuted) {
        Intent intent = new Intent(activity, RoomActivity.class);
        intent.putExtra(ROOM_TOKEN, token);
        intent.putExtra(ROOM_AUDIO_MUTED, roomAudioMuted);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        imageZoomHelper = new ImageZoomHelper(this);

        String token = getIntent().getStringExtra(ROOM_TOKEN);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // if is the first item then it weights 4 else 2
                return position == 0 ? 4 : 2;
            }
        });

        pubSubs.setLayoutManager(layoutManager);
        pubSubs.setAdapter(pubSubsAdapter);

        streams.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        streams.setAdapter(streamAdapter);

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
        for (Capturer capturer : Capturer.Registry.getCapturers())
            capturer.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Capturer capturer : Capturer.Registry.getCapturers()) {
            if (capturer instanceof CapturerScreenVideo) continue;
            if (capturer instanceof CapturerAudioVideo)
                ((CapturerAudioVideo) capturer).pause(true, false);
            else capturer.pause();
        }
    }

    @Override
    public void onRoomEnter() {
        Log.d("RoomActivity", "onRoomEnter");
    }

    @Override
    public void onRoomExit() {
        Log.d("RoomActivity", "exit");
    }

    @Override
    public void onRoomStateChanged(@NonNull RoomState state) {
        Log.d("RoomActivity", "onRoomStateChanged " + state);
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("RoomActivity", "onRoomError " + reason);
    }

    @Override
    public void onRoomReconnecting() {
        Log.d("RoomActivity", "reconnecting...");
    }

    @Override
    public void onRemotePublisherJoined(@NonNull final Stream stream) {
        Log.d("RoomActivity", "onRemotePublisherJoined");
        StreamItem streamItem = new StreamItem(stream);
        streamAdapter.add(0, streamItem);
    }

    @Override
    public void onRemotePublisherLeft(@NonNull Stream stream) {
        Log.d("RoomActivity", "onRemotePublisherLeft");
        streamAdapter.getItemAdapter().removeByIdentifier(stream.getStreamId().hashCode());
    }

    @Override
    public void onRemotePublisherUpdateStream(@NotNull Stream stream) {
        Log.d("RoomActivity", "onRemotePublisherUpdateStream " + stream);
    }

    @Override
    public void onLocalPublisherAudioMuted(@NotNull Publisher publisher, boolean muted) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherAudioMuted " + muted);
    }

    @Override
    public void onLocalPublisherUpdateStream(@NotNull Publisher publisher) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherUpdateStream");
    }

    @Override
    public void onLocalPublisherVideoMuted(@NotNull Publisher publisher, boolean muted) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherVideoMuted " + muted);
    }

    @Override
    public void onRoomActorUpdateStream(@NotNull RoomActor roomActor) {
        Log.d("RoomActivity", "roomActor" + roomActor.getId() + " onRoomActorUpdateStream " );
    }

    @SuppressWarnings("unchecked")
    private void setAdapterListeners() {
        pubSubsAdapter.withEventHook(new PublisherItem.PublisherItemClickListener());
        pubSubsAdapter.withEventHook(new SubscriberItem.SubscriberItemClickListener());
        pubSubsAdapter.withOnLongClickListener((v, adapter, item, position) -> {
            if (item instanceof PublisherItem) {
                Publisher publisher = ((PublisherItem) item).getPublisher();
                room.unpublish(publisher);
            } else if (item instanceof SubscriberItem) {
                Subscriber subscriber = ((SubscriberItem) item).getSubscriber();
                room.unsubscribe(subscriber);
                onRemotePublisherJoined(subscriber.getStream());
            }
            pubSubsAdapter.remove(position);
            return false;
        });

        streamAdapter.withOnClickListener((v, adapter, item, position) -> {
            addNewSubscriber(item.getStream());
            streamAdapter.remove(position);
            return false;
        });
    }

    @OnClick(R.id.add_publish)
    void chooseTypeOfCall() {
        final BottomListPicker<String> joinType = new BottomListPicker<>(this, getString(R.string.room_join_title));
        List<String> types = new ArrayList<String>() {{
            add("Audio Call");
            add("VideoOnly Call");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                add("ScreenShare");
                add("Audio&ScreenShare");
            }
            add("Audio&Video Call");
        }};

        joinType.setItems(types, -1, (adapterPosition, item) -> {
            onAddPublish(item);
            joinType.dismiss();
        }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snackbar != null) snackbar.dismiss();
        Capturer.Registry.destroy();
        Room.Registry.destroyAll();
        ScreenSharingUtils.hideScreenShareNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.information)
            showInfo();
        else if (id == R.id.internal_stats) {
            showInternalStats();
        }

        return super.onOptionsItemSelected(item);
    }

    public void showInfo() {
        if (room == null || room.getRoomState() != RoomState.CONNECTED)
            return;

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

        InformationActivity.show(this, headers, items);
    }

    @SuppressWarnings("unchecked")
    private void addNewSubscriber(final Stream stream) {
        final Subscriber subscriber = room.create(stream);
        subscriber.addSubscribeObserver(RoomActivity.this);
        room.subscribe(subscriber);
        SubscriberItem subscriberItem = new SubscriberItem(subscriber);
        pubSubsAdapter.add(0, subscriberItem);
    }


    StatsPagerAdapter pagerAdapter;

    public void showInternalStats() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.alert_internals_stats);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(it -> {
            InternalStatsLogger.Companion.stop();
            InternalStatsLogger.Companion.removeStatsObserver(RoomActivity.this);
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper != null && (imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev));
    }

    void onAddPublish(String capturerType) {
        if (room == null || room.getRoomState() != RoomState.CONNECTED) return;

        Capturer capturer = null;

        switch (capturerType) {
            case "Audio Call":
                capturer = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio());
                break;
            case "VideoOnly Call":
                capturer = Capturer.Registry.get(this, new CapturerOptions.Builder().withCamera());
                break;
            case "ScreenShare":
                capturer = Capturer.Registry.get(this, new CapturerOptions.Builder().withScreenShare());
                break;
            case "Audio&ScreenShare":
                capturer = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio().withScreenShare());
                break;
            case "Audio&Video Call":
                capturer = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio().withCamera());
                break;
        }

        // on Android Q before launching a screenShare a notification MUST be shown as foreground service with mediaProjection
        if (capturer instanceof CapturerScreenVideo)
            ScreenSharingUtils.showScreenShareNotification(this);

        Publisher publisher = room.create(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"));
        publisher.addPublisherObserver(RoomActivity.this)
                .setCapturer(capturer);
        room.publish(publisher);
        PublisherItem item = new PublisherItem(publisher, capturer);
        pubSubsAdapter.add(item);
        capturer.start();
    }

    @Override
    public void onCapturerStarted(@NotNull Capturer capturer, @NotNull Stream stream) {
        Log.d("RoomActivity", "onCapturerStarted");
    }

    @Override
    public void onCapturerResumed(@NotNull Capturer capturer) {
        Log.d("RoomActivity", "onCapturerResumed " + capturer.getId());
    }

    @Override
    public void onCapturerError(@NotNull Capturer capturer, @NotNull CapturerException reason) {
        Log.e("RoomActivity", "onCapturerError " + reason.getLocalizedMessage());
        if (capturer instanceof CapturerScreenVideo)
            ScreenSharingUtils.hideScreenShareNotification();
    }

    @Override
    public void onCapturerPaused(@NotNull Capturer capturer) {
        Log.d("RoomActivity", "onCapturerPaused");
    }

    // LOCAL PUBLISHER

    @Override
    public void onLocalPublisherJoined(@NotNull Publisher publisher) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherJoined");
    }

    @Override
    public void onLocalPublisherRemoved(@NonNull Publisher publisher) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherRemoved");
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
        if (publisher.getStream().isScreenshare()) ScreenSharingUtils.hideScreenShareNotification();

    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherError: " + reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
        if (publisher.getStream().isScreenshare()) ScreenSharingUtils.hideScreenShareNotification();
    }

    @Override
    public void onLocalPublisherStateChanged(@NonNull Publisher publisher, @NonNull PublisherState publisherState) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherStateChanged" + publisherState.name());
    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.d("RoomActivity", "publisher" + publisher.getId() + " onLocalPublisherAdded");
    }

    // LOCAL SUBSCRIBER

    @Override
    public void onLocalSubscriberJoined(@NotNull Subscriber subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberJoined");
    }

    @Override
    public void onLocalSubscriberAdded(@NonNull Subscriber subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberError " + reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberStateChanged(@NonNull Subscriber subscriber, @NonNull SubscriberState subscriberState) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberStateChanged " + subscriberState);
    }

    @Override
    public void onLocalSubscriberRemoved(@NotNull Subscriber subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberRemoved");
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberUpdateStream(@NotNull Subscriber subscriber) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberUpdateStream");
    }

    @Override
    public void onLocalSubscriberAudioMuted(@NotNull Subscriber subscriber, boolean muted) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberAudioMuted " + muted);
    }

    @Override
    public void onLocalSubscriberVideoMuted(@NotNull Subscriber subscriber, boolean muted) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberVideoMuted " + muted);
    }

    @Override
    public void onLocalSubscriberStartedScreenSharing(@NotNull Subscriber subscriber, boolean started) {
        Log.d("RoomActivity", "subscriber" + subscriber.getId() + " onLocalSubscriberStartedScreenSharing " + started);
    }
}
