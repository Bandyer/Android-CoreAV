<p align="center">
<img src="img/bandyer.jpg" alt="Bandyer" title="Bandyer" />
</p>


[ ![Download](https://api.bintray.com/packages/bandyer/Android-CoreAV/Android-CoreAV/images/download.svg) ](https://bintray.com/bandyer/Android-CoreAV/Android-CoreAV/_latestVersion)[![Docs](https://img.shields.io/badge/docs-current-brightgreen.svg)](https://bandyer.github.io/Android-CoreAV/)
[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social&logo=twitter)](https://twitter.com/intent/follow?screen_name=bandyersrl)


Bandyer is a young innovative startup that enables audio/video communication and collaboration from any platform and browser! Through its WebRTC architecture, it makes video communication simple and punctual. 


---

. **[Overview](#overview)** .
**[Features](#features)** .
**[Requirements](#requirements)** .
**[Installation](#installation)** .
**[Quickstart](#quickstart)** .
**[Documentation](#documentation)** .
**[Support](#support)** .
**[Credits](#credits)** .

---

## Overview

Imagine that you would like to make audio/video calls. You may have heard of an open source solution known as [WebRTC](https://webrtc.org/). 

If you know what we are talking about, then you already know the huge effort that is required to integrate **WebRTC**.

While **WebRTC** may be open and free it does not make your life as a developer as easy as it should be.

In Bandyer-CoreAV we have decided to represent a call with 3 Major entities.

* **Room** - The place where publishers and subscribers meet.
* **RoomActor** - An user of the room
* **Publisher** - An user that wants to add his audio/video in the room.
* **Subscriber** - An user that wants to see the audio/video of a publisher in the room.

### Let's see a typical scenario 

In this scenario a user wants to stream his video to 2 friends.

![Architecture](img/pubSub.png)

## Features
* It's easy to use
* No need to understand WebRTC flows
* High-reliability
* Small-sized
* Fully covered by Unit-Tests
* Written in Kotlin
* Advanced proprietary features not available in standard WebRTC


## Requirements

Supports from API level 16 (Android 4.1 Jelly Bean).

**Requires compileOptions for Java8**
```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```
## Latest releases

Java && AndroidX >= **v1.3.0**

Java && AppCompat <= **v1.2.5**

## Installation

Download the [latest AAR](https://bintray.com/bandyer/Android-CoreAV/Android-CoreAV) or grab via Gradle:

```groovy
implementation 'com.bandyer:core_av:2.1.1'
```

## Quickstart

In your **Application** class:

```kotlin
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        BandyerCoreAV.initWithDefaults(this);
    }
}
```

Create **activity_main.xml** in **res/layout/**

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.bandyer.core_av.view.BandyerView
        android:id="@+id/publisherView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true">

        <LinearLayout
            android:id="@+id/subscribersListView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal" />
    </HorizontalScrollView>

</RelativeLayout>
```

Change your **MainActivity.kt**

```kotlin
class MainActivity : AppCompatActivity(), RoomObserver, SubscriberObserver, PublisherObserver {
    private var room: Room? = null;
    private var publisher: Publisher? = null

    // Layout elements
    private var subscribersListView: LinearLayout? = null
    private var publisherView: BandyerView? = null
    
    private var capturer: CameraCapturer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribersListView = findViewById(R.id.subscribersListView)
        publisherView = findViewById(R.id.publisherView)

        // Let's create a video call!!
        room = Room.Registry.get(RoomToken(TOKEN));
        room!!.addRoomObserver(this);
        room!!.join();
    }

    override fun onDestroy() {
        super.onDestroy()
        capturer?.destroy()
        Room.Registry.destroyAll();
    }

    /**
     * Once we have joined the call room
     * Let's add a publisher that will stream from the Frontal Camera
     */
    override fun onRoomEnter() {
        Log.d("Room", "onRoomEnter")

        // TODO: Publisher needs runtime permissions for AUDIO/VIDEO, otherwise it won't stream anything
        capturer = capturer<CameraCapturer>(this) {
            video = camera()
            audio = default()
        }
        capturer!!.start()

        publisher = room!!.create(RoomUser("aliasKris", "kristiyan", "petrov", "kris@bandyer.com", "image"))
                .addPublisherObserver(this@MainActivity)
                .setCapturer(capturer!!)
        room!!.publish(publisher!!)
        publisher!!.setView(publisherView!!, object : OnStreamListener {
            override fun onReadyToPlay(view: StreamView, stream: Stream) {
                view.play(stream)
            }
        })
    }

    override fun onRoomReconnecting() {
        Log.d("Room", "onRoomReconnecting ...")
    }

    override fun onRoomStateChanged(state: RoomState) {
        Log.d("Room", "onRoomStateChanged " + state.name())
    }

    override fun onRoomExit() {
        Log.d("Room", "onRoomExit")
    }

    override fun onRoomError(reason: String) {
        Log.e("Room", reason)
    }

    override fun onRoomActorUpdateStream(roomActor: RoomActor) {}


    /**
     * A new publisher has entered the remote room
     * Let's add a subscriber for each published stream
     *
     * @param stream remote audio/video stream
     */
    override fun onRemotePublisherJoined(stream: Stream) {
        Log.d("Publisher", "onRemotePublisherJoined")
        val subscriber = room!!.create(stream).addSubscribeObserver(this)
        room!!.subscribe(subscriber);

        // set the view where the stream will be played
        val subscriberView = BandyerView(this)
        val size = getDp(60)
        subscribersListView!!.addView(subscriberView, LinearLayout.LayoutParams(size, size))
        subscriber.setView(subscriberView, object : OnStreamListener {
            override fun onReadyToPlay(view: StreamView, stream: Stream) {
                subscriberView.play(stream);
                subscriberView.bringToFront(true);
            }
        });
    }

    private fun getDp(size: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), getResources().getDisplayMetrics()).toInt()
    }

    /**
     * The local publisher has joined the local room
     *
     * @param publisher the publisher created in this activity
     */
    override fun onLocalPublisherJoined(publisher: Publisher) {
        Log.d("Publisher", "onLocalPublisherJoined")
    }

    /**
     * If a remote publisher has left we should remove the subscriber related
     *
     * @param stream remote stream
     */
    override fun onRemotePublisherLeft(stream: Stream) {
        Log.d("Publisher", "onRemotePublisherLeft")
        val subscriber = room!!.getSubscriber(stream) ?: return
        room!!.unsubscribe(subscriber);
    }

    override fun onRemotePublisherUpdateStream(stream: Stream) = Unit

    // LOCAL SUBSCRIBER EVENTS
    override fun onLocalSubscriberAdded(subscriber: Subscriber) = Unit
    override fun onLocalSubscriberError(subscriber: Subscriber, reason: String) = Unit
    override fun onLocalSubscriberStateChanged(subscriber: Subscriber, state: SubscriberState) = Unit
    override fun onLocalSubscriberJoined(subscriber: Subscriber) = Unit
    override fun onLocalSubscriberUpdateStream(subscriber: Subscriber) = Unit
    override fun onLocalSubscriberAudioMuted(subscriber: Subscriber, muted: Boolean) = Unit
    override fun onLocalSubscriberVideoMuted(subscriber: Subscriber, muted: Boolean) = Unit
    override fun onLocalSubscriberStartedScreenSharing(subscriber: Subscriber, started: Boolean) = Unit
    override fun onLocalSubscriberRemoved(subscriber: Subscriber) = Unit
    override fun onLocalSubscriberConnected(subscriber: Subscriber, connected: Boolean) = Unit

    // LOCAL PUBLISHER EVENTS
    override fun onLocalPublisherAudioMuted(publisher: Publisher, muted: Boolean) = Unit
    override fun onLocalPublisherConnected(publisher: Publisher, connected: Boolean) = Unit
    override fun onLocalPublisherVideoMuted(publisher: Publisher, muted: Boolean) = Unit
    override fun onLocalPublisherUpdateStream(publisher: Publisher) = Unit
    override fun onLocalPublisherAdded(publisher: Publisher) = Unit
    override fun onLocalPublisherRemoved(publisher: Publisher) = Unit
    override fun onLocalPublisherStateChanged(publisher: Publisher, state: PublisherState) = Unit
    override fun onLocalPublisherError(publisher: Publisher, reason: String) = Unit

    companion object {
        // the token will be provided to you by a rest call
        private const val TOKEN = "Bandyer-Token"
    }
}
```

## Documentation

You can find the complete documentation in two different styles

Kotlin Doc: [https://bandyer.github.io/Bandyer-Android-CoreAV/kDoc/core_av/](https://bandyer.github.io/Bandyer-Android-CoreAV/kDoc/core_av/)

## Support
To get basic support please submit an [Issue](https://github.com/Bandyer/Bandyer-Android-CoreAV/issues) 

If you prefer commercial support, please contact [bandyer.com](https://bandyer.com) by mail: <mailto:info@bandyer.com>.


## Credits
- [WebRTC](https://webrtc.org/) by Google, Mozilla, Opera, W3C and ITF
- [Gson](https://github.com/google/gson) by Google
- [Android-weak-handler](https://github.com/badoo/android-weak-handler) by Badoo
- [Socket.io](https://github.com/socketio/socket.io-client-java) by socket.io
