/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2;

import android.app.Application;
import android.util.Log;

import com.bandyer.core_av.BandyerCoreAV;
import com.bandyer.core_av.utils.logging.CoreLogger;
import com.bandyer.core_av.utils.logging.NetworkLogger;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.leakcanary.LeakCanary;

import org.jetbrains.annotations.NotNull;

import okhttp3.OkHttpClient;

import static com.bandyer.core_av.utils.logging.BaseLogger.ERROR;

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
                .setLogger(new CoreLogger(ERROR) { // will log only the errors type

                    @Override
                    public int getLevel() {
                        return ROOM | PUBLISHER | SUBSCRIBER; // add all the levels you want to debug
                    }

                    @Override
                    public void verbose(@NotNull String tag, @NotNull String message) {
                        Log.v(tag, message);
                    }

                    @Override
                    public void debug(@NotNull String tag, @NotNull String message) {
                        Log.d(tag, message);
                    }

                    @Override
                    public void info(@NotNull String tag, @NotNull String message) {
                        Log.i(tag, message);
                    }

                    @Override
                    public void warn(@NotNull String tag, @NotNull String message) {
                        Log.w(tag, message);
                    }

                    @Override
                    public void error(@NotNull String tag, @NotNull String message) {
                        Log.e(tag, message);
                    }
                })
                .setGsonBuilder(gsonBuilder)

        );
    }

    @Override
    public void onConnected(@NotNull String tag, @NotNull String url) {
        Log.d(tag, "onConnected " + url);
        stethoReporter.onCreated(url);
    }

    @Override
    public void onMessageReceived(@NotNull String tag, @NotNull String response) {
        Log.d(tag, "onMessageReceived " + response);
        stethoReporter.onReceive(response);
    }

    @Override
    public void onMessageSent(@NotNull String tag, @NotNull String request) {
        Log.d(tag, "onMessageSent " + request);
        stethoReporter.onSend(request);
    }

    @Override
    public void onDisconnected(@NotNull String tag) {
        Log.d(tag, "onDisconnected");
        stethoReporter.onClosed();
    }

    @Override
    public void onError(@NotNull String tag, @NotNull String reason) {
        Log.d(tag, "connection error " + reason);
        stethoReporter.onError(reason);
    }
}
