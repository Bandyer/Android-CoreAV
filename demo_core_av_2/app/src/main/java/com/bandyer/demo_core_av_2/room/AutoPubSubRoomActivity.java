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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.bandyer.core_av.Stream;
import com.bandyer.core_av.capturer.CapturerAV;
import com.bandyer.core_av.peerconnection.bandwidthThrottling.FixedBandwidthThrottlingStrategy;
import com.bandyer.core_av.publisher.Publisher;
import com.bandyer.core_av.publisher.PublisherObserver;
import com.bandyer.core_av.room.Room;
import com.bandyer.core_av.room.RoomObserver;
import com.bandyer.core_av.room.RoomState;
import com.bandyer.core_av.room.RoomToken;
import com.bandyer.core_av.room.RoomUser;
import com.bandyer.core_av.subscriber.Subscriber;
import com.bandyer.core_av.subscriber.SubscriberObserver;
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

    private Publisher publisher;

    ImageZoomHelper imageZoomHelper;

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
    @SuppressWarnings("unchecked")
    public void onRemotePublisherJoined(@NonNull final Stream stream) {
        Log.e("Publisher", "onRemotePublisherJoined");
        final Subscriber subscriber = new Subscriber(stream)
                .addSubscribeObserver(this)
                .setBandwidthThrottlingStrategy(new FixedBandwidthThrottlingStrategy(10));
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
        CapturerAV capturerAV = new CapturerAV();
        publisher = new Publisher(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(AutoPubSubRoomActivity.this)
                .setCapturer(capturerAV);
        room.publish(this, publisher);

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
    protected void onDestroy() {
        super.onDestroy();
        if (room != null)
            room.leave();
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("Room", reason);
        if (room != null)
            room.leave();
    }

    @Override
    public void onLocalSubscriberAdded(@NonNull Subscriber subscriber) {
        Log.e("Subscriber", "onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("Subscriber", reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.e("Publisher", "onLocalPublisherAdded");
    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("Publisher", reason);
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper != null && (imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev));
    }
}
