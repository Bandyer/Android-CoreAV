/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.networking.request_models

/**
 * @author kristiyan
 */
class RequestToken {
    var username = "26"
    var role = "presenter" // viewer | publishOnly | viewerWithData
    var room = "basicExampleRoom"
    var type = "p2p" // p2p
}