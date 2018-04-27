/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2;

import android.app.Application;

import com.bandyer.core_av.BandyerCoreAV;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.leakcanary.LeakCanary;

import okhttp3.OkHttpClient;

/**
 * @author kristiyan
 **/

public class App extends Application {

    public static OkHttpClient okHttpClient;
    public static Gson gson;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        Stetho.initializeWithDefaults(this);

        okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        GsonBuilder gsonBuilder = new GsonBuilder().setLenient();
        gson = gsonBuilder.create();

        BandyerCoreAV.init(
                new BandyerCoreAV.Builder(this)
                        .setHttpStack(okHttpClient)
                        .setGsonBuilder(gsonBuilder)
        );

        BandyerCoreAV.initWithDefaults(this);


    }
}
