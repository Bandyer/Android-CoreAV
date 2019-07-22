/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.networking.request_models;

/**
 * @author kristiyan
 **/

public class RequestToken {
    public String username = "26";
    public String role = "presenter"; // viewer | publishOnly | viewerWithData
    public String room = "basicExampleRoom";
    public String type = "p2p"; // p2p
}
