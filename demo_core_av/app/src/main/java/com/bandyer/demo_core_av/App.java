/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av;

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