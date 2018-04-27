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

import com.bandyer.core_av.Stream;
import com.bandyer.core_av.capturer.CapturerAV;
import com.bandyer.core_av.publisher.Publisher;
import com.bandyer.core_av.publisher.PublisherObserver;
import com.bandyer.core_av.room.Room;
import com.bandyer.core_av.room.RoomObserver;
import com.bandyer.core_av.room.RoomToken;
import com.bandyer.core_av.room.RoomUser;
import com.bandyer.core_av.subscriber.Subscriber;
import com.bandyer.core_av.subscriber.SubscriberObserver;
import com.bandyer.core_av.view.BandyerView;
import com.bandyer.core_av.view.OnViewStatusListener;

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
            room = new Room(new RoomToken(TOKEN));
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
        CapturerAV capturerAV = new CapturerAV();
        publisher = new Publisher(new RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(MainActivity.this)
                .setCapturer(capturerAV);
        room.publish(this, publisher);
        publisher.setView(publisherView, new OnViewStatusListener() {

            @Override
            public void onReadyToPlay(@NonNull Stream stream) {
                publisherView.play(stream);
            }


            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onViewSizeChanged(int width, int height, int rotationDegree) {

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
    protected void onDestroy() {
        super.onDestroy();
        // close the call
        if (room != null)
            room.leave();
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

        final Subscriber subscriber = new Subscriber(stream).addSubscribeObserver(this);
        room.subscribe(subscriber);

        // set the view where the stream will be played
        final BandyerView subscriberView = new BandyerView(this);
        int size = getDp(60);

        subscribersListView.addView(subscriberView, new LinearLayout.LayoutParams(size, size));
        subscriber.setView(subscriberView, new OnViewStatusListener() {

            @Override
            public void onReadyToPlay(@NonNull Stream stream) {
                subscriberView.play(stream);
            }

            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onViewSizeChanged(int width, int height, int rotationDegree) {

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
    public void onLocalSubscriberError(@NonNull Subscriber subscriber, @NonNull String reason) {
        Log.e("Subscriber", reason);

    }

    @Override
    public void onLocalPublisherAdded(@NonNull Publisher publisher) {
        Log.d("Publisher", "onLocalPublisherAdded");
    }

    @Override
    public void onLocalPublisherError(@NonNull Publisher publisher, @NonNull String reason) {
        Log.e("Publisher", reason);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

}