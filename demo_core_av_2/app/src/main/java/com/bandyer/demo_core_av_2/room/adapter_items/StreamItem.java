/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.room.adapter_items;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bandyer.core_av.Stream;
import com.bandyer.demo_core_av_2.R;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

import butterknife.BindView;

/**
 * @author kristiyan
 **/

public class StreamItem extends AbstractItem<StreamItem, StreamItem.ViewHolder> {

    private Stream stream;

    public StreamItem(Stream stream) {
        this.stream = stream;
    }

    public Stream getStream() {
        return stream;
    }

    @Override
    public long getIdentifier() {
        return stream.getStreamId().hashCode();
    }

    @Override
    public int getType() {
        return R.id.stream_item;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.item_stream;
    }

    @NonNull
    @Override
    public StreamItem.ViewHolder getViewHolder(@NonNull View v) {
        return new StreamItem.ViewHolder(v);
    }

    /**
     * our ViewHolder
     */
    public static class ViewHolder extends ButterKnifeViewHolder<StreamItem> {

        @BindView(R.id.stream)
        TextView stream;

        ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindView(@NonNull final StreamItem item, @NonNull List<Object> payloads) {
            stream.setText(item.stream.getStreamId());
        }

        @Override
        public void unbindView(@NonNull StreamItem item) {
            stream.setText(null);
        }
    }

}