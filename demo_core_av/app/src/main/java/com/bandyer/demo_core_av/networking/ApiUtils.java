/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.networking;

/**
 * @author kristiyan
 **/

public class ApiUtils {

    private ApiUtils() {
    }

    public static RestApi getRestApi(String url) {
        return RetrofitClient.getClient(url).create(RestApi.class);
    }
}
