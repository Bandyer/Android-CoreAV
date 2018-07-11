/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.room;

import android.content.Intent;
import android.os.Bundle;
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
import com.bandyer.core_av.capturer.AbstractBaseCapturer;
import com.bandyer.core_av.capturer.CapturerAV;
import com.bandyer.core_av.capturer.audio.CapturerAudio;
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
import com.bandyer.demo_core_av_2.design.bottom_sheet.picker.BottomListPicker;
import com.bandyer.demo_core_av_2.design.bottom_sheet.picker.BottomListPickerItem;
import com.bandyer.demo_core_av_2.room.adapter_items.PublisherItem;
import com.bandyer.demo_core_av_2.room.adapter_items.StreamItem;
import com.bandyer.demo_core_av_2.room.adapter_items.SubscriberItem;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.mikepenz.fastadapter.listeners.OnLongClickListener;
import com.viven.imagezoom.ImageZoomHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author kristiyan
 **/
public class RoomActivity extends BaseActivity implements RoomObserver, SubscriberObserver, PublisherObserver {

    public static final String ROOM_TOKEN = "token";
    public static final String ROOM_AUDIO_MUTED = "audio_muted";

    private Room room;

    private Publisher publisher;

    @BindView(R.id.streams)
    RecyclerView streams;

    @BindView(R.id.pubsubs)
    RecyclerView pubSubs;

    FastItemAdapter<StreamItem> streamAdapter = new FastItemAdapter<>();
    FastItemAdapter pubSubsAdapter = new FastItemAdapter<>();

    ImageZoomHelper imageZoomHelper;

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

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // if is the first item then it weights 2 else 1
                return position == 0 ? 2 : 1;
            }
        });

        pubSubs.setLayoutManager(layoutManager);
        pubSubs.setAdapter(pubSubsAdapter);

        streams.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        streams.setAdapter(streamAdapter);

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
                    onRemotePublisherJoined(subscriber.getStream());
                }
                pubSubsAdapter.remove(position);
                return false;
            }
        });

        streamAdapter.withOnClickListener(new OnClickListener<StreamItem>() {
            @Override
            public boolean onClick(@javax.annotation.Nullable View v, @NonNull IAdapter<StreamItem> adapter, @NonNull StreamItem item, int position) {
                addNewSubscriber(item.getStream());
                streamAdapter.remove(position);
                return false;
            }
        });
    }

    @OnClick(R.id.add_publish)
    void chooseTypeOfCall() {
        final BottomListPicker<String> joinType = new BottomListPicker<>(this, getString(R.string.room_join_title));
        List<String> types = Arrays.asList("Audio Call", "Video Call");
        joinType.setItems(types, -1, new BottomListPickerItem.Delegate<String>() {
            @Override
            public void onClicked(int adapterPosition, String item) {
                onAddPublish(item.equals("Audio Call"));
                joinType.dismiss();
            }
        }).show();
    }

    @SuppressWarnings("unchecked")
    void onAddPublish(boolean isAudioOnlyCall) {
        if (room == null || room.getRoomState() != RoomState.CONNECTED || publisher != null)
            return;

        AbstractBaseCapturer<?> capturer = isAudioOnlyCall ? new CapturerAudio(this) : new CapturerAV(this);
        publisher = new Publisher(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(RoomActivity.this)
                .setCapturer(capturer);
        capturer.start();
        room.publish(publisher);

        PublisherItem item = new PublisherItem(publisher, capturer);
        pubSubsAdapter.add(0, item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (room != null)
            room.leave();
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

        return super.onOptionsItemSelected(item);
    }

    public void showInfo() {
        if (room == null || room.getRoomState() != RoomState.CONNECTED)
            return;

        ArrayList<String> headers = new ArrayList<String>() {{
            add("Room Properties");
            add("Remote Streams");
            add("Subscribers");
            if (publisher != null)
                add("Publishers");
        }};

        ArrayList<String> items = new ArrayList<String>() {{
            add(App.gson.toJson(room.getRoomInfo()));
            add(App.gson.toJson(room.getStreams()));
            add(App.gson.toJson(room.getSubscribers()));
            if (publisher != null)
                add(App.gson.toJson(room.getPublisher()));
        }};

        InformationActivity.show(this, headers, items);
    }

    @Override
    public void onRemotePublisherJoined(@NotNull final Stream stream) {
        StreamItem streamItem = new StreamItem(stream);
        streamAdapter.add(0, streamItem);
    }

    @SuppressWarnings("unchecked")
    private void addNewSubscriber(final Stream stream) {
        final Subscriber subscriber = new Subscriber(stream)
                .addSubscribeObserver(RoomActivity.this);
        room.subscribe(subscriber);
        SubscriberItem subscriberItem = new SubscriberItem(subscriber);
        pubSubsAdapter.add(0, subscriberItem);
    }


    @Override
    public void onRemotePublisherLeft(@NotNull Stream stream) {
        streamAdapter.getItemAdapter().removeByIdentifier(stream.getStreamId().hashCode());
    }

    @Override
    public void onRoomExit() {
        Log.d("Room", "exit");
    }


    @Override
    public void onRoomStateChanged(@NotNull RoomState state) {
        Log.d("Room", "onRoomStateChanged " + state);
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("Room", "onRoomError " + reason);
    }

    @Override
    public void onRoomEnter() {
        Log.d("RoomActivity", "onRoomEnter");
    }

    @Override
    public void onRoomReconnecting() {
        Log.d("RoomActivity", "reconnecting...");
    }


    @Override
    public void onLocalSubscriberAdded(@NotNull Subscriber subscriber) {
        Log.d("RoomActivity", "onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberError(@NotNull Subscriber subscriber, @NotNull String reason) {
        pubSubsAdapter.getItemAdapter().removeByIdentifier(subscriber.getId().hashCode());
    }

    @Override
    public void onLocalSubscriberStateChanged(@NonNull Subscriber subscriber, @NonNull SubscriberState subscriberState) {
        Log.d("RoomActivity", "onLocalSubscriberStateChanged");
    }

    @Override
    public void onLocalPublisherAdded(@NotNull Publisher publisher) {
        Log.d("RoomActivity", "onLocalPublisherAdded");
    }

    @Override
    public void onLocalPublisherRemoved(@NotNull Publisher publisher) {
        streamAdapter.getItemAdapter().removeByIdentifier(publisher.getStream().getStreamId().hashCode());
    }

    @Override
    public void onLocalPublisherError(@NotNull Publisher publisher, @NotNull String reason) {
        pubSubsAdapter.getItemAdapter().removeByIdentifier(publisher.getId().hashCode());
    }

    @Override
    public void onLocalPublisherStateChanged(@NonNull Publisher publisher, @NonNull PublisherState publisherState) {
        Log.d("RoomActivity", "onLocalPublisherStateChanged");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper != null && (imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev));
    }
}
