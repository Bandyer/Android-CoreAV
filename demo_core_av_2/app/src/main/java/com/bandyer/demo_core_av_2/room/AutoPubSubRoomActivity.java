/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.room;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.bandyer.core_av.capturer.CapturerAV;
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
import com.bandyer.demo_core_av_2.App;
import com.bandyer.demo_core_av_2.BaseActivity;
import com.bandyer.demo_core_av_2.R;
import com.bandyer.demo_core_av_2.room.adapter_items.PublisherItem;
import com.bandyer.demo_core_av_2.room.adapter_items.SubscriberItem;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnLongClickListener;
import com.viven.imagezoom.ImageZoomHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author kristiyan
 **/
public class AutoPubSubRoomActivity extends BaseActivity implements RoomObserver, SubscriberObserver, PublisherObserver {

    public static final String ROOM_TOKEN = "token";
    public static final String ROOM_AUDIO_MUTED = "audio_muted";

    @BindView(R.id.pubsubs)
    RecyclerView pubSubs;

    FastItemAdapter pubSubsAdapter = new FastItemAdapter();

    private Room room;
    private CapturerAV capturerAV;

    private Publisher publisher;

    ImageZoomHelper imageZoomHelper;

    Snackbar snackbar;

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

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3, LinearLayoutManager.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // if is the first item then it weights 2 else 1
                return position == 0 ? 2 : 1;
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

        room = new Room(new RoomToken(token));
        room.addRoomObserver(this);
        room.muteAllAudio(getIntent().getBooleanExtra(ROOM_AUDIO_MUTED, false));
        room.join();
    }

    @SuppressWarnings("unchecked")
    private void setAdapterListeners() {
        pubSubsAdapter.withEventHook(new PublisherItem.PublisherItemClickListener());
        pubSubsAdapter.withEventHook(new SubscriberItem.SubscriberItemClickListener());
        pubSubsAdapter.withOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(@NonNull View v, @NonNull IAdapter adapter, @NonNull IItem item, int position) {
                if (item instanceof PublisherItem) {
                    room.unpublish(publisher);
                    publisher = null;
                } else if (item instanceof SubscriberItem) {
                    Subscriber subscriber = ((SubscriberItem) item).getSubscriber();
                    room.unsubscribe(subscriber);
                }
                pubSubsAdapter.remove(position);
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
    public void onLocalPublisherJoined(@NonNull Publisher publisher) {
        Log.e("Publisher", "onLocalPublisherJoined");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRemotePublisherJoined(@NonNull final Stream stream) {
        Log.e("Publisher", "onRemotePublisherJoined");
        final Subscriber subscriber = new Subscriber(stream)
                .addSubscribeObserver(this);
        room.subscribe(subscriber);
        SubscriberItem item = new SubscriberItem(subscriber);
        pubSubsAdapter.add(item);
    }

    @Override
    public void onRemotePublisherLeft(@NonNull Stream stream) {
        Log.e("Publisher", "onRemotePublisherLeft");
        Subscriber subscriber = room.getSubscriber(stream);
        if (subscriber == null)
            return;
        room.unsubscribe(subscriber);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRoomEnter() {
        capturerAV = new CapturerAV(this);
        capturerAV.start();
        publisher = new Publisher(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(AutoPubSubRoomActivity.this)
                .setCapturer(capturerAV);
        room.publish(publisher);

        PublisherItem item = new PublisherItem(publisher, capturerAV);
        pubSubsAdapter.add(0, item);
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
                    if (publisher != null)
                        add("Publisher");
                }};

                ArrayList<String> items = new ArrayList<String>() {{
                    add(App.gson.toJson(room.getRoomInfo()));
                    add(App.gson.toJson(room.getStreams()));
                    add(App.gson.toJson(room.getSubscribers()));
                    if (publisher != null)
                        add(App.gson.toJson(room.getPublisher()));
                }};
                InformationActivity.show(AutoPubSubRoomActivity.this, headers, items);
            }
        });
    }

    @Override
    public void onRoomExit() {
        Log.d("Room", "exit");
    }

    @Override
    public void onRoomReconnecting() {
        Log.d("RoomActivity", "reconnecting");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snackbar != null)
            snackbar.dismiss();
        if (room != null)
            room.leave();
    }

    @Override
    public void onRoomStateChanged(@NonNull RoomState state) {
        Log.d("Room", "onRoomStateChanged " + state);
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("Room", "onRoomError " + reason);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onLocalSubscriberAdded(@NonNull Subscriber subscriber) {
        Log.d("Subscriber", "onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("Subscriber", reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberStateChanged(@NonNull Subscriber subscriber, @NonNull SubscriberState subscriberState) {
        Log.d("Subscriber", "changed status to " + subscriberState.name());
    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.d("Publisher", "onLocalPublisherAdded");
    }

    @Override
    public void onLocalPublisherRemoved(@NonNull Publisher publisher) {
        room.unpublish(publisher);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("Publisher", reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public void onLocalPublisherStateChanged(@NonNull Publisher publisher, @NonNull PublisherState publisherState) {
        Log.d("Publisher", "changed status to " + publisherState.name());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper != null && (imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev));
    }
    
}
