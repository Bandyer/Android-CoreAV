/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.networking

import com.bandyer.demo_core_av.App
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author kristiyan
 */
object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var clientUrl: String? = null

    fun getClient(url: String): Retrofit? {
        App.okHttpClient ?: return null
        App.gson ?: return null
        if (retrofit != null && url == clientUrl) return retrofit
        retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(App.okHttpClient!!)
                .addConverterFactory(GsonConverterFactory.create(App.gson!!))
                .build()
        clientUrl = url
        return retrofit
    }
}