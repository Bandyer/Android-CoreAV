/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bandyer.core_av.utils.logging.InternalStatsTypes
import java.util.*

class StatsPage {

    private var encoderStatsText: TextView? = null
    private var senderStatsText: TextView? = null
    private var receiverStatsText: TextView? = null
    private var connectionStatsText: TextView? = null
    private var bandwidthStatsView: TextView? = null

    fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val rootView = inflater.inflate(R.layout.alert_internals_stats_page, container, false) as ViewGroup
        bandwidthStatsView = rootView.findViewById(R.id.bandwidthStatsView)
        connectionStatsText = rootView.findViewById(R.id.connectionStatsView)
        receiverStatsText = rootView.findViewById(R.id.receiverStatsView)
        senderStatsText = rootView.findViewById(R.id.senderStatsView)
        encoderStatsText = rootView.findViewById(R.id.encoderStatsView)
        container.addView(rootView)
        return rootView
    }

    fun updateStats(stats: HashMap<InternalStatsTypes, String>) {
        if (encoderStatsText == null) return
        encoderStatsText!!.setText(colorized(stats[InternalStatsTypes.ENCODER]), TextView.BufferType.SPANNABLE)
        senderStatsText!!.setText(colorized(stats[InternalStatsTypes.SENDER]), TextView.BufferType.SPANNABLE)
        receiverStatsText!!.setText(colorized(stats[InternalStatsTypes.RECEIVER]), TextView.BufferType.SPANNABLE)
        connectionStatsText!!.setText(colorized(stats[InternalStatsTypes.CONNECTION]), TextView.BufferType.SPANNABLE)
        bandwidthStatsView!!.setText(colorized(stats[InternalStatsTypes.BANDWIDTH]), TextView.BufferType.SPANNABLE)
    }

    private fun colorized(text: String?): Spannable {
        val spannable: Spannable = SpannableString(text)
        var start: Int
        val rows = text!!.split("\n").toTypedArray()
        for (row in rows) {
            for (word in keywords) {
                if (row.contains(word.keyword)) {
                    start = text.indexOf(row)
                    spannable.setSpan(
                            ForegroundColorSpan(word.color),
                            start,
                            start + row.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    break
                }
            }
        }
        return spannable
    }

    private val keywords = arrayOf(
            HighlightedKeywords("ReceiveBandwidth", Color.BLUE),
            HighlightedKeywords("SendBandwidth", Color.BLUE),
            HighlightedKeywords("CodecName", Color.BLUE),
            HighlightedKeywords("mediaType", Color.BLUE),
            HighlightedKeywords("codecImplementationName", Color.BLUE),
            HighlightedKeywords("packetsLost", Color.BLUE),
            HighlightedKeywords("RemoteCandidateType", Color.BLUE),
            HighlightedKeywords("NacksSent", Color.BLUE),
            HighlightedKeywords("NacksReceived", Color.BLUE),
            HighlightedKeywords("PlisSent", Color.BLUE),
            HighlightedKeywords("PlisReceived", Color.BLUE),
            HighlightedKeywords("HasEnteredLowResolution=true", Color.RED),
            HighlightedKeywords("CpuLimitedResolution=true", Color.RED),
            HighlightedKeywords("BandwidthLimitedResolution=true", Color.RED)
    )

    internal inner class HighlightedKeywords(var keyword: String, var color: Int)
}