/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av

import com.facebook.stetho.inspector.network.NetworkEventReporterImpl
import com.facebook.stetho.inspector.network.SimpleTextInspectorWebSocketFrame

/**
 * Stetho reporter for networking
 *
 */
class StethoReporter {
    private val mReporter = NetworkEventReporterImpl.get()
    private var mRequestId: String? = null
    fun onCreated(host: String?) {
        mRequestId = mReporter.nextRequestId()
        mReporter.webSocketCreated(mRequestId, host)
    }

    fun onError(e: String?) {
        mReporter.webSocketFrameError(mRequestId, e)
    }

    fun onClosed() {
        mReporter.webSocketClosed(mRequestId)
    }

    fun onSend(message: String?) {
        mReporter.webSocketFrameSent(SimpleTextInspectorWebSocketFrame(mRequestId, message))
    }

    fun onReceive(message: String?) {
        mReporter.webSocketFrameReceived(SimpleTextInspectorWebSocketFrame(mRequestId, message))
    }
}