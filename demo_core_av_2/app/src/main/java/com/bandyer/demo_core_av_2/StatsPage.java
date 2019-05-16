/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bandyer.core_av.utils.logging.InternalStatsTypes;

import java.util.HashMap;

public class StatsPage {

    private TextView encoderStatsText;
    private TextView senderStatsText;
    private TextView receiverStatsText;
    private TextView connectionStatsText;
    private TextView bandwidthStatsView;

    View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.alert_internals_stats_page, container, false);
        bandwidthStatsView = rootView.findViewById(R.id.bandwidthStatsView);
        connectionStatsText = rootView.findViewById(R.id.connectionStatsView);
        receiverStatsText = rootView.findViewById(R.id.receiverStatsView);
        senderStatsText = rootView.findViewById(R.id.senderStatsView);
        encoderStatsText = rootView.findViewById(R.id.encoderStatsView);
        container.addView(rootView);
        return rootView;
    }

    public void updateStats(@NonNull final HashMap<InternalStatsTypes, String> stats) {
        if (encoderStatsText == null) return;

        encoderStatsText.setText(colorized(stats.get(InternalStatsTypes.ENCODER)), TextView.BufferType.SPANNABLE);
        senderStatsText.setText(colorized(stats.get(InternalStatsTypes.SENDER)), TextView.BufferType.SPANNABLE);
        receiverStatsText.setText(colorized(stats.get(InternalStatsTypes.RECEIVER)), TextView.BufferType.SPANNABLE);
        connectionStatsText.setText(colorized(stats.get(InternalStatsTypes.CONNECTION)), TextView.BufferType.SPANNABLE);
        bandwidthStatsView.setText(colorized(stats.get(InternalStatsTypes.BANDWIDTH)), TextView.BufferType.SPANNABLE);
    }

    private Spannable colorized(final String text) {
        final Spannable spannable = new SpannableString(text);
        int start;
        String[] rows = text.split("\n");
        for (String row : rows) {
            for (HighlightedKeywords word : keywords) {
                if (row.contains(word.keyword)) {
                    start = text.indexOf(row);
                    spannable.setSpan(
                            new ForegroundColorSpan(word.color),
                            start,
                            start + row.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    break;
                }
            }
        }

        return spannable;
    }

    private HighlightedKeywords[] keywords = new HighlightedKeywords[]{
            new HighlightedKeywords("ReceiveBandwidth", Color.BLUE),
            new HighlightedKeywords("SendBandwidth", Color.BLUE),
            new HighlightedKeywords("CodecName", Color.BLUE),
            new HighlightedKeywords("mediaType", Color.BLUE),
            new HighlightedKeywords("codecImplementationName", Color.BLUE),
            new HighlightedKeywords("packetsLost", Color.BLUE),
            new HighlightedKeywords("RemoteCandidateType", Color.BLUE),
            new HighlightedKeywords("NacksSent", Color.BLUE),
            new HighlightedKeywords("NacksReceived", Color.BLUE),
            new HighlightedKeywords("PlisSent", Color.BLUE),
            new HighlightedKeywords("PlisReceived", Color.BLUE),
            new HighlightedKeywords("HasEnteredLowResolution=true", Color.RED),
            new HighlightedKeywords("CpuLimitedResolution=true", Color.RED),
            new HighlightedKeywords("BandwidthLimitedResolution=true", Color.RED)
    };

    class HighlightedKeywords {

        HighlightedKeywords(String keyword, int color) {
            this.keyword = keyword;
            this.color = color;
        }

        String keyword;
        int color;
    }

}