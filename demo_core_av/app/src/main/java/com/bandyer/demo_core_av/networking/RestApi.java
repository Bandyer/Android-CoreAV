/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.networking;

import com.bandyer.demo_core_av.networking.request_models.RequestToken;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * @author kristiyan
 **/

public interface RestApi {

    @Headers("Content-Type: application/json")
    @POST("createToken/")
    Call<String> connect(@Body RequestToken requestToken);

}