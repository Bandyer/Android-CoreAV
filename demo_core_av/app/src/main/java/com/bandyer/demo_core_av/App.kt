/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av

import android.app.Application
import android.util.Log
import com.bandyer.android_common.logging.BaseLogger
import com.bandyer.android_common.logging.NetworkLogger
import com.bandyer.core_av.BandyerCoreAV
import com.bandyer.core_av.utils.logging.CoreLogger
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.leakcanary.LeakCanary
import okhttp3.OkHttpClient

/**
 * @author kristiyan
 */
class App : Application(), NetworkLogger {

    private var stethoReporter: StethoReporter? = null

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)
        Stetho.initializeWithDefaults(this)

        stethoReporter = StethoReporter()

        val okHttpClientBuilder = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor())
        val gsonBuilder = GsonBuilder().setLenient()

        BandyerCoreAV.init(BandyerCoreAV.Builder(this)
                .setHttpStackBuilder(okHttpClientBuilder)
                .setGsonBuilder(gsonBuilder)
                .setNetworkLogger(this)
                .setLogger(object : CoreLogger(BaseLogger.ERROR) {
                    // add all the levels you want to debug
                    // will log only the errors type
                    override val target: Int
                        get() = ROOM or PUBLISHER or SUBSCRIBER // add all the levels you want to debug

                    override fun verbose(tag: String, message: String) {
                        Log.v(tag, message)
                    }

                    override fun debug(tag: String, message: String) {
                        Log.d(tag, message)
                    }

                    override fun info(tag: String, message: String) {
                        Log.i(tag, message)
                    }

                    override fun warn(tag: String, message: String) {
                        Log.w(tag, message)
                    }

                    override fun error(tag: String, message: String) {
                        Log.e(tag, message)
                    }
                })
        )

        gson = BandyerCoreAV.instance!!.gson
        okHttpClient = BandyerCoreAV.instance!!.httpStack
    }

    override fun onConnected(tag: String, url: String) {
        Log.d(tag, "onConnected $url")
        stethoReporter!!.onCreated(url)
    }

    override fun onMessageReceived(tag: String, response: String) {
        Log.d(tag, "onMessageReceived $response")
        stethoReporter!!.onReceive(response)
    }

    override fun onMessageSent(tag: String, request: String) {
        Log.d(tag, "onMessageSent $request")
        stethoReporter!!.onSend(request)
    }

    override fun onDisconnected(tag: String) {
        Log.d(tag, "onDisconnected")
        stethoReporter!!.onClosed()
    }

    override fun onError(tag: String, reason: String) {
        Log.d(tag, "connection error $reason")
        stethoReporter!!.onError(reason)
    }

    companion object {
        var okHttpClient: OkHttpClient? = null
        var gson: Gson? = null
    }
}