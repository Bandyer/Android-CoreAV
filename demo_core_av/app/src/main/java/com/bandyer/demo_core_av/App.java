/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av;

import android.app.Application;
import androidx.annotation.NonNull;
import android.util.Log;

import com.bandyer.android_common.logging.BaseLogger;
import com.bandyer.android_common.logging.NetworkLogger;
import com.bandyer.core_av.BandyerCoreAV;
import com.bandyer.core_av.utils.logging.CoreLogger;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.leakcanary.LeakCanary;

import okhttp3.OkHttpClient;

/**
 * @author kristiyan
 **/
public class App extends Application implements NetworkLogger {

    public static OkHttpClient okHttpClient;
    public static Gson gson;

    StethoReporter stethoReporter;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        Stetho.initializeWithDefaults(this);

        stethoReporter = new StethoReporter();

        okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        GsonBuilder gsonBuilder = new GsonBuilder().setLenient();
        gson = gsonBuilder.create();

        BandyerCoreAV.init(new BandyerCoreAV.Builder(this)
                .setHttpStack(okHttpClient)
                .setNetworkLogger(this)
                .setLogger(new CoreLogger(BaseLogger.ERROR) { // will log only the errors type
                    @Override
                    public int getTarget() {
                        return ROOM | PUBLISHER | SUBSCRIBER; // add all the levels you want to debug
                    }

                    @Override
                    public void verbose(@NonNull String tag, @NonNull String message) {
                        Log.v(tag, message);
                    }

                    @Override
                    public void debug(@NonNull String tag, @NonNull String message) {
                        Log.d(tag, message);
                    }

                    @Override
                    public void info(@NonNull String tag, @NonNull String message) {
                        Log.i(tag, message);
                    }

                    @Override
                    public void warn(@NonNull String tag, @NonNull String message) {
                        Log.w(tag, message);
                    }

                    @Override
                    public void error(@NonNull String tag, @NonNull String message) {
                        Log.e(tag, message);
                    }
                })
                .setGsonBuilder(gsonBuilder)

        );
    }

    @Override
    public void onConnected(@NonNull String tag, @NonNull String url) {
        Log.d(tag, "onConnected " + url);
        stethoReporter.onCreated(url);
    }

    @Override
    public void onMessageReceived(@NonNull String tag, @NonNull String response) {
        Log.d(tag, "onMessageReceived " + response);
        stethoReporter.onReceive(response);
    }

    @Override
    public void onMessageSent(@NonNull String tag, @NonNull String request) {
        Log.d(tag, "onMessageSent " + request);
        stethoReporter.onSend(request);
    }

    @Override
    public void onDisconnected(@NonNull String tag) {
        Log.d(tag, "onDisconnected");
        stethoReporter.onClosed();
    }

    @Override
    public void onError(@NonNull String tag, @NonNull String reason) {
        Log.d(tag, "connection error " + reason);
        stethoReporter.onError(reason);
    }
}
