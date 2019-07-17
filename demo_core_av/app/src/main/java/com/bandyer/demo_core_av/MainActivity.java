/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av;


import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.widget.LinearLayout;

import com.bandyer.core_av.OnStreamListener;
import com.bandyer.core_av.Stream;
import com.bandyer.core_av.capturer.Capturer;
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
import com.bandyer.core_av.view.BandyerView;
import com.bandyer.core_av.view.StreamView;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements RoomObserver, SubscriberObserver, PublisherObserver {

    // the token will be provided to you by a rest call
    private static final String TOKEN = "Bandyer-Token";

    private Room room;
    private Publisher publisher;

    // Layout elements
    private LinearLayout subscribersListView;
    private BandyerView publisherView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        subscribersListView = findViewById(R.id.subscribersListView);
        publisherView = findViewById(R.id.publisherView);

        // Here we request the permissions to be able to show our current camera and hear the audio
        // Once we have the permissions we will join the Call Room
        MainActivityPermissionsDispatcher.hasPermissionsWithPermissionCheck(MainActivity.this);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void hasPermissions() {
        // Let's create a call
        try {
            room = Room.Registry.get(new RoomToken(TOKEN));
            room.addRoomObserver(this);
            room.join();
        } catch (Throwable error) {
            Snackbar.make(subscribersListView, "Please provide a valid Bandyer-Token", LENGTH_LONG).show();
        }
    }

    /**
     * Once we have joined the call room
     * Let's add a publisher that will stream from the Frontal Camera
     */
    @Override
    public void onRoomEnter() {
        Log.d("Room", "onRoomEnter");
        Capturer capturerAV = Capturer.Registry.get(this, new CapturerOptions.Builder().withAudio().withCamera());
        capturerAV.start();
        publisher = room.create(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(MainActivity.this)
                .setCapturer(capturerAV);
        room.publish(publisher);

        publisher.setView(publisherView, new OnStreamListener() {
            @Override
            public void onReadyToPlay(@NonNull StreamView view, @NonNull Stream stream) {
                view.play(stream);
            }
        });
    }

    @Override
    public void onRoomExit() {
        Log.d("Room", "onRoomExit");
    }

    @Override
    public void onRoomError(@NonNull String reason) {
        Log.e("Room", reason);
    }

    @Override
    public void onRoomReconnecting() {
        Log.d("Room", "onRoomReconnecting ...");
    }

    @Override
    public void onRoomStateChanged(@NonNull RoomState state) {
        Log.d("Room", "onRoomStateChanged " + state.name());
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
    protected void onDestroy() {
        super.onDestroy();
        // close the call
        Capturer.Registry.destroy();
        Room.Registry.destroyAll();
    }

    /**
     * The local publisher has started to stream in the room
     *
     * @param publisher the publisher created in this activity
     */
    @Override
    public void onLocalPublisherJoined(@NonNull Publisher publisher) {
        Log.d("Publisher", "onLocalPublisherJoined");
    }

    /**
     * A new publisher has started to stream in the room
     * Let's add a subscriber for each published stream
     *
     * @param stream remote audio/video stream
     */
    @Override
    public void onRemotePublisherJoined(@NonNull final Stream stream) {
        Log.d("Publisher", "onRemotePublisherJoined");

        final Subscriber subscriber = room.create(stream).addSubscribeObserver(this);
        room.subscribe(subscriber);

        // set the view where the stream will be played
        final BandyerView subscriberView = new BandyerView(this);
        int size = getDp(60);

        subscribersListView.addView(subscriberView, new LinearLayout.LayoutParams(size, size));
        subscriber.setView(subscriberView, new OnStreamListener() {
            @Override
            public void onReadyToPlay(@NonNull StreamView view, @NonNull Stream stream) {
                view.play(stream);
                subscriberView.bringToFront(true);
            }
        });
    }

    private int getDp(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getResources().getDisplayMetrics());
    }

    /**
     * If a remote publisher has left we should remove the subscriber related
     *
     * @param stream remote stream
     */
    @Override
    public void onRemotePublisherLeft(@NonNull Stream stream) {
        Log.d("Publisher", "onRemotePublisherLeft");
        Subscriber subscriber = room.getSubscriber(stream);
        if (subscriber == null)
            return;
        room.unsubscribe(subscriber);
    }

    @Override
    public void onLocalSubscriberAdded(@NonNull Subscriber subscriber) {
        Log.d("Subscriber", "onLocalSubscriberAdded");
    }

    @Override
    public void onLocalSubscriberRemoved(@NonNull Subscriber subscriber) {
        Log.d("Subscriber", "onLocalSubscriberRemoved");
    }

    @Override
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("Subscriber", reason);
    }

    @Override
    public void onLocalSubscriberStateChanged(@NonNull Subscriber subscriber, @NonNull SubscriberState subscriberState) {
        Log.d("Subscriber", "onLocalSubscriberStateChanged" + subscriberState.name());
    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.d("Publisher", "onLocalPublisherAdded");
    }

    @Override
    public void onLocalPublisherRemoved(@NonNull Publisher publisher) {
        Log.d("Publisher", "onLocalPublisherRemoved");
    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("Publisher", reason);
    }

    @Override
    public void onLocalPublisherStateChanged(@NonNull Publisher publisher, @NonNull PublisherState publisherState) {
        Log.d("Publisher", "onLocalPublisherStateChanged" + publisherState.name());
    }

    // SHOW RUN TIME PERMISSIONS DIALOG

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showDeniedForCamera() {
        Snackbar.make(subscribersListView, R.string.permission_camera_denied, LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showNeverAskForCamera() {
        Snackbar.make(subscribersListView, R.string.permission_camera_never_ask_again, LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onRemotePublisherUpdateStream(@NonNull Stream stream) {

    }

    @Override
    public void onLocalSubscriberJoined(@NonNull Subscriber subscriber) {

    }

    @Override
    public void onLocalSubscriberUpdateStream(@NonNull Subscriber subscriber) {

    }

    @Override
    public void onLocalSubscriberAudioMuted(@NonNull Subscriber subscriber, boolean muted) {

    }

    @Override
    public void onLocalSubscriberVideoMuted(@NonNull Subscriber subscriber, boolean muted) {

    }

    @Override
    public void onLocalSubscriberStartedScreenSharing(@NonNull Subscriber subscriber, boolean started) {

    }
}