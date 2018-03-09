package com.example.bandyer.demo_bandyerCoreAV;

import android.app.Application;

import com.bandyer.core_av.BandyerCoreAV;

/**
 * @author kristiyan
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BandyerCoreAV.initWithDefaults(this);
    }
}